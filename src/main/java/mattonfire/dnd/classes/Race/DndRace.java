package mattonfire.dnd.classes.Race;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * A player's race, picked before the class. The ids are stable: they are saved in the player NBT
 * ({@code DndRace}), synced in the player's DataTracker and sent in the race packets.
 */
public enum DndRace {
    NONE(0),
    HUMAN(1),
    ELF(2),
    DWARF(3),
    HALFLING(4),
    GNOME(5),
    HALFORC(6),
    TIEFLING(7),
    DRAGONBORN(8);

    private final int value;

    DndRace(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /** The command and lang name: {@code human}, {@code halforc}, ... */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean hasAncestry() {
        return this == DRAGONBORN;
    }

    public static DndRace fromValue(int value) {
        for (DndRace race : values()) {
            if (race.value == value) {
                return race;
            }
        }
        throw new IllegalArgumentException("No DndRace found for value: " + value);
    }

    /** The race with this command name, or null. */
    @Nullable
    public static DndRace byId(String id) {
        for (DndRace race : values()) {
            if (race.id().equalsIgnoreCase(id)) {
                return race;
            }
        }
        return null;
    }
}
