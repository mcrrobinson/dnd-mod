package mattonfire.dnd.classes;

public interface BrewingStandAccess {
    /**
     * Whether the brew in progress was started by a non-Alchemist (one had the stand open when it
     * started and no Alchemist did). An armed stand explodes when that brew finishes.
     */
    boolean isBrewArmed();

    void setBrewArmed(boolean armed);

    /** The last player to open the stand; an Alchemist gets the brewing XP. */
    java.util.UUID getLastUser();

    void setLastUser(java.util.UUID user);
}
