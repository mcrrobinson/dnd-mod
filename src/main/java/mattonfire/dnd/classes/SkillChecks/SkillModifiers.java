package mattonfire.dnd.classes.SkillChecks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import mattonfire.dnd.classes.Progression.Progression;
import net.minecraft.entity.player.PlayerEntity;

/**
 * The modifier a player adds to a d20 check, for checks that are gated by {@link Eligibility}
 * (class-gated obstacles).
 *
 * <p>Until ability scores exist, the base is a placeholder from the class table: a primary solver
 * gets +3 ability plus proficiency, a secondary solver +1 plus proficiency, and an untrained one +0.
 * Proficiency grows with class level: levels 0-3 +2, 4-7 +3, 8-10 +4. A primary solver therefore
 * rolls +5 at level 0, the same as a Rogue's lockpick.
 *
 * <p>{@link SkillModifierProvider}s registered with {@link #register} run in order after the
 * placeholder and may add to it or replace it. This is the seam for ability scores, races,
 * subclasses and magic items, so obstacle code never needs to know about them.
 */
public final class SkillModifiers {
    public static final int PRIMARY_ABILITY = 3;
    public static final int SECONDARY_ABILITY = 1;

    /** Adjusts a check modifier. {@code current} is the modifier so far; return the new one. */
    @FunctionalInterface
    public interface SkillModifierProvider {
        int modify(PlayerEntity player, D20.Skill skill, Eligibility eligibility, int current);
    }

    private static final List<SkillModifierProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private SkillModifiers() {
    }

    public static void register(SkillModifierProvider provider) {
        PROVIDERS.add(provider);
    }

    /** Proficiency bonus for a class level: 0-3 +2, 4-7 +3, 8-10 +4. */
    public static int proficiency(int level) {
        return level >= 8 ? 4 : level >= 4 ? 3 : 2;
    }

    /** The placeholder from the class table, before any provider. */
    public static int base(PlayerEntity player, Eligibility eligibility) {
        return switch (eligibility) {
            case PRIMARY -> PRIMARY_ABILITY + proficiency(Progression.current(player).level());
            case SECONDARY -> SECONDARY_ABILITY + proficiency(Progression.current(player).level());
            case UNTRAINED, NONE -> 0;
        };
    }

    public static int modifier(PlayerEntity player, D20.Skill skill, Eligibility eligibility) {
        int modifier = base(player, eligibility);
        for (SkillModifierProvider provider : PROVIDERS) {
            modifier = provider.modify(player, skill, eligibility, modifier);
        }
        return modifier;
    }
}
