package mattonfire.dnd.faction;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * The loaded factions: every {@code data/<ns>/factions/<id>.json} in the server's data packs.
 * A file with a bad field is logged and skipped; the rest still load.
 */
public final class Factions {
    private static final String FOLDER = "factions";
    private static volatile Map<Identifier, Faction> factions = Map.of();

    private Factions() {
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

    static void load(ResourceManager manager) {
        Map<Identifier, Faction> loaded = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.findResources(FOLDER,
                path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath().substring(FOLDER.length() + 1, file.getPath().length() - ".json".length());
            Identifier id = new Identifier(file.getNamespace(), path);
            try (Reader reader = entry.getValue().getReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                if (!json.isJsonObject()) {
                    throw new IllegalArgumentException("not a JSON object");
                }
                loaded.put(id, Faction.parse(id, json.getAsJsonObject()));
            } catch (Exception e) {
                DnDClasses.LOGGER.error("[Factions] Skipping faction {} ({}): {}", id, file, e.getMessage());
            }
        }
        for (Faction faction : loaded.values()) {
            for (Identifier rival : faction.rivals().keySet()) {
                if (!loaded.containsKey(rival)) {
                    DnDClasses.LOGGER.warn("[Factions] Faction {} lists unknown rival {}", faction.id(), rival);
                }
            }
        }
        factions = Collections.unmodifiableMap(loaded);
        DnDClasses.LOGGER.info("[Factions] Loaded {} factions: {}", loaded.size(), loaded.keySet());
    }

    public static Collection<Faction> all() {
        return factions.values();
    }

    @Nullable
    public static Faction get(Identifier id) {
        return factions.get(id);
    }
}
