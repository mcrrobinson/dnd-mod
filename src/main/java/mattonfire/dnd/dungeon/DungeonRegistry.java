package mattonfire.dnd.dungeon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mattonfire.dnd.classes.Blocks.DungeonWardBlockEntity;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.BlockEntity;
import mattonfire.dnd.world.gen.dungeon.DungeonPiece;
import mattonfire.dnd.world.gen.dungeon.DungeonStructure;
import mattonfire.dnd.world.gen.dungeon.DungeonTheme;
import mattonfire.dnd.world.gen.dungeon.StairPiece;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.gen.structure.Structure;

/**
 * Every dungeon this world has seen, saved with the world ({@code data/dndclasses_dungeons.dat}
 * in each dimension). Entries are made lazily from the structure start, the first time a ward
 * ticks or a command asks, so dungeons nobody has visited cost nothing.
 *
 * Query API for other systems: {@link #find}, {@link #containing}, {@link #roomAt},
 * {@link #isInsideUncleared} and {@link #nearest}.
 */
public class DungeonRegistry extends PersistentState {
    private static final String ID = DnDClasses.MOD_ID + "_dungeons";
    /** Structures that count as dungeons. */
    public static final TagKey<Structure> DUNGEONS = TagKey.of(RegistryKeys.STRUCTURE, new Identifier(DnDClasses.MOD_ID, "dungeons"));

    private static final int WARD_SEARCH_CHUNKS = 4;
    /** A cleared dungeon fills up again after 7 in-game days (on the game clock or the day clock)... */
    public static final long REPOPULATE_TICKS = 7L * 24000L;
    /** ...once nobody has been inside it for 30 s. */
    public static final long REPOPULATE_EMPTY_TICKS = 600L;
    private static final int REPOPULATE_CHECK_INTERVAL = 100;

    private final Map<Long, DungeonState> dungeons = new HashMap<>();

