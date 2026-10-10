package mattonfire.dnd.quest.dialogue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.quest.QuestJson;
import mattonfire.dnd.quest.Quests;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * The loaded dialogue: every {@code data/<ns>/dialogue/**.json}, merged by role (files in id order). A
 * file with a bad field is logged with its name and skipped; the rest still load.
 */
public final class Dialogues {
    private static final String FOLDER = "dialogue";
    private static volatile Map<String, Dialogue> dialogues = Map.of();

    private Dialogues() {
    }

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier(DnDClasses.MOD_ID, FOLDER);
            }

            // Quests first, so links to unknown quests can be reported.
            @Override
            public java.util.Collection<Identifier> getFabricDependencies() {
                return List.of(new Identifier(DnDClasses.MOD_ID, "quests"));
            }

            @Override
            public void reload(ResourceManager manager) {
                load(manager);
            }
        });
    }

    static void load(ResourceManager manager) {
        Map<String, List<Dialogue.Part>> parts = new LinkedHashMap<>();
        Map<Identifier, Resource> files = new TreeMap<>(manager.findResources(FOLDER, path -> path.getPath().endsWith(".json")));
        for (Map.Entry<Identifier, Resource> entry : files.entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath().substring(FOLDER.length() + 1, file.getPath().length() - ".json".length());
            Identifier id = new Identifier(file.getNamespace(), path);
            try (Reader reader = entry.getValue().getReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                Dialogue.Part part = Dialogue.Part.parse(id, QuestJson.object(json, "dialogue file"));
                parts.computeIfAbsent(part.role(), role -> new ArrayList<>()).add(part);
            } catch (Exception e) {
                DnDClasses.LOGGER.error("[Dialogue] Skipping dialogue {} ({}): {}", id, file, e.getMessage());
            }
        }
        Map<String, Dialogue> loaded = new LinkedHashMap<>();
        for (Map.Entry<String, List<Dialogue.Part>> entry : parts.entrySet()) {
            loaded.put(entry.getKey(), merge(entry.getKey(), entry.getValue()));
        }
        dialogues = Collections.unmodifiableMap(loaded);
        DnDClasses.LOGGER.info("[Dialogue] Loaded dialogue for {} roles: {}", loaded.size(), loaded.keySet());
    }

    private static Dialogue merge(String role, List<Dialogue.Part> parts) {
        Text speaker = null;
        Map<String, Text> greetings = new LinkedHashMap<>();
        List<Dialogue.Option> topics = new ArrayList<>();
        Map<String, Dialogue.Node> nodes = new LinkedHashMap<>();
        for (Dialogue.Part part : parts) {
            if (speaker == null) {
                speaker = part.speaker();
            }
            part.greetings().forEach(greetings::putIfAbsent);
            topics.addAll(part.topics());
            part.nodes().forEach((id, node) -> {
                if (nodes.putIfAbsent(id, node) != null) {
                    DnDClasses.LOGGER.warn("[Dialogue] {}: node {} is defined twice; keeping the first", role, id);
                }
            });
        }
        Dialogue dialogue = new Dialogue(role, speaker, Map.copyOf(greetings), List.copyOf(topics), Map.copyOf(nodes));
        // Links to missing pages and quests only warn: the option then returns to the first page.
        List<Dialogue.Option> all = new ArrayList<>(dialogue.topics());
        dialogue.nodes().values().forEach(node -> all.addAll(node.options()));
        for (Dialogue.Option option : all) {
            for (String target : new String[]{option.go(), option.check() == null ? null : option.check().success(),
                    option.check() == null ? null : option.check().failure()}) {
                if (target != null && !target.equals(Dialogue.HUB) && !dialogue.nodes().containsKey(target)) {
                    DnDClasses.LOGGER.warn("[Dialogue] {}: option {} goes to unknown node {}", role, option.key(), target);
                }
            }
            for (Identifier quest : new Identifier[]{option.accept(), option.requires().quest()}) {
                if (quest != null && Quests.get(quest) == null) {
                    DnDClasses.LOGGER.warn("[Dialogue] {}: option {} names unknown quest {}", role, option.key(), quest);
                }
            }
        }
        return dialogue;
    }

    /** The dialogue for a role, or null if no file names it (quest offers and hand-ins still work). */
    @Nullable
    public static Dialogue get(String role) {
        return dialogues.get(role);
    }
}
