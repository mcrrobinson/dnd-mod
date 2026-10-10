package mattonfire.dnd.classes.SkillChecks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mattonfire.dnd.classes.Abilities.Ability;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;

/**
 * The saving throws each creature's attacks call for, so creature knowledge (the Study card) and docs can list
 * them without repeating the numbers. Register an entry where the mob's attack is defined and roll the save from
 * that same entry, so the two never drift apart:
 *
 * <pre>{@code
 * static final MobSaveInfo.Entry BREATH = MobSaveInfo.register(ModEntityTypes.WYVERN,
 *         "save.dndclasses.fire_breath", Ability.DEX, 12, true, "save.dndclasses.effect.half_damage");
 * ...
 * SaveResult r = SavingThrow.of(victim, BREATH).source(this).exposure(this, "breath", 60).roll();
 * }</pre>
 */
public final class MobSaveInfo {
    /**
     * One attack's save.
     *
     * @param labelKey     the attack's name, shown on the save lane ("Fire breath")
     * @param halvesDamage a success halves the damage
     * @param effectKey    what a success does, for the Study card ("half damage, shorter burn")
     */
    public record Entry(EntityType<?> type, String labelKey, Ability ability, int dc, boolean halvesDamage,
            String effectKey) {
        public Text describe() {
            return Text.translatable("save.dndclasses.info", Text.translatable(labelKey),
                    Text.translatable(ability.shortTranslationKey()), dc, Text.translatable(effectKey));
        }
    }

    private static final Map<EntityType<?>, List<Entry>> ENTRIES = new LinkedHashMap<>();

    private MobSaveInfo() {
    }

    public static synchronized Entry register(EntityType<?> type, String labelKey, Ability ability, int dc,
            boolean halvesDamage, String effectKey) {
        Entry entry = new Entry(type, labelKey, ability, dc, halvesDamage, effectKey);
        ENTRIES.computeIfAbsent(type, k -> new ArrayList<>()).add(entry);
        return entry;
    }

    /** A creature's saves, in registration order (empty if none). */
    public static synchronized List<Entry> of(EntityType<?> type) {
        List<Entry> entries = ENTRIES.get(type);
        return entries == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public static synchronized Map<EntityType<?>, List<Entry>> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(ENTRIES));
    }
}
