package mattonfire.dnd.dm;

import mattonfire.dnd.classes.mixin.ThreadedAnvilChunkStorageAccessor;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * The veil ({@code /dm veil}): a veiled DM isn't sent to non-DM clients at all ({@code DmEntityTrackerMixin}),
 * can fly, takes no damage, makes no sound, and mobs can't target them (invulnerable abilities fail vanilla's
 * {@code TargetPredicate}; {@code ActiveTargetGoalMixin} also drops DMs). Unlike spectator mode the DM can still
 * use blocks, chests and items.
 */
public final class DmVeil {
    private DmVeil() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !DungeonMaster.isVeiled(entity));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> apply(handler.getPlayer()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> apply(newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(DmVeil::tick);
    }

    /** True if {@code viewer}'s client must not see {@code entity}: a veiled DM, seen by a non-DM. */
    public static boolean hiddenFrom(Entity entity, ServerPlayerEntity viewer) {
        return entity instanceof ServerPlayerEntity && DungeonMaster.isVeiled(entity) && !DungeonMaster.isDm(viewer);
    }

    /** Applies or lifts the veil to match the saved state, and re-checks who can see the player. */
    public static void apply(ServerPlayerEntity player) {
        boolean veiled = DungeonMaster.isVeiled(player);
        PlayerAbilities abilities = player.getAbilities();
        if (veiled) {
            abilities.allowFlying = true;
            abilities.invulnerable = true;
        } else {
            // Back to what the game mode gives (creative keeps flight, survival loses it).
            player.interactionManager.getGameMode().setAbilities(abilities);
        }
        player.setSilent(veiled);
        // Falling while veiled (invulnerable) mustn't land as fall damage once the veil lifts.
        player.fallDistance = 0.0F;
        player.sendAbilitiesUpdate();
        refreshTracking(player.getServer());
    }

    /** Re-runs tracking for every player, so a veil change shows or hides them at once. */
    public static void refreshTracking(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            var trackers = ((ThreadedAnvilChunkStorageAccessor) world.getChunkManager().threadedAnvilChunkStorage)
                    .dnd$getEntityTrackers();
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (trackers.get(player.getId()) instanceof DmTracked tracker) {
                    tracker.dnd$updateTrackedStatus(world.getPlayers());
                }
            }
        }
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) {
            return;
        }
        // Game mode changes reset abilities; put the veil's back.
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (DungeonMaster.isVeiled(player)) {
                PlayerAbilities abilities = player.getAbilities();
                if (!abilities.allowFlying || !abilities.invulnerable || !player.isSilent()) {
                    apply(player);
                }
            }
        }
    }
}
