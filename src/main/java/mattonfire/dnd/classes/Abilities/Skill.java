package mattonfire.dnd.classes.Abilities;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * Every skill and tool proficiency a check can use, each with its ability. All eighteen 5e skills are
 * here, plus the tool proficiencies the mod's checks need, so other features never have to add one.
 */
public enum Skill {
    ACROBATICS(Ability.DEX),
    ANIMAL_HANDLING(Ability.WIS),
    ARCANA(Ability.INT),
    ATHLETICS(Ability.STR),
    DECEPTION(Ability.CHA),
    HISTORY(Ability.INT),
    INSIGHT(Ability.WIS),
    INTIMIDATION(Ability.CHA),
    INVESTIGATION(Ability.INT),
    MEDICINE(Ability.WIS),
    NATURE(Ability.INT),
    PERCEPTION(Ability.WIS),
    PERFORMANCE(Ability.CHA),
    PERSUASION(Ability.CHA),
    RELIGION(Ability.INT),
    SLEIGHT_OF_HAND(Ability.DEX),
    STEALTH(Ability.DEX),
    SURVIVAL(Ability.WIS),
    /** Tool: picking locks and disarming traps. */
    THIEVES_TOOLS(Ability.DEX),
    /** Tool: mechanisms and contraptions (Artificer). */
    TINKERS_TOOLS(Ability.INT),
    /** Tool: brewing and reagents (Alchemist). */
    ALCHEMISTS_SUPPLIES(Ability.INT);

    private final Ability ability;

    Skill(Ability ability) {
        this.ability = ability;
    }

    public Ability ability() {
        return ability;
    }

    /** Lower-case id for commands and data ("thieves_tools"). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "skill.dndclasses." + id();
    }

    /** Parses "stealth" or "STEALTH"; null if unknown. */
    @Nullable
    public static Skill byId(String text) {
        try {
            return valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
