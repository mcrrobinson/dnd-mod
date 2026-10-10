package mattonfire.dnd.quest.dialogue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.quest.QuestAction;
import mattonfire.dnd.quest.QuestJson;
import mattonfire.dnd.quest.QuestObjective;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * What NPCs with one role say: every {@code data/<ns>/dialogue/**.json} naming that {@code role},
 * merged (so a quest pack can add topics and nodes to the innkeeper without copying its file).
 *
 * <pre>
 * {
 *   "role": "innkeeper",
 *   "speaker": "...",                       optional, else the NPC's name
 *   "greeting": "..." or {"default": "...", "unfriendly": "...", "friendly": "...", ...},
 *   "topics": [option, ...],                extra options on the first page
 *   "nodes": {"id": {"speaker": ..., "lines": ["..."], "options": [option, ...]}}
 * }
 * option: {"id": "...", "text": "...", "requires": {...}, "goto": "node" | "hub", "end": true,
 *          "accept": "quest id", "actions": [quest action, ...],
 *          "check": {"skill": "persuasion", "dc": 13, "success": "node", "failure": "node",
 *                    "on_success": [action, ...], "on_failure": [action, ...]}}
 * requires: {"quest": "id", "state": "active|finished|not_started|available", "stage": 2,
 *            "flags": ["..."], "not_flags": ["..."], "rep": {"faction": "tier"}, "class": "rogue"}
 * </pre>
 *
 * @param speaker   the name shown over the text, or null for the NPC's own name
 * @param greetings first-page line by tier id ({@code default} for the rest)
 * @param topics    options added to the first page, after the quest offers
 * @param nodes     the other pages, by id
 */
public record Dialogue(String role, @Nullable Text speaker, Map<String, Text> greetings, List<Option> topics,
                       Map<String, Node> nodes) {
    /** The first page: greeting, quest offers, topics, trade and goodbye. */
    public static final String HUB = "hub";

    @Nullable
    public Text greeting(ReputationTier tier) {
        Text text = this.greetings.get(tier.id);
        return text != null ? text : this.greetings.get("default");
    }

    /** One page: up to three lines and its options. */
    public record Node(@Nullable Text speaker, List<Text> lines, List<Option> options) {
        static Node parse(String fileKey, String id, JsonElement element) {
            JsonObject json = QuestJson.object(element, "node " + id);
            List<Text> lines = QuestJson.list(json, "lines", line -> QuestJson.text(line, "line"));
            if (lines.isEmpty() && json.has("text")) {
                lines = List.of(QuestJson.text(json.get("text"), "text"));
            }
            List<Option> options = parseOptions(json, "options", fileKey + "/" + id);
            return new Node(QuestJson.optText(json, "speaker"), List.copyOf(lines), options);
        }
    }

    /**
     * A reply the player can pick.
     *
     * @param key     stable id for re-validation and the one-try rule of checks
     * @param go      the page to show next (null: back to the first page)
     * @param end     close the dialogue after the option's effects
     * @param accept  a quest to start (the dialogue reports why if it can't)
     * @param check   a d20 roll that picks the next page
     * @param actions quest actions run for the player, in the context of {@code requires.quest}
     */
    public record Option(String key, Text text, Requires requires, @Nullable String go, boolean end,
                         @Nullable Identifier accept, @Nullable Check check, List<QuestAction> actions) {
        static Option parse(String key, JsonElement element) {
            JsonObject json = QuestJson.object(element, "option");
            String id = json.has("id") ? QuestJson.string(json, "id") : key;
            Requires requires = Requires.parse(json.get("requires"));
            List<QuestAction> actions = QuestJson.list(json, "actions", QuestAction::parse);
            Check check = json.has("check") ? Check.parse(json.get("check")) : null;
            if ((!actions.isEmpty() || check != null && check.hasActions()) && requires.quest() == null) {
                throw new IllegalArgumentException("an option with actions needs requires.quest (the quest they belong to)");
            }
            return new Option(id, QuestJson.text(json.get("text"), "text"), requires, QuestJson.optString(json, "goto"),
                    QuestJson.bool(json, "end", false), json.has("accept") ? QuestJson.id(json, "accept") : null, check,
                    List.copyOf(actions));
        }
    }

    /**
     * A skill check in dialogue. The DC is shifted by the player's standing with the NPC's faction; each
     * player (or quest instance, with {@code requires.quest}) gets one try.
     */
    public record Check(Skill skill, int dc, @Nullable String success, @Nullable String failure,
                        List<QuestAction> onSuccess, List<QuestAction> onFailure) {
        boolean hasActions() {
            return !this.onSuccess.isEmpty() || !this.onFailure.isEmpty();
        }

        static Check parse(JsonElement element) {
            JsonObject json = QuestJson.object(element, "check");
            String name = QuestJson.string(json, "skill");
            Skill skill = Skill.byId(name);
            if (skill == null) {
                throw new IllegalArgumentException("check: unknown skill \"" + name + "\"");
            }
            return new Check(skill, QuestJson.integer(json, "dc", 10, 1, 40), QuestJson.optString(json, "success"),
                    QuestJson.optString(json, "failure"),
                    List.copyOf(QuestJson.list(json, "on_success", QuestAction::parse)),
                    List.copyOf(QuestJson.list(json, "on_failure", QuestAction::parse)));
        }
    }

    /** When an option shows. */
    public enum QuestState {
        /** On the quest now (the default when a quest is named). */
        ACTIVE,
        /** Finished it at least once. */
        FINISHED,
        /** Neither on it nor finished it. */
        NOT_STARTED,
        /** Could start it now (requirements met, not on it, not finished or off cooldown). */
        AVAILABLE
    }

    public record Requires(@Nullable Identifier quest, QuestState state, int stage, List<String> flags,
                           List<String> notFlags, Map<Identifier, ReputationTier> rep, @Nullable String playerClass) {
        static final Requires NONE = new Requires(null, QuestState.ACTIVE, 0, List.of(), List.of(), Map.of(), null);

        static Requires parse(@Nullable JsonElement element) {
            if (element == null) {
                return NONE;
            }
            JsonObject json = QuestJson.object(element, "requires");
            Identifier quest = json.has("quest") ? QuestJson.id(json, "quest") : null;
            QuestState state = QuestState.ACTIVE;
            if (json.has("state")) {
                String name = QuestJson.string(json, "state");
                try {
                    state = QuestState.valueOf(name.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("requires.state: unknown state \"" + name + "\"");
                }
            }
            int stage = QuestJson.integer(json, "stage", 0, 0, 100);
            List<String> flags = QuestJson.list(json, "flags", JsonElement::getAsString);
            List<String> notFlags = QuestJson.list(json, "not_flags", JsonElement::getAsString);
            if (quest == null && (stage > 0 || !flags.isEmpty() || !notFlags.isEmpty() || json.has("state"))) {
                throw new IllegalArgumentException("requires: stage, state and flags need a quest");
            }
            if ((stage > 0 || !flags.isEmpty()) && state != QuestState.ACTIVE) {
                throw new IllegalArgumentException("requires: stage and flags need state active");
            }
            Map<Identifier, ReputationTier> rep = new LinkedHashMap<>();
            if (json.has("rep")) {
                for (Map.Entry<String, JsonElement> entry : QuestJson.object(json.get("rep"), "requires.rep").entrySet()) {
                    rep.put(QuestJson.id(entry.getKey(), "requires.rep faction"), tier(entry.getValue().getAsString()));
                }
            }
            String playerClass = json.has("class") ? QuestJson.string(json, "class").toLowerCase(Locale.ROOT) : null;
            return new Requires(quest, state, stage, List.copyOf(flags), List.copyOf(notFlags), Map.copyOf(rep),
                    playerClass);
        }
    }

    static ReputationTier tier(String name) {
        for (ReputationTier tier : ReputationTier.values()) {
            if (tier.id.equals(name)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("unknown tier \"" + name + "\"");
    }

    static List<Option> parseOptions(JsonObject json, String field, String keyPrefix) {
        List<Option> options = new ArrayList<>();
        List<JsonElement> raw = QuestJson.list(json, field, element -> element);
        for (int i = 0; i < raw.size(); i++) {
            try {
                options.add(Option.parse(keyPrefix + "#" + i, raw.get(i)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(field + "[" + i + "]: " + e.getMessage());
            }
        }
        return List.copyOf(options);
    }

    /** One file, before merging by role. */
    record Part(String role, @Nullable Text speaker, Map<String, Text> greetings, List<Option> topics,
                Map<String, Node> nodes) {
        static Part parse(Identifier file, JsonObject json) {
            String role = QuestObjective.role(json, "role");
            Map<String, Text> greetings = new LinkedHashMap<>();
            JsonElement greeting = json.get("greeting");
            if (greeting != null && greeting.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : greeting.getAsJsonObject().entrySet()) {
                    if (!entry.getKey().equals("default")) {
                        tier(entry.getKey());
                    }
                    greetings.put(entry.getKey(), QuestJson.text(entry.getValue(), "greeting." + entry.getKey()));
                }
            } else if (greeting != null) {
                greetings.put("default", QuestJson.text(greeting, "greeting"));
            }
            String fileKey = file.toString();
            List<Option> topics = parseOptions(json, "topics", fileKey + "/" + HUB);
            Map<String, Node> nodes = new LinkedHashMap<>();
            if (json.has("nodes")) {
                for (Map.Entry<String, JsonElement> entry : QuestJson.object(json.get("nodes"), "nodes").entrySet()) {
                    if (entry.getKey().equals(HUB)) {
                        throw new IllegalArgumentException("nodes: \"" + HUB + "\" is the first page; add to it with topics");
                    }
                    try {
                        nodes.put(entry.getKey(), Node.parse(fileKey, entry.getKey(), entry.getValue()));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("nodes." + entry.getKey() + ": " + e.getMessage());
                    }
                }
            }
            return new Part(role, QuestJson.optText(json, "speaker"), greetings, topics, nodes);
        }
    }
}
