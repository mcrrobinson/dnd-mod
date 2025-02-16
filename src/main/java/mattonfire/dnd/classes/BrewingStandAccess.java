package mattonfire.dnd.classes;

import java.util.UUID;

public interface BrewingStandAccess {
    DndCharacter getLastPlayer();

    void setLastPlayer(DndCharacter playerUUID);
}
