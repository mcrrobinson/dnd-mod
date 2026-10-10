package mattonfire.dnd.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mattonfire.dnd.faction.ReputationTier;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * A quest from {@code data/<ns>/quests/<path>.json}; its id is {@code <ns>:<path>}, e.g.
 * {@code dndclasses:goblin_menace/1}.
 *
 * @param chain         groups quests in the Journal (defaults to the id's folder)
 * @param giver         NPC role that offers the quest and pays its rewards, or null for none
 *                      (rewards are then paid as soon as it's finished)
 * @param requires      what a player needs before it's offered
 * @param cooldownDays  for a repeatable quest, days before it can be taken again; -1 if not repeatable
 * @param rumourWeight  weight on bounty boards' Rumour notices (0 = never offered as a rumour)
 * @param onAccept      run when the quest starts (and for each player who joins later)
 * @param rewards       paid to each participant when the quest is finished, at the giver
 */
public record QuestDefinition(Identifier id, Text title, @Nullable Text description, Identifier chain,
                              @Nullable String giver, Requirements requires, int cooldownDays, int rumourWeight,
                              List<QuestAction> onAccept, List<QuestStage> stages, List<QuestAction> rewards) {
    public static final int MAX_ACTIVE = 8;

    /** Quests that must be finished and minimum standings with factions. */
    public record Requirements(List<Identifier> quests, Map<Identifier, ReputationTier> rep) {
        static final Requirements NONE = new Requirements(List.of(), Map.of());

        static Requirements parse(@Nullable JsonElement element) {
            if (element == null) {
                return NONE;
            }
            JsonObject json = QuestJson.object(element, "requires");
            List<Identifier> quests = QuestJson.list(json, "quests",
                    value -> QuestJson.id(value.getAsString(), "required quest"));
            Map<Identifier, ReputationTier> rep = new LinkedHashMap<>();
            if (json.has("rep")) {
                for (Map.Entry<String, JsonElement> entry : QuestJson.object(json.get("rep"), "requires.rep").entrySet()) {
                    String tierName = entry.getValue().getAsString();
                    ReputationTier tier = null;
                    for (ReputationTier candidate : ReputationTier.values()) {
                        if (candidate.id.equals(tierName)) {
                            tier = candidate;
                        }
                    }
                    if (tier == null) {
                        throw new IllegalArgumentException("requires.rep: unknown tier \"" + tierName + "\"");
                    }
                    rep.put(QuestJson.id(entry.getKey(), "requires.rep faction"), tier);
                }
            }
            return new Requirements(List.copyOf(quests), Map.copyOf(rep));
        }
    }

    public boolean repeatable() {
        return this.cooldownDays >= 0;
    }

    public QuestStage stage(int index) {
        return this.stages.get(index);
    }

    static QuestDefinition parse(Identifier id, JsonObject json) {
        Text title = QuestJson.text(json.get("title"), "title");
        Text description = QuestJson.optText(json, "description");
        String folder = id.getPath().contains("/") ? id.getPath().substring(0, id.getPath().lastIndexOf('/')) : id.getPath();
        Identifier chain = json.has("chain") ? QuestJson.id(json, "chain") : new Identifier(id.getNamespace(), folder);
        String giver = json.has("giver") ? QuestObjective.role(json, "giver") : null;
        int cooldown = -1;
        if (json.has("repeatable")) {
            cooldown = QuestJson.integer(QuestJson.object(json.get("repeatable"), "repeatable"), "cooldown_days", 0, 0, 3650);
        }
        List<QuestStage> stages = QuestJson.list(json, "stages", QuestStage::parse);
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("a quest needs at least one stage");
        }
        return new QuestDefinition(id, title, description, chain, giver, Requirements.parse(json.get("requires")),
                cooldown, QuestJson.integer(json, "rumour_weight", 0, 0, 1000),
                List.copyOf(QuestJson.list(json, "on_accept", QuestAction::parse)), List.copyOf(stages),
                List.copyOf(QuestJson.list(json, "rewards", QuestAction::parse)));
    }
}
