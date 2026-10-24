package mattonfire.dnd.classes.Conditions;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * The 5e conditions the {@link Conditions} layer knows. The ordinal is the bit in the synced mask
 * ({@link Conditions#mask}), so new ones are appended, never inserted.
 */
public enum Condition {
    // Order is the sync bit order: append only.
    PRONE, FRIGHTENED, RESTRAINED, BLINDED, STUNNED, PETRIFIED, POISONED, CHARMED, INCAPACITATED, PARALYZED;

    /** Default length when a caller or command gives none (ticks; {@link Conditions#INDEFINITE} for holds). */
    public int defaultTicks() {
        return switch (this) {
            case PRONE -> 60;
            case FRIGHTENED -> 160;
            case RESTRAINED, PETRIFIED -> Conditions.INDEFINITE;
            case BLINDED -> 100;
            case STUNNED, INCAPACITATED, PARALYZED -> 60;
            case POISONED, CHARMED -> 200;
        };
    }

    public int bit() {
        return 1 << ordinal();
    }

    /** "prone", "frightened", ... (NBT, commands, lang). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** "condition.dndclasses.<id>". */
    public String translationKey() {
        return "condition.dndclasses." + id();
    }

    /** STUNNED, PETRIFIED, INCAPACITATED and PARALYZED: the creature can't act. */
    public boolean incapacitates() {
        return this == STUNNED || this == PETRIFIED || this == INCAPACITATED || this == PARALYZED;
    }

    /** RESTRAINED, STUNNED, PARALYZED and PETRIFIED root the creature. */
    public boolean roots() {
        return this == RESTRAINED || this == STUNNED || this == PARALYZED || this == PETRIFIED;
    }

    @Nullable
    public static Condition byId(String id) {
        if (id == null) {
            return null;
        }
        String lower = id.toLowerCase(Locale.ROOT);
        for (Condition c : values()) {
            if (c.id().equals(lower)) {
                return c;
            }
        }
        return null;
    }
}
