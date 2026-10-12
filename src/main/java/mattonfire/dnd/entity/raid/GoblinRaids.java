package mattonfire.dnd.entity.raid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.dungeon.DungeonEvents;
import mattonfire.dnd.dungeon.DungeonState;
import mattonfire.dnd.world.gen.dungeon.DungeonTheme;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.faction.TierEffects;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    /** Clearing a Goblin Warren calls off raids and quiets settlements within this many blocks of it... */
    public static final int WARREN_RADIUS = 256;
    /** ...for this long: the same three days as a raid's own cooldown. */
    public static final long WARREN_QUIET = SETTLEMENT_COOLDOWN;

    /**
     * A place goblins won't raid near until {@code until} (game time): {@code radius} blocks round
     * {@code center} (horizontally), e.g. round a cleared Goblin Warren.
     */
    public record QuietZone(BlockPos center, int radius, long until) {
        public boolean covers(BlockPos pos) {
            double dx = pos.getX() - this.center.getX();
            double dz = pos.getZ() - this.center.getZ();
            return dx * dx + dz * dz <= (double) this.radius * this.radius;
        }
    }

    /** Raiders of ended raids that loaded this tick (server thread only). */
    private static final List<Entity> LEFTOVERS = new ArrayList<>();

    private final List<GoblinRaid> raids = new ArrayList<>();
    /** Settlement key -> game time its last raid started. */
    private final Map<Long, Long> lastRaid = new HashMap<>();
    private final List<QuietZone> quiet = new ArrayList<>();
    private int nextId = 1;

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> get(world).tick(world));
        ServerTickEvents.END_SERVER_TICK.register(server -> sendAwayLeftovers());
        ServerWorldEvents.UNLOAD.register((server, world) -> get(world).raids.forEach(GoblinRaid::clearBar));
        ServerEntityEvents.ENTITY_LOAD.register(GoblinRaids::onEntityLoad);
        DungeonEvents.CLEARED.register(GoblinRaids::onDungeonCleared);
        // Settlement health: folk killed and blocks broken by hand.
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.world instanceof ServerWorld world) {
                for (GoblinRaid raid : get(world).raids) {
                    raid.onDeath(world, entity);
                }
            }
        });
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld serverWorld) {
                for (GoblinRaid raid : get(serverWorld).raids) {
                    raid.onPlayerBreak(pos);
                }
            }
        });
    }

    /**
     * A cleared Goblin Warren (its Warlord dead, or {@code /dungeon clear}) breaks the goblins' nerve
     * round it: every raid within {@link #WARREN_RADIUS} blocks is called off, and no settlement there
     * is raided for {@link #WARREN_QUIET} ticks.
     */
    private static void onDungeonCleared(ServerWorld world, DungeonState dungeon, List<ServerPlayerEntity> participants) {
        if (dungeon.theme() != DungeonTheme.GOBLIN_WARREN) {
            return;
        }
        BlockPos center = dungeon.bounds().getCenter();
        int called = get(world).quiet(world, center, WARREN_RADIUS, WARREN_QUIET);
        DnDClasses.LOGGER.info("[GoblinRaid] goblin warren {} cleared at {}: called off {} raid(s) within {} blocks; no raids there until day {}",
                dungeon.startKey(), center.toShortString(), called, WARREN_RADIUS, (world.getTime() + WARREN_QUIET) / 24000L);
        for (ServerPlayerEntity player : participants) {
            player.sendMessage(Text.translatable("raid.dndclasses.goblin.warren_cleared").formatted(Formatting.GOLD), false);
        }
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
            if (this.quiet.removeIf(zone -> now >= zone.until())) {
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
                    // Players Marked by the goblins draw raids twice as often.
                    || world.random.nextInt(TierEffects.raidChance(player, ModEntityTypes.GOBLIN_WARRIOR, RAID_CHANCE)) != 0) {
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
        if (this.isQuiet(world, settlement.rally())) {
            return false;
        }
        Long last = this.lastRaid.get(settlement.key());
        return last == null || world.getTime() - last >= SETTLEMENT_COOLDOWN;
    }

    // ---- Cooldowns ----

    /** Puts {@code settlementKey} on the raid cooldown from now, as if a raid on it had just started. */
    public void startCooldown(ServerWorld world, long settlementKey) {
        this.lastRaid.put(settlementKey, world.getTime());
        this.markDirty();
    }

    /** Whether a settlement at {@code pos} is in a quiet zone now (natural raids skip it). */
    public boolean isQuiet(ServerWorld world, BlockPos pos) {
        long now = world.getTime();
        return this.quiet.stream().anyMatch(zone -> now < zone.until() && zone.covers(pos));
    }

    public List<QuietZone> getQuietZones() {
        return List.copyOf(this.quiet);
    }

    /**
     * Calls off every raid whose rally point is within {@code radius} blocks of {@code center}
     * (putting its settlement back on cooldown), and keeps natural raids away from there for
     * {@code ticks}. Returns how many raids were called off.
     */
    public int quiet(ServerWorld world, BlockPos center, int radius, long ticks) {
        QuietZone zone = new QuietZone(center.toImmutable(), radius, world.getTime() + ticks);
        int called = 0;
        for (GoblinRaid raid : List.copyOf(this.raids)) {
            if (!raid.isOver() && zone.covers(raid.getRally())) {
                raid.withdraw(world, "raid.dndclasses.goblin.routed");
                this.raids.remove(raid);
                this.startCooldown(world, raid.getSettlementKey());
                called++;
            }
        }
        this.quiet.add(zone);
        this.markDirty();
        return called;
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
        for (NbtElement element : nbt.getList("Quiet", NbtElement.COMPOUND_TYPE)) {
            NbtCompound entry = (NbtCompound) element;
            raids.quiet.add(new QuietZone(BlockPos.fromLong(entry.getLong("Center")), entry.getInt("Radius"), entry.getLong("Until")));
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
        NbtList quiet = new NbtList();
        for (QuietZone zone : this.quiet) {
            NbtCompound entry = new NbtCompound();
            entry.putLong("Center", zone.center().asLong());
            entry.putInt("Radius", zone.radius());
            entry.putLong("Until", zone.until());
            quiet.add(entry);
        }
        nbt.put("Quiet", quiet);
        return nbt;
    }
}
