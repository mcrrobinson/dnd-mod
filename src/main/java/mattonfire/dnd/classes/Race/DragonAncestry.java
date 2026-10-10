package mattonfire.dnd.classes.Race;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/** A Dragonborn's draconic ancestry. Every other race has NONE. Ids are stable (NBT {@code DndAncestry}). */
public enum DragonAncestry {
    NONE(0, "None", "none"),
    EMBER(1, "Ember", "fire"),
    FROST(2, "Frost", "frost"),
    STORM(3, "Storm", "lightning");

    private final int value;
    private final String displayName;
    private final String element;

    DragonAncestry(int value, String displayName, String element) {
        this.value = value;
        this.displayName = displayName;
        this.element = element;
    }

    public int getValue() {
        return value;
    }

    public String displayName() {
        return displayName;
    }

    /** The breath and resistance element, for text. */
    public String element() {
        return element;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DragonAncestry fromValue(int value) {
        for (DragonAncestry ancestry : values()) {
            if (ancestry.value == value) {
                return ancestry;
            }
        }
        throw new IllegalArgumentException("No DragonAncestry found for value: " + value);
    }

    @Nullable
    public static DragonAncestry byId(String id) {
        for (DragonAncestry ancestry : values()) {
            if (ancestry.id().equalsIgnoreCase(id)) {
                return ancestry;
            }
        }
        return null;
    }
}
