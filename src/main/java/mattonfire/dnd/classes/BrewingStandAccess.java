package mattonfire.dnd.classes;

public interface BrewingStandAccess {
    DndCharacter getLastPlayer();

    void setLastPlayer(DndCharacter playerUUID);

    /** The last player to open the stand; an Alchemist gets the brewing XP. */
    java.util.UUID getLastUser();

    void setLastUser(java.util.UUID user);
}
