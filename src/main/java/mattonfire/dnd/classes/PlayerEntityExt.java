package mattonfire.dnd.classes;

public interface PlayerEntityExt {
	void setDndClass(DndCharacter classID);

	int addProgress(DndCharacter character, int amount);

	int getProgress(DndCharacter character);

	DndCharacter getDndClass();
}