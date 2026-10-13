package mattonfire.dnd.entity.boss;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** Shared viewer bookkeeping for the mod's boss bars (bosses, dungeon champions, goblin raids, the orc trial). */
public final class BossBars {
    private BossBars() {
    }

    /** {@link #syncViewers(ServerBossBar, ServerWorld, Predicate, Consumer, Consumer)} with no callbacks. */
    public static void syncViewers(ServerBossBar bar, ServerWorld world, Predicate<ServerPlayerEntity> shows) {
        syncViewers(bar, world, shows, player -> {
        }, player -> {
        });
    }

    /**
     * Makes the bar's viewers the players in {@code world} that {@code shows} accepts. A viewer who left
     * (disconnected, respawned as a new entity, or changed dimension) or no longer passes {@code shows} loses the
     * bar, then {@code removed} runs for them; a player in {@code world} who passes it and isn't a viewer yet gets
     * the bar, then {@code added} runs (for boss music, a second bar, ...).
     */
    public static void syncViewers(ServerBossBar bar, ServerWorld world, Predicate<ServerPlayerEntity> shows,
            Consumer<ServerPlayerEntity> added, Consumer<ServerPlayerEntity> removed) {
        for (ServerPlayerEntity player : List.copyOf(bar.getPlayers())) {
            if (player.isRemoved() || player.getWorld() != world || !shows.test(player)) {
                bar.removePlayer(player);
                removed.accept(player);
            }
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!bar.getPlayers().contains(player) && shows.test(player)) {
                bar.addPlayer(player);
                added.accept(player);
            }
        }
    }
}
