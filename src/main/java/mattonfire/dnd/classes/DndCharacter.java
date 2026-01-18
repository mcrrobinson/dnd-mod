package mattonfire.dnd.classes;

public enum DndCharacter {
    NONE(0),
    BARBARIAN(1),
    BARD(2),
    CLERIC(3),
    DRUID(4),
    FIGHTER(5),
    MONK(6),
    PALADIN(7),
    RANGER(8),
    ROGUE(9),
    NECROMANCER(10),
    WARLOCK(11),
    WIZARD(12),
    ARTIFICER(13),
    BLOODHUNTER(14),
    ALCHEMIST(15);

    private int value = 0;

    DndCharacter(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DndCharacter fromValue(int value) {
        for (DndCharacter character : DndCharacter.values()) {
            if (character.getValue() == value) {
                return character;
            }
        }
        throw new IllegalArgumentException("No DndCharacter found for value: " + value);
    }
}
