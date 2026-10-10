package mattonfire.dnd.classes.Abilities;

/** How much of the proficiency bonus a save or skill adds. Higher levels win when sources overlap. */
public enum Proficiency {
    NONE,
    /** Half, rounded down (the Bard's Jack of All Trades). */
    HALF,
    PROFICIENT,
    /** Double (Rogue and Bard signature skills). */
    EXPERTISE;

    public int bonus(int proficiencyBonus) {
        return switch (this) {
            case NONE -> 0;
            case HALF -> proficiencyBonus / 2;
            case PROFICIENT -> proficiencyBonus;
            case EXPERTISE -> proficiencyBonus * 2;
        };
    }

    public Proficiency max(Proficiency other) {
        return other.ordinal() > ordinal() ? other : this;
    }

    /** Sheet marker: ● proficient, ◆ expertise, ½ half. */
    public String symbol() {
        return switch (this) {
            case NONE -> "";
            case HALF -> "½";
            case PROFICIENT -> "●";
            case EXPERTISE -> "◆";
        };
    }
}
