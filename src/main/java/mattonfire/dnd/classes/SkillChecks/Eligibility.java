package mattonfire.dnd.classes.SkillChecks;

/**
 * How well trained a character is for a check. Class-gated obstacles use it to decide who may try
 * and which placeholder modifier they roll at ({@link SkillModifiers}).
 */
public enum Eligibility {
    /** The class the check is built for: +3 ability, plus proficiency. */
    PRIMARY,
    /** A class that can also do it: +1 ability, plus proficiency. */
    SECONDARY,
    /** Anyone may try, at +0. */
    UNTRAINED,
    /** Not allowed to try at all. */
    NONE;

    public boolean canTry() {
        return this != NONE;
    }
}
