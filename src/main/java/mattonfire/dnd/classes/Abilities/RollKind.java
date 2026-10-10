package mattonfire.dnd.classes.Abilities;

/** What a d20 roll is for. Decides which sheet bonuses and advantage filters apply. */
public enum RollKind {
    /** Ability or skill check against a DC. */
    CHECK,
    SAVE,
    ATTACK,
    /** Death saving throw (flat d20 vs 10). */
    DEATH,
    /** Lore check on a creature. */
    STUDY,
    /** A plain d20 with no sheet bonus (DM "flat" rolls). */
    FLAT
}
