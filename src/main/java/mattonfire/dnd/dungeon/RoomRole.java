package mattonfire.dnd.dungeon;

/**
 * What a dungeon room is for. The main path runs through these in declaration order, from
 * {@link #ENTRANCE} to {@link #VAULT} ({@link #ENCOUNTER_SMALL} appears twice, the second time
 * optionally); the last few are side rooms off the main path.
 */
public enum RoomRole {
    ENTRANCE("Entrance", false),
    ANTECHAMBER("Antechamber", false),
    ENCOUNTER_SMALL("Encounter (small)", true),
    TRAP_CORRIDOR("Trap corridor", false),
    ENCOUNTER_LARGE("Encounter (large)", true),
    GATE("Class-check gate", false),
    PUZZLE("Puzzle room", false),
    CHAMPION("Mid-boss room", true),
    BOSS("Boss room", true),
    VAULT("Treasure vault", false),
    SECRET("Secret room", false),
    SIDE_VAULT("Side vault", false);

    private final String label;
    private final boolean combat;

    RoomRole(String label, boolean combat) {
        this.label = label;
        this.combat = combat;
    }

    public String label() {
        return this.label;
    }

    /** Rooms whose ward spawns an encounter when a party walks in (from ticket 2 on). */
    public boolean combat() {
        return this.combat;
    }

    /** Side rooms hang off the main path rather than lying on it. */
    public boolean branch() {
        return this == SECRET || this == SIDE_VAULT;
    }

    public static RoomRole byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return ENCOUNTER_SMALL;
        }
    }
}
