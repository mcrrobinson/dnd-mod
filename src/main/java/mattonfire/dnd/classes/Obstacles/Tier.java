package mattonfire.dnd.classes.Obstacles;

import java.util.Locale;

import net.minecraft.util.StringIdentifiable;

/** How hard an obstacle is: its DC and the class XP its solver earns. */
public enum Tier implements StringIdentifiable {
    EASY(10, 5),
    MEDIUM(13, 10),
    HARD(15, 15),
    VERY_HARD(18, 25);

    public final int dc;
    public final int xp;

    Tier(int dc, int xp) {
        this.dc = dc;
        this.xp = xp;
    }

    @Override
    public String asString() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "obstacle.dndclasses.tier." + asString();
    }

    public static Tier byName(String name, Tier fallback) {
        for (Tier tier : values()) {
            if (tier.asString().equalsIgnoreCase(name) || tier.name().equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return fallback;
    }
}
