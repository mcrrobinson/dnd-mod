package mattonfire.dnd.classes.SkillChecks;

/**
 * Whether a class may attempt a class-gated check (obstacles). It only gates the attempt and orders
 * the classes named in hints; the modifier always comes from the character sheet ({@link SkillCheck}).
 */
public enum Eligibility {
    /** The class the check is built for (named first in hints). */
    PRIMARY,
    /** A class that can also do it. */
    SECONDARY,
    /** Anyone may try (not named in hints). */
    UNTRAINED,
    /** Not allowed to try at all. */
    NONE;

    public boolean canTry() {
        return this != NONE;
    }
}
