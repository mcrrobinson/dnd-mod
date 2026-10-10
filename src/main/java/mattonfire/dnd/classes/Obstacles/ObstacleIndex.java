package mattonfire.dnd.classes.Obstacles;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

/**
 * Every obstacle block in a dimension, open or sealed, by chunk. Filled as obstacle block entities
 * load (so worldgen placement needs no bookkeeping) and emptied when the block is removed. Used by
 * {@code /dndobstacle list}, and later by passive Perception and dungeon resets.
 */
public final class ObstacleIndex extends PersistentState {
    private static final String ID = "dndclasses_obstacles";

    private final Long2ObjectOpenHashMap<LongSet> byChunk = new Long2ObjectOpenHashMap<>();

    public static ObstacleIndex get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(ObstacleIndex::fromNbt, ObstacleIndex::new, ID);
    }

    /** Adds pos from any thread (block entities can load off the server thread). */
    static void addLater(ServerWorld world, BlockPos pos) {
        MinecraftServer server = world.getServer();
        BlockPos immutable = pos.toImmutable();
        if (server.isOnThread()) {
            get(world).add(immutable);
        } else {
            server.execute(() -> get(world).add(immutable));
        }
    }

    public void add(BlockPos pos) {
        if (byChunk.computeIfAbsent(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4), k -> new LongOpenHashSet())
                .add(pos.asLong())) {
            markDirty();
        }
    }

    public void remove(BlockPos pos) {
        long chunk = ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
        LongSet set = byChunk.get(chunk);
        if (set != null && set.remove(pos.asLong())) {
            if (set.isEmpty()) {
                byChunk.remove(chunk);
            }
            markDirty();
        }
    }

    /** Obstacle blocks within {@code radius} blocks (horizontally, by chunk, then exact distance). */
    public List<BlockPos> within(BlockPos center, int radius) {
        List<BlockPos> found = new ArrayList<>();
        int minX = (center.getX() - radius) >> 4;
        int maxX = (center.getX() + radius) >> 4;
        int minZ = (center.getZ() - radius) >> 4;
        int maxZ = (center.getZ() + radius) >> 4;
        double max = (double) radius * radius;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                LongSet set = byChunk.get(ChunkPos.toLong(cx, cz));
                if (set == null) {
                    continue;
                }
                for (long packed : set) {
                    BlockPos pos = BlockPos.fromLong(packed);
                    if (pos.getSquaredDistance(center) <= max) {
                        found.add(pos);
                    }
                }
            }
        }
        return found;
    }

    public int size() {
        int total = 0;
        for (LongSet set : byChunk.values()) {
            total += set.size();
        }
        return total;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        List<Long> all = new ArrayList<>();
        for (LongSet set : byChunk.values()) {
            all.addAll(set);
        }
        nbt.putLongArray("Positions", all);
        return nbt;
    }

    private static ObstacleIndex fromNbt(NbtCompound nbt) {
        ObstacleIndex index = new ObstacleIndex();
        for (long packed : nbt.getLongArray("Positions")) {
            index.add(BlockPos.fromLong(packed));
        }
        index.setDirty(false);
        return index;
    }
}
