package mattonfire.dnd.classes.Rest;

/** A short rest (campfire, 30 s) or a long rest (bed, tavern room, party camp). */
public enum RestKind {
    SHORT, LONG;

    public String label() {
        return this == SHORT ? "short rest" : "long rest";
    }
}