    public static DungeonRegistry get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(DungeonRegistry::fromNbt, DungeonRegistry::new, ID);
    }

    public List<DungeonState> all() {
        return new ArrayList<>(this.dungeons.values());
    }

    public DungeonState get(long startKey) {
        return this.dungeons.get(startKey);
    }

    /** The dungeon of this structure start, recording it the first time. Null if it isn't a dungeon. */
    public DungeonState getOrCreate(StructureStart start) {
        if (start == null || !start.hasChildren() || !(start.getStructure() instanceof DungeonStructure structure)) {
            return null;
        }
        long key = start.getPos().toLong();
        DungeonState existing = this.dungeons.get(key);
        if (existing != null) {
            return existing;
        }
        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof DungeonPiece p && p.summary() != null) {
                return this.getOrCreate(p.summary());
            }
        }
        List<DungeonState.Room> rooms = new ArrayList<>();
        int tier = 1;
        int floorY = start.getBoundingBox().getMinY() + 1;
        BlockPos entrance = start.getBoundingBox().getCenter();
        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof DungeonPiece p) {
                tier = p.tier();
                floorY = p.floorY();
                if (p.roomId() >= 0) {
                    rooms.add(new DungeonState.Room(p.roomId(), p.role(), p.getBoundingBox()));
                }
                if (p instanceof StairPiece stair) {
                    entrance = stair.top();
                }
            }
        }
        rooms.sort((a, b) -> Integer.compare(a.id(), b.id()));
        DungeonState state = new DungeonState(key, structure.theme(), tier, start.getBoundingBox(), entrance, floorY, rooms);
        state.owner = this;
        this.dungeons.put(key, state);
        this.markDirty();
        return state;
    }

    /**
     * The dungeon described by {@code summary} (a fresh {@link DungeonState#toNbt()}, written into
     * every ward at generation), recording it the first time. Works for dungeons made with
     * /place structure too, which leaves no structure start behind to look up.
     */
    public DungeonState getOrCreate(NbtCompound summary) {
        long key = summary.getLong("StartKey");
        DungeonState existing = this.dungeons.get(key);
        if (existing != null) {
            return existing;
        }
        DungeonState state = DungeonState.fromNbt(summary);
        state.owner = this;
        this.dungeons.put(key, state);
        this.markDirty();
        return state;
    }

    /** The dungeon whose bounds hold {@code pos}, from those already recorded. */
    public Optional<DungeonState> containing(BlockPos pos) {
        for (DungeonState state : this.dungeons.values()) {
            if (state.contains(pos)) {
                return Optional.of(state);
            }
        }
        return Optional.empty();
    }

    /** The room at {@code pos} in a recorded dungeon, or null. */
    public DungeonState.Room roomAt(BlockPos pos) {
        return this.containing(pos).map(state -> state.roomAt(pos)).orElse(null);
    }

    /** For rest rules: inside a dungeon whose boss is still alive. */
    public boolean isInsideUncleared(BlockPos pos) {
        return this.containing(pos).map(state -> !state.isCleared()).orElse(false);
    }

    /**
     * The nearest recorded dungeon within {@code maxDistance} blocks (horizontally) of the entrance,
     * optionally of one theme and/or not yet cleared. Only dungeons someone has been near are
     * recorded; finding unvisited ones (for maps and bounties) is a later ticket.
     */
    public Optional<DungeonState> nearest(BlockPos pos, DungeonTheme theme, boolean unclearedOnly, double maxDistance) {
        DungeonState best = null;
        double bestDist = maxDistance * maxDistance;
        for (DungeonState state : this.dungeons.values()) {
            if ((theme != null && state.theme() != theme) || (unclearedOnly && state.isCleared())) {
                continue;
            }
            double dx = state.entrance().getX() - pos.getX();
            double dz = state.entrance().getZ() - pos.getZ();
            double dist = dx * dx + dz * dz;
            if (dist <= bestDist) {
                bestDist = dist;
                best = state;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * The dungeon at or nearest to {@code pos}: the one whose pieces hold it, else one whose pieces
     * reach the chunks round it (standing over it on the surface counts), else the nearest
     * recorded one within 256 blocks.
     */
    public static Optional<DungeonState> find(ServerWorld world, BlockPos pos) {
        DungeonRegistry registry = get(world);
        StructureStart start = findStart(world, pos);
        if (start != null) {
            return Optional.ofNullable(registry.getOrCreate(start));
        }
        Optional<DungeonState> known = registry.containing(pos);
        if (known.isPresent()) {
            return known;
        }
        // /place structure leaves no start behind, but its wards know their dungeon
        DungeonState fromWard = registry.fromNearbyWard(world, pos);
        return fromWard != null ? Optional.of(fromWard) : registry.nearest(pos, null, false, 256);
    }

    /** The dungeon of the nearest ward in the loaded chunks within {@link #WARD_SEARCH_CHUNKS} of {@code pos}. */
    private DungeonState fromNearbyWard(ServerWorld world, BlockPos pos) {
        ChunkPos center = new ChunkPos(pos);
        DungeonWardBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = center.x - WARD_SEARCH_CHUNKS; x <= center.x + WARD_SEARCH_CHUNKS; x++) {
            for (int z = center.z - WARD_SEARCH_CHUNKS; z <= center.z + WARD_SEARCH_CHUNKS; z++) {
                if (!world.getChunkManager().isChunkLoaded(x, z)) {
                    continue;
                }
                for (BlockEntity be : world.getChunk(x, z).getBlockEntities().values()) {
                    if (be instanceof DungeonWardBlockEntity ward && ward.getSummary() != null) {
                        double dist = be.getPos().getSquaredDistance(pos);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = ward;
                        }
                    }
                }
            }
        }
        return best == null ? null : this.getOrCreate(best.getSummary());
    }

    /** The dungeon structure start at or next to {@code pos}, or null. */
    public static StructureStart findStart(ServerWorld world, BlockPos pos) {
        StructureStart containing = world.getStructureAccessor().getStructureContaining(pos, DUNGEONS);
        if (containing.hasChildren()) {
            return containing;
        }
        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Set<Structure> dungeons = new HashSet<>();
        registry.iterateEntries(DUNGEONS).forEach(entry -> dungeons.add(entry.value()));
        if (dungeons.isEmpty()) {
            return null;
        }
        ChunkPos center = new ChunkPos(pos);
        StructureStart best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = center.x - 1; x <= center.x + 1; x++) {
            for (int z = center.z - 1; z <= center.z + 1; z++) {
                for (StructureStart start : world.getStructureAccessor().getStructureStarts(new ChunkPos(x, z), dungeons::contains)) {
                    if (!start.hasChildren()) {
                        continue;
                    }
                    double dist = start.getBoundingBox().getCenter().getSquaredDistance(pos);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = start;
                    }
                }
            }
        }
        return best;
    }

    /** Players (not spectators) inside the dungeon's bounds. */
    public static List<ServerPlayerEntity> playersInside(ServerWorld world, DungeonState state) {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!player.isSpectator() && state.contains(player.getBlockPos())) {
                players.add(player);
            }
        }
        return players;
    }

    /** Clears the dungeon (boss down, every room open) and tells everyone listening. */
    public static void clear(ServerWorld world, DungeonState state) {
        state.markCleared(world.getTime());
        state.setClearedAtDay(world.getTimeOfDay());
        DungeonEvents.CLEARED.invoker().onCleared(world, state, playersInside(world, state));
    }

    /**
     * Ticks until a cleared dungeon may repopulate (0 = due, once it's empty), or -1 if it isn't
     * cleared. Counts on both the game clock and the day clock, so sleeping and {@code /time add}
     * bring it closer.
     */
    public static long repopulatesIn(ServerWorld world, DungeonState state) {
        if (!state.isCleared()) {
            return -1L;
        }
        long elapsed = world.getTime() - state.clearedAt();
        if (state.clearedAtDay() >= 0) {
            elapsed = Math.max(elapsed, world.getTimeOfDay() - state.clearedAtDay());
        }
        return Math.max(0L, REPOPULATE_TICKS - elapsed);
    }

    /**
     * Fresh monsters and a fresh boss: every room back to UNTOUCHED (traps and the puzzle hook in
     * through {@link DungeonEvents#REPOPULATED}). Hoard Coffer claims stay, so players who already
     * had their roll get the salvage roll after the next clear. Also what {@code /dungeon reset} does.
     */
    public static void repopulate(ServerWorld world, DungeonState state) {
        state.reset();
        DnDClasses.LOGGER.info("[Dungeon] {} repopulated (cleared {} time(s) so far)", state.startKey(), state.clears());
        DungeonEvents.REPOPULATED.invoker().onRepopulated(world, state);
    }

    /** Every few seconds: repopulates cleared dungeons whose time is up and that have stood empty a while. */
    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % REPOPULATE_CHECK_INTERVAL != 0) {
                return;
            }
            DungeonRegistry registry = world.getPersistentStateManager().get(DungeonRegistry::fromNbt, ID);
            if (registry == null) {
                return;
            }
            for (DungeonState state : registry.all()) {
                if (state.isCleared() && repopulatesIn(world, state) == 0L
                        && world.getTime() - state.lastOccupied() >= REPOPULATE_EMPTY_TICKS
                        && playersInside(world, state).isEmpty()) {
                    repopulate(world, state);
                }
            }
        });
    }

    /** A ward saw players in the dungeon at {@code time}. */
    public static void occupied(DungeonState state, long time) {
        state.setLastOccupied(time);
    }

    /** Records the player as having been inside; true the first time. */
    public static boolean firstVisit(DungeonState state, ServerPlayerEntity player) {
        return state.addVisitor(player.getUuid());
    }

    private static DungeonRegistry fromNbt(NbtCompound nbt) {
        DungeonRegistry registry = new DungeonRegistry();
        NbtList list = nbt.getList("Dungeons", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            DungeonState state = DungeonState.fromNbt(list.getCompound(i));
            state.owner = registry;
            registry.dungeons.put(state.startKey(), state);
        }
        return registry;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (DungeonState state : this.dungeons.values()) {
            list.add(state.toNbt());
        }
        nbt.put("Dungeons", list);
        return nbt;
    }
}
