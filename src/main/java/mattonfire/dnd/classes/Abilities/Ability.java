package mattonfire.dnd.classes.Abilities;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/** The six ability scores, in sheet order. */
public enum Ability {
    STR,
    DEX,
    CON,
    INT,
    WIS,
    CHA;

    /** "STR", "DEX"... as used in class_info.json and commands. */
    public String shortKey() {
        return name();
    }

    /** Lower-case id for commands and data ("str"). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Full name ("Strength"). */
    public String translationKey() {
        return "ability.dndclasses." + id();
    }

    /** Short name ("STR"). */
    public String shortTranslationKey() {
        return translationKey() + ".short";
    }

    /** "DEX save" style label for a saving throw with this ability. */
    public String saveTranslationKey() {
        return translationKey() + ".save";
    }

    /** Parses "dex", "DEX" or "dexterity"; null if unknown. */
    @Nullable
    public static Ability byId(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        for (Ability a : values()) {
            if (a.name().equals(upper) || upper.length() > 3 && fullName(a).startsWith(upper)) {
                return a;
            }
        }
        return null;
    }

    private static String fullName(Ability a) {
        return switch (a) {
            case STR -> "STRENGTH";
            case DEX -> "DEXTERITY";
            case CON -> "CONSTITUTION";
            case INT -> "INTELLIGENCE";
            case WIS -> "WISDOM";
            case CHA -> "CHARISMA";
        };
    }

    /** 5e modifier: floor((score - 10) / 2). */
    public static int modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }
}
