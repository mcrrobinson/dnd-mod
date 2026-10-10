package mattonfire.dnd.classes.Obstacles;

import net.minecraft.util.StringIdentifiable;

/** An obstacle block is SEALED (in the way) until someone gets past it, then OPEN (no collision). */
public enum ObstacleState implements StringIdentifiable {
    SEALED("sealed"),
    OPEN("open");

    private final String name;

    ObstacleState(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return name;
    }
}
