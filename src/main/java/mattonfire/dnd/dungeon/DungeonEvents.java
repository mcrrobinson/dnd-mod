package mattonfire.dnd.dungeon;

import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Hooks other systems (bounties, quests, reputation, music, rests) use to follow what happens in
 * dungeons. All are fired on the server thread.
 */
public final class DungeonEvents {
    /** A player walked into a dungeon for the first time (once per player per dungeon). */
    public static final Event<Entered> ENTERED = EventFactory.createArrayBacked(Entered.class,
            listeners -> (world, dungeon, player) -> {
                for (Entered listener : listeners) {
                    listener.onEntered(world, dungeon, player);
                }
            });

    /** A room's encounter is over and its seals are open. {@code players} were inside the room. */
    public static final Event<RoomCleared> ROOM_CLEARED = EventFactory.createArrayBacked(RoomCleared.class,
            listeners -> (world, dungeon, room, players) -> {
                for (RoomCleared listener : listeners) {
                    listener.onRoomCleared(world, dungeon, room, players);
                }
            });

    /** The dungeon's boss died. {@code players} were in the boss room at the kill. Fired before {@link #CLEARED}. */
    public static final Event<BossDefeated> BOSS_DEFEATED = EventFactory.createArrayBacked(BossDefeated.class,
            listeners -> (world, dungeon, boss, players) -> {
                for (BossDefeated listener : listeners) {
                    listener.onBossDefeated(world, dungeon, boss, players);
                }
            });

    /** The whole dungeon is cleared (boss defeated, or {@code /dungeon clear}). {@code participants} were inside it. */
    public static final Event<Cleared> CLEARED = EventFactory.createArrayBacked(Cleared.class,
            listeners -> (world, dungeon, participants) -> {
                for (Cleared listener : listeners) {
                    listener.onCleared(world, dungeon, participants);
                }
            });

    /**
     * A cleared dungeon filled up again (after its repopulation time, or {@code /dungeon reset}):
     * every room is UNTOUCHED and the boss is back. Traps re-arm and puzzles shuffle on this.
     */
    public static final Event<Repopulated> REPOPULATED = EventFactory.createArrayBacked(Repopulated.class,
            listeners -> (world, dungeon) -> {
                for (Repopulated listener : listeners) {
                    listener.onRepopulated(world, dungeon);
                }
            });

    private DungeonEvents() {
    }

    @FunctionalInterface
    public interface Entered {
        void onEntered(ServerWorld world, DungeonState dungeon, ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface RoomCleared {
        void onRoomCleared(ServerWorld world, DungeonState dungeon, DungeonState.Room room, List<ServerPlayerEntity> players);
    }

    @FunctionalInterface
    public interface BossDefeated {
        void onBossDefeated(ServerWorld world, DungeonState dungeon, LivingEntity boss, List<ServerPlayerEntity> players);
    }

    @FunctionalInterface
    public interface Cleared {
        void onCleared(ServerWorld world, DungeonState dungeon, List<ServerPlayerEntity> participants);
    }

    @FunctionalInterface
    public interface Repopulated {
        void onRepopulated(ServerWorld world, DungeonState dungeon);
    }
}
