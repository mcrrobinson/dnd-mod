package mattonfire.dnd.classes.Abilities;

/** Advantage rolls 2d20 and keeps the higher, disadvantage keeps the lower. */
public enum Advantage {
    NORMAL,
    ADVANTAGE,
    DISADVANTAGE;

    /** 5e: any amount of advantage and disadvantage together cancel to a straight roll. */
    public static Advantage resolve(int advantages, int disadvantages) {
        if (advantages > 0 && disadvantages == 0) {
            return ADVANTAGE;
        }
        if (disadvantages > 0 && advantages == 0) {
            return DISADVANTAGE;
        }
        return NORMAL;
    }
}
