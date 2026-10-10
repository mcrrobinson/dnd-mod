package mattonfire.dnd.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import mattonfire.dnd.entity.boss.BossMinions;
import mattonfire.dnd.tavern.Bounty;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Where reputation comes from: kills and hits (hooked here), plus the calls existing systems make
 * ({@link #raidWon}, {@link #bountyClaimed}, {@link #traded}, {@link #theftWitnessed}). The amounts
 * all come from the faction files.
 */
public final class FactionEvents {
    private static final int DECAY_CHECK_INTERVAL = 100;
    /** A dwarf seeing you at the hoard costs standing at most once per this many ticks. */
    private static final int THEFT_COOLDOWN = 200;
    private static final Map<UUID, Long> lastTheft = new HashMap<>();

    private FactionEvents() {
    }

    public static void register() {
        Factions.register();
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            ServerPlayerEntity player = responsible(killer);
            if (player != null) {
                onKill(player, killed);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            ServerPlayerEntity player = responsible(source.getAttacker());
            // The killing blow is charged as a kill, not a hit too.
            if (player != null && entity != player && amount > 0.0F && amount < entity.getHealth()) {
                onHit(player, entity);
            }
            return true;
        });
        ServerTickEvents.END_SERVER_TICK.register(FactionEvents::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Reputation.sync(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> lastTheft.remove(handler.getPlayer().getUuid()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Reputation.sync(newPlayer));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) ->
                server.getPlayerManager().getPlayerList().forEach(Reputation::sync));
    }

    /** The player behind a kill or hit: the player, or the owner of their pet. */
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

    static void onKill(ServerPlayerEntity player, LivingEntity killed) {
        if (BossMinions.isMinion(killed)) {
            return;
        }
        Reputation.Change change = Reputation.change(player, Reputation.Source.KILL);
        for (Faction faction : Factions.all()) {
            if (faction.isMember(killed)) {
                int penalty = faction.killPenalty(killed);
                if (faction.betrayal() != 0 && Reputation.tier(player, faction).atLeast(ReputationTier.NEUTRAL)) {
                    penalty = Math.min(penalty, faction.betrayal());
                }
                change.add(faction, penalty);
                continue;
            }
            for (Map.Entry<Identifier, Integer> enemy : faction.enemyKills().entrySet()) {
                Faction enemyFaction = Factions.get(enemy.getKey());
                if (enemyFaction == null || !enemyFaction.isMember(killed)) {
                    continue;
                }
                Integer special = Faction.lookup(faction.enemyKillOverrides(), killed);
                if (special != null) {
                    change.add(faction, special);
                } else {
                    change.add(faction, enemy.getValue(), Reputation.Cap.KILL, faction.killCap());
                }
                break;
            }
        }
        change.apply();
    }

    private static void onHit(ServerPlayerEntity player, LivingEntity hit) {
        Reputation.Change change = Reputation.change(player, Reputation.Source.HIT);
        for (Faction faction : Factions.all()) {
            if (faction.hitMember() != 0 && faction.isMember(hit)) {
                change.add(faction, faction.hitMember());
            }
        }
        change.apply();
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % DECAY_CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Reputation.decay(player);
        }
    }

    // ---- Calls from existing systems ----

    /**
     * A goblin raid on a {@code settlement} structure (e.g. {@code dndclasses:hobbit_village}) was won
     * with {@code player} taking part: the settlement's factions gain {@code raid_won}, the raiders'
     * factions {@code raid_defeated}.
     */
    public static void raidWon(ServerPlayerEntity player, ServerWorld world, Identifier settlement, EntityType<?> raider) {
        Reputation.Change change = Reputation.change(player, Reputation.Source.RAID);
        for (Faction faction : Factions.all()) {
            if (faction.raidWon() != 0 && faction.ownsSettlement(world, settlement)) {
                change.add(faction, faction.raidWon());
            }
            if (faction.raidDefeated() != 0 && faction.members().matchesType(raider)) {
                change.add(faction, faction.raidDefeated());
            }
        }
        change.apply();
    }

    /**
     * A finished bounty was handed in at {@code pos}. The factions with a {@code bounties} reward whose
     * settlement that is gain it; if it's in none of them, every faction with a reward does.
     */
    public static void bountyClaimed(ServerPlayerEntity player, Bounty.Tier tier, BlockPos pos) {
        ServerWorld world = player.getWorld();
        Reputation.Change local = Reputation.change(player, Reputation.Source.BOUNTY);
        Reputation.Change anywhere = Reputation.change(player, Reputation.Source.BOUNTY);
        boolean found = false;
        for (Faction faction : Factions.all()) {
            int reward = tier == Bounty.Tier.MAJOR ? faction.bountyMajor() : faction.bountyMinor();
            if (reward == 0) {
                continue;
            }
            anywhere.add(faction, reward);
            if (faction.isInSettlement(world, pos)) {
                local.add(faction, reward);
                found = true;
            }
        }
        (found ? local : anywhere).apply();
    }

    /** A trade (or barter) with {@code npc}: its faction gains {@code trade}, up to {@code trade_cap} a day. */
    public static void traded(ServerPlayerEntity player, Entity npc) {
        Faction faction = Reputation.factionOf(npc);
        if (faction != null && faction.trade() != 0) {
            Reputation.change(player, Reputation.Source.TRADE)
                    .add(faction, faction.trade(), Reputation.Cap.TRADE, faction.tradeCap()).apply();
        }
    }

    /** {@code witness} saw the player meddling with their faction's things (the dwarven hoard). */
    public static void theftWitnessed(ServerPlayerEntity player, Entity witness) {
        Faction faction = Reputation.factionOf(witness);
        if (faction == null || faction.theftWitnessed() == 0) {
            return;
        }
        long now = player.getServer().getTicks();
        Long last = lastTheft.get(player.getUuid());
        if (last != null && now - last < THEFT_COOLDOWN) {
            return;
        }
        lastTheft.put(player.getUuid(), now);
        Reputation.add(player, faction, faction.theftWitnessed(), Reputation.Source.THEFT);
    }
}
