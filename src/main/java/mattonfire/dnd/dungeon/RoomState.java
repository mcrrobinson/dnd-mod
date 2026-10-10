package mattonfire.dnd.dungeon;

/** Where a room's ward is in its encounter: not yet entered, fighting (sealed), or done. */
public enum RoomState {
    UNTOUCHED,
    ACTIVE,
    CLEARED;

    public static RoomState byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return UNTOUCHED;
        }
    }
}
