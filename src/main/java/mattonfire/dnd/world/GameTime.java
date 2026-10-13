package mattonfire.dnd.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

/**
 * The in-game clock: day numbers and the night window. Day numbers are {@code timeOfDay / 24000}; pass the
 * world whose clock counts (the overworld for anything kept per player, as {@link #day(MinecraftServer)}).
 */
public final class GameTime {
    /** Ticks in one in-game day. */
    public static final long TICKS_PER_DAY = 24000L;
    /** Night runs from dusk ({@code timeOfDay % 24000 >= 13000})... */
    public static final long DUSK = 13000L;
    /** ...to dawn ({@code < 23000}). */
    public static final long DAWN = 23000L;

    private GameTime() {
    }

    /** The day number on {@code world}'s clock. */
    public static long day(World world) {
        return world.getTimeOfDay() / TICKS_PER_DAY;
    }

    /** The day number on the overworld clock, the same in every dimension. */
    public static long day(MinecraftServer server) {
        return day(server.getOverworld());
    }

    /** Whole days in {@code ticks}. */
    public static long days(long ticks) {
        return ticks / TICKS_PER_DAY;
    }

    /** The time within the day, 0 (sunrise) to 23999. */
    public static long timeOfDay(World world) {
        return world.getTimeOfDay() % TICKS_PER_DAY;
    }

    /** Whether it's night on {@code world}'s clock: {@link #DUSK} (inclusive) to {@link #DAWN} (exclusive). */
    public static boolean isNight(World world) {
        long time = timeOfDay(world);
        return time >= DUSK && time < DAWN;
    }
}
