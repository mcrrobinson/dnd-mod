package mattonfire.dnd.entity.raid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

/**
 * Goblin raids in one world: starts them, ticks them and saves them with the world.
 *
 * <p>At night, every {@link #CHECK_INTERVAL} ticks, each player in or near a hobbit village or
 * dwarven fortress has a {@code 1 in} {@link #RAID_CHANCE} chance of a war party setting out for it,
 * unless that settlement was raided in the last {@link #SETTLEMENT_COOLDOWN} ticks. Turn raids off
 * with {@code /gamerule dndGoblinRaids false}; start one by hand with {@code /goblinraid start}.
 */
public class GoblinRaids extends PersistentState {
    public static final GameRules.Key<GameRules.BooleanRule> DO_GOBLIN_RAIDS = GameRuleRegistry.register(
            "dndGoblinRaids", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));

    private static final String ID = DnDClasses.MOD_ID + "_goblin_raids";
    private static final int CHECK_INTERVAL = 600;
    private static final int RAID_CHANCE = 20;
    /** Three in-game days between raids on the same settlement. */
    private static final long SETTLEMENT_COOLDOWN = 24000L * 3;
    /** How close (blocks outside its bounds) a player must be for their settlement to be raided. */
    private static final int NEAR_SETTLEMENT = 32;

    /** Raiders of ended raids that loaded this tick (server thread only). */
    private static final List<Entity> LEFTOVERS = new ArrayList<>();

    private final List<GoblinRaid> raids = new ArrayList<>();
    /** Settlement key -> game time its last raid started. */
    private final Map<Long, Long> lastRaid = new HashMap<>();
    private int nextId = 1;

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> get(world).tick(world));
        ServerTickEvents.END_SERVER_TICK.register(server -> sendAwayLeftovers());
        ServerWorldEvents.UNLOAD.register((server, world) -> get(world).raids.forEach(GoblinRaid::clearBar));
        ServerEntityEvents.ENTITY_LOAD.register(GoblinRaids::onEntityLoad);
    }

    /**
     * Raiders are persistent, so one that was unloaded when its raid ended (withdrew, was won or
     * called off) or that the raid gave up on would otherwise stay by the settlement for good.
     * Send it away as soon as it loads.
     */
    private static void onEntityLoad(Entity entity, ServerWorld world) {
        if (!entity.getCommandTags().contains(GoblinRaid.RAIDER_TAG)) {
            return;
        }
        for (GoblinRaid raid : get(world).raids) {
            if (!raid.isOver() && raid.hasRaider(entity.getUuid())) {
                return;
            }
        }
        // Not removed mid-load; the world tick does it.
        LEFTOVERS.add(entity);
    }

    private static void sendAwayLeftovers() {
        if (LEFTOVERS.isEmpty()) {
            return;
        }
        for (Entity entity : List.copyOf(LEFTOVERS)) {
            if (!entity.isRemoved() && entity.world instanceof ServerWorld world) {
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getBodyY(0.5D), entity.getZ(),
                        15, 0.4D, 0.6D, 0.4D, 0.02D);
                DnDClasses.LOGGER.info("[GoblinRaid] sent away a raider left over from an ended raid at {}",
                        entity.getBlockPos().toShortString());
                entity.discard();
            }
        }
        LEFTOVERS.clear();
    }

    public static GoblinRaids get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(GoblinRaids::fromNbt, GoblinRaids::new, ID);
    }

    public List<GoblinRaid> getRaids() {
        return List.copyOf(this.raids);
    }

    private void tick(ServerWorld world) {
        if (world.getTime() % CHECK_INTERVAL == 0) {
            // Settlements off cooldown don't need remembering.
            long now = world.getTime();
            if (this.lastRaid.values().removeIf(time -> now - time >= SETTLEMENT_COOLDOWN)) {
                this.markDirty();
            }
            this.maybeStartRaids(world);
        }
        if (this.raids.isEmpty()) {
            return;
        }
        for (GoblinRaid raid : this.raids) {
            raid.tick(world);
        }
        this.raids.removeIf(GoblinRaid::isOver);
        // Raids change every tick; save whatever state they're in when the world saves.
        this.markDirty();
    }

    private void maybeStartRaids(ServerWorld world) {
        if (world.getRegistryKey() != World.OVERWORLD || world.getDifficulty() == Difficulty.PEACEFUL
                || !world.getGameRules().getBoolean(DO_GOBLIN_RAIDS) || !world.isNight()) {
            return;
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator() || player.isCreative() || mattonfire.dnd.dm.DungeonMaster.isDm(player)
                    || world.random.nextInt(RAID_CHANCE) != 0) {
                continue;
            }
            Optional<Settlement> settlement = Settlement.find(world, player.getBlockPos(), 2, NEAR_SETTLEMENT);
            if (settlement.isPresent() && this.canRaid(world, settlement.get())) {
                this.start(world, settlement.get());
            }
        }
    }

    private boolean canRaid(ServerWorld world, Settlement settlement) {
        if (this.raids.stream().anyMatch(raid -> raid.getSettlementKey() == settlement.key())) {
            return false;
        }
        Long last = this.lastRaid.get(settlement.key());
        return last == null || world.getTime() - last >= SETTLEMENT_COOLDOWN;
    }

    /** Sends a war party against {@code settlement}, or returns null if one is already attacking it. */
    public GoblinRaid start(ServerWorld world, Settlement settlement) {
        if (this.raids.stream().anyMatch(raid -> raid.getSettlementKey() == settlement.key())) {
            return null;
        }
        GoblinRaid raid = new GoblinRaid(this.nextId++, settlement, world.getDifficulty());
        this.raids.add(raid);
        this.lastRaid.put(settlement.key(), world.getTime());
        this.markDirty();
        DnDClasses.LOGGER.info("[GoblinRaid] raid {} on {} at {}", raid.getId(), settlement.kind().id,
                settlement.rally().toShortString());
        return raid;
    }

    /** Calls off the raid nearest {@code pos}, sending its goblins away. */
    public Optional<GoblinRaid> stopNearest(ServerWorld world, BlockPos pos) {
        Optional<GoblinRaid> nearest = this.raids.stream()
                .min(Comparator.comparingDouble(raid -> raid.getRally().getSquaredDistance(pos)));
        nearest.ifPresent(raid -> {
            raid.withdraw(world);
            this.raids.remove(raid);
            this.markDirty();
        });
        return nearest;
    }

    // ---- Saving ----

    private static GoblinRaids fromNbt(NbtCompound nbt) {
        GoblinRaids raids = new GoblinRaids();
        raids.nextId = Math.max(1, nbt.getInt("NextId"));
        for (NbtElement element : nbt.getList("Raids", NbtElement.COMPOUND_TYPE)) {
            raids.raids.add(GoblinRaid.fromNbt((NbtCompound) element));
        }
        for (NbtElement element : nbt.getList("LastRaids", NbtElement.COMPOUND_TYPE)) {
            NbtCompound entry = (NbtCompound) element;
            raids.lastRaid.put(entry.getLong("Settlement"), entry.getLong("Time"));
        }
        return raids;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("NextId", this.nextId);
        NbtList raids = new NbtList();
        for (GoblinRaid raid : this.raids) {
            raids.add(raid.toNbt());
        }
        nbt.put("Raids", raids);
        NbtList lastRaids = new NbtList();
        this.lastRaid.forEach((key, time) -> {
            NbtCompound entry = new NbtCompound();
            entry.putLong("Settlement", key);
            entry.putLong("Time", time);
            lastRaids.add(entry);
        });
        nbt.put("LastRaids", lastRaids);
        return nbt;
    }
}
