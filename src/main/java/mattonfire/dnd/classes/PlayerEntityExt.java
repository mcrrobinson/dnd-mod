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

	/** Downed state bits ({@code Downed.BIT_DOWNED}, {@code Downed.BIT_STABLE}), synced to every client. */
	byte getDownedBits();

	/** Server side; use {@code Downed} instead of calling this directly. */
	void setDownedBits(byte bits);

	/** How far below 0 HP the last hit would have taken the player (massive damage), 0 if it didn't. */
	float getLastDamageOverflow();
}
