package mattonfire.dnd.quest;

import java.util.List;
import java.util.UUID;
import mattonfire.dnd.entity.LichEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.entity.PhylacteryEntity;
import mattonfire.dnd.entity.boss.BossMinions;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Hooks the quest engine into the game: kills, visits (checked every second), goblin raids, party
 * changes, joins and data pack reloads.
 */
public final class QuestEvents {
    private static final int TICK_INTERVAL = 20;

    private QuestEvents() {
    }

    public static void register() {
        Quests.register();
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            ServerPlayerEntity player = responsible(killer);
            if (player != null) {
                onKill(player, killed);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % TICK_INTERVAL == 0) {
                QuestManager manager = QuestManager.get(server);
                manager.tickVisits(server);
                manager.flushSync(server);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                QuestManager.get(server).onJoin(handler.getPlayer()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) ->
                QuestManager.get(server).resyncAll(server));
    }

    /** The player behind a kill: the player, or the owner of their pet or summon. */
    @Nullable
    private static ServerPlayerEntity responsible(@Nullable Entity entity) {
        if (entity instanceof ServerPlayerEntity player) {
            return player;
        }
        if (entity instanceof Tameable tamed && tamed.getOwner() instanceof ServerPlayerEntity owner) {
            return owner;
        }
        return null;
    }

    private static void onKill(ServerPlayerEntity player, LivingEntity killed) {
        // Boss minions never count, as for bounties.
        if (BossMinions.isMinion(killed)) {
            return;
        }
        // A Lich whose soul fled to its phylactery will reform: only its final death counts.
        if (killed instanceof LichEntity lich && lich.soulFled()) {
            return;
        }
        QuestManager manager = QuestManager.get(player.getServer());
        manager.onKill(player, killed, killed.getType());
        // Smashing the phylactery while the Lich reforms destroys the Lich for good.
        if (killed instanceof PhylacteryEntity phylactery && phylactery.isReforming()) {
            manager.onKill(player, killed, ModEntityTypes.LICH);
        }
    }

    /** Called by {@code GoblinRaid.win}: a raid on a settlement kind (e.g. {@code hobbit_village}) was won. */
    public static void onRaidWon(MinecraftServer server, List<ServerPlayerEntity> defenders, String settlement) {
        QuestManager.get(server).onRaidWon(server, defenders, settlement);
    }

    /** Called by the party commands when a player leaves or is removed from their party. */
    public static void onPartyLeft(MinecraftServer server, UUID player) {
        QuestManager.get(server).onPartyLeft(server, player);
    }
}
