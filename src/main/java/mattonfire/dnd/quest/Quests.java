package mattonfire.dnd.quest;

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
 * The loaded quests: every {@code data/<ns>/quests/**.json} in the server's data packs. A file with a
 * bad field is logged (with its name and the field) and skipped; the rest still load.
 */
public final class Quests {
    private static final String FOLDER = "quests";
    private static volatile Map<Identifier, QuestDefinition> quests = Map.of();

    private Quests() {
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
        Map<Identifier, QuestDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.findResources(FOLDER,
                path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath().substring(FOLDER.length() + 1, file.getPath().length() - ".json".length());
            Identifier id = new Identifier(file.getNamespace(), path);
            try (Reader reader = entry.getValue().getReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                loaded.put(id, QuestDefinition.parse(id, QuestJson.object(json, "quest file")));
            } catch (Exception e) {
                DnDClasses.LOGGER.error("[Quests] Skipping quest {} ({}): {}", id, file, e.getMessage());
            }
        }
        for (QuestDefinition quest : loaded.values()) {
            for (Identifier required : quest.requires().quests()) {
                if (!loaded.containsKey(required)) {
                    DnDClasses.LOGGER.warn("[Quests] Quest {} requires unknown quest {}", quest.id(), required);
                }
            }
        }
        quests = Collections.unmodifiableMap(loaded);
        DnDClasses.LOGGER.info("[Quests] Loaded {} quests: {}", loaded.size(), loaded.keySet());
    }

    public static Collection<QuestDefinition> all() {
        return quests.values();
    }

    @Nullable
    public static QuestDefinition get(Identifier id) {
        return quests.get(id);
    }
}
