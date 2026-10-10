package mattonfire.dnd.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mattonfire.dnd.quest.QuestJson.EntityMatch;
import mattonfire.dnd.quest.QuestJson.ItemMatch;
import mattonfire.dnd.quest.QuestJson.StructureMatch;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * One thing a quest stage asks for. Every objective in a stage is required. {@link #count()} is how
 * many times it has to happen; progress is stored per quest instance.
 *
 * <p>{@code text} (optional in the file) replaces the generated description, e.g. "Slay goblins on
 * the road" instead of "Slay goblins".
 */
public sealed interface QuestObjective {
    int count();

    @Nullable
    Text text();

    /** The generated description, used when the file gives no {@code text}. */
    Text describe();

    default Text description() {
        return this.text() != null ? this.text() : this.describe();
    }

    /** Kill {@code count} mobs matching {@code entity}, optionally inside a structure. */
    record Kill(EntityMatch entity, int count, @Nullable StructureMatch within, @Nullable QuestDrop drop,
                @Nullable Text text) implements QuestObjective {
        @Override
        public Text describe() {
            return this.within != null
                    ? Text.translatable("quest.dndclasses.objective.kill_within", this.entity.name(), this.within.name())
                    : Text.translatable("quest.dndclasses.objective.kill", this.entity.name());
        }
    }

    /** Stand inside a structure. */
    record Visit(StructureMatch structure, @Nullable Text text) implements QuestObjective {
        @Override
        public int count() {
            return 1;
        }

        @Override
        public Text describe() {
            return Text.translatable("quest.dndclasses.objective.visit", this.structure.name());
        }
    }

    /** Speak with any NPC that has the role (command tag {@code dndclasses.role.<role>}). */
    record Talk(String role, @Nullable Text text) implements QuestObjective {
        @Override
        public int count() {
            return 1;
        }

        @Override
        public Text describe() {
            return Text.translatable("quest.dndclasses.objective.talk", roleName(this.role));
        }
    }

    /**
     * Have {@code count} matching items when handing in at the quest's giver; {@code consume} takes
     * them.
     */
    record Collect(ItemMatch item, int count, boolean consume, @Nullable Text text) implements QuestObjective {
        @Override
        public Text describe() {
            return Text.translatable("quest.dndclasses.objective.collect", this.item.name());
        }
    }

    /** Give {@code count} matching items to an NPC with the role (always consumed). */
    record Deliver(ItemMatch item, int count, String role, @Nullable Text text) implements QuestObjective {
        @Override
        public Text describe() {
            return Text.translatable("quest.dndclasses.objective.deliver", this.item.name(), roleName(this.role));
        }
    }

    /**
     * Pass a d20 check offered in dialogue: a dialogue option with a {@code check} for this skill calls
     * {@link QuestManager#checkPassed} when the roll succeeds.
     */
    record Check(String skill, int dc, @Nullable Text text) implements QuestObjective {
        @Override
        public int count() {
            return 1;
        }

        @Override
        public Text describe() {
            return Text.translatable("quest.dndclasses.objective.check",
                    Text.translatableWithFallback("skill.dndclasses." + this.skill, this.skill), this.dc);
        }
    }

    /** Win a goblin raid on a settlement of this kind ({@code hobbit_village}, {@code dwarven_fortress}). */
    record Defend(String settlement, @Nullable Text text) implements QuestObjective {
        @Override
        public int count() {
            return 1;
        }

        @Override
        public Text describe() {
            return Text.translatable("quest.dndclasses.objective.defend",
                    Text.translatableWithFallback("raid.dndclasses.goblin.place." + this.settlement, this.settlement));
        }
    }

    /**
     * An item that drops from matching kills, for quest participants only, with {@code chance}.
     */
    record QuestDrop(Identifier item, double chance) {
        static QuestDrop parse(JsonElement element) {
            JsonObject json = QuestJson.object(element, "quest_drop");
            Identifier item = QuestJson.id(json, "item");
            if (!Registries.ITEM.containsId(item)) {
                throw new IllegalArgumentException("quest_drop: unknown item \"" + item + "\"");
            }
            return new QuestDrop(item, QuestJson.number(json, "chance", 1.0D, 0.0D, 1.0D));
        }
    }

    static MutableText roleName(String role) {
        return Text.translatableWithFallback("quest.dndclasses.role." + role, role.replace('_', ' '));
    }

    static QuestObjective parse(JsonElement element) {
        JsonObject json = QuestJson.object(element, "objective");
        String type = QuestJson.string(json, "type");
        Text text = QuestJson.optText(json, "text");
        return switch (type) {
            case "kill" -> new Kill(EntityMatch.parse(QuestJson.string(json, "entity")),
                    QuestJson.integer(json, "count", 1, 1, 10000),
                    json.has("within_structure") ? StructureMatch.parse(QuestJson.string(json, "within_structure")) : null,
                    json.has("quest_drop") ? QuestDrop.parse(json.get("quest_drop")) : null, text);
            case "visit" -> new Visit(StructureMatch.parse(QuestJson.string(json, "structure")), text);
            case "talk" -> new Talk(role(json, "role"), text);
            case "collect" -> new Collect(ItemMatch.parse(QuestJson.string(json, "item")),
                    QuestJson.integer(json, "count", 1, 1, 10000), QuestJson.bool(json, "consume", true), text);
            case "deliver" -> new Deliver(ItemMatch.parse(QuestJson.string(json, "item")),
                    QuestJson.integer(json, "count", 1, 1, 10000), role(json, "role"), text);
            case "check" -> new Check(QuestJson.string(json, "skill"), QuestJson.integer(json, "dc", 10, 1, 40), text);
            case "defend" -> new Defend(QuestJson.string(json, "settlement"), text);
            default -> throw new IllegalArgumentException("unknown objective type \"" + type + "\"");
        };
    }

    static String role(JsonObject json, String key) {
        String role = QuestJson.string(json, key);
        if (!role.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("role \"" + role + "\" must be lowercase letters, digits and _");
        }
        return role;
    }
}
