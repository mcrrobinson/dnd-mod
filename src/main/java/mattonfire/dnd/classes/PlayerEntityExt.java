package mattonfire.dnd.classes;

import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.DragonAncestry;

public interface PlayerEntityExt {
	void setDndClass(DndCharacter classID);

	DndCharacter getDndClass();

	/** Never null. Synced to every client through the player's DataTracker. */
	DndRace getDndRace();

	/** NONE unless the race is Dragonborn. */
	DragonAncestry getDragonAncestry();

	/** Server side: saves and syncs the race (and recalculates the hitbox, for racial sizes). */
	void setDndRace(DndRace race, DragonAncestry ancestry);
}
