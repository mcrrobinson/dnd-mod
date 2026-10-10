package mattonfire.dnd.classes.Obstacles;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Hooks for other systems: quests listen for {@link #SOLVED}, dungeons wake their rooms on
 * {@link #ALARM}.
 */
public final class ObstacleEvents {
    /** Radius, in blocks, that a fumble's alarm reaches. */
    public static final int ALARM_RADIUS = 16;

    private ObstacleEvents() {
    }

    @FunctionalInterface
    public interface Solved {
        /** {@code solver} got past the obstacle group at {@code pos} by its class route (not by breaking it). */
        void onSolved(ServerPlayerEntity solver, ObstacleType type, Tier tier, BlockPos pos);
    }

    @FunctionalInterface
    public interface Alarm {
        /** Something loud happened at {@code pos}: dormant mobs within {@code radius} should wake. */
        void onAlarm(ServerWorld world, BlockPos pos, int radius, @Nullable ServerPlayerEntity cause);
    }

    public static final Event<Solved> SOLVED = EventFactory.createArrayBacked(Solved.class,
            listeners -> (solver, type, tier, pos) -> {
                for (Solved listener : listeners) {
                    listener.onSolved(solver, type, tier, pos);
                }
            });

    public static final Event<Alarm> ALARM = EventFactory.createArrayBacked(Alarm.class,
            listeners -> (world, pos, radius, cause) -> {
                for (Alarm listener : listeners) {
                    listener.onAlarm(world, pos, radius, cause);
                }
            });
}
