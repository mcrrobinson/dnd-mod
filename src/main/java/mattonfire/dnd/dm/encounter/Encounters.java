package mattonfire.dnd.dm.encounter;

import java.io.Reader;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonParser;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

/**
 * Registry of {@link Encounter}s from {@code data/<namespace>/encounters/*.json}, reloaded with {@code /reload}.
 * A file {@code data/dndclasses/encounters/goblin_patrol.json} becomes {@code dndclasses:goblin_patrol}.
 */
public final class Encounters {
    public static final String FOLDER = "encounters";

    private static Map<Identifier, Encounter> encounters = Map.of();

    private Encounters() {
    }

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier(DnDClasses.MOD_ID, FOLDER);
            }

            @Override
            public void reload(ResourceManager manager) {
                load(manager);
            }
        });
    }

    private static void load(ResourceManager manager) {
        Map<Identifier, Encounter> loaded = new TreeMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager
                .findResources(FOLDER, path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath().substring(FOLDER.length() + 1, file.getPath().length() - ".json".length());
            Identifier id = new Identifier(file.getNamespace(), path);
            try (Reader reader = entry.getValue().getReader()) {
                loaded.put(id, Encounter.parse(id, JsonHelper.asObject(JsonParser.parseReader(reader), "encounter")));
            } catch (Exception e) {
                DnDClasses.LOGGER.error("[Encounters] couldn't load {}: {}", file, e.getMessage());
            }
        }
        encounters = loaded;
        DnDClasses.LOGGER.info("[Encounters] loaded {} encounters", loaded.size());
    }

    /**
     * Looks an encounter up by id. A bare name ({@code goblin_patrol}, which parses as {@code minecraft:}) falls back
     * to the {@code dndclasses} namespace.
     */
    @Nullable
    public static Encounter get(Identifier id) {
        Encounter encounter = encounters.get(id);
        if (encounter == null && id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            encounter = encounters.get(new Identifier(DnDClasses.MOD_ID, id.getPath()));
        }
        return encounter;
    }

    public static Collection<Encounter> all() {
        return encounters.values();
    }
}
