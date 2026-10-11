package mattonfire.dnd.classes.Obstacles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * An obstacle is a group of touching blocks of the same type and {@code GroupSeed}, flood-filled
 * from the block that was clicked, so a template author just paints obstacle blocks. Opening,
 * resealing and retiering always act on the whole group.
 */
public final class ObstacleGroups {
    /** Most blocks one group can have. */
    public static final int MAX_BLOCKS = 48;

    private ObstacleGroups() {
    }

    /** The group containing {@code origin}, origin first (empty if it isn't an obstacle). */
    public static List<BlockPos> group(World world, BlockPos origin) {
        List<BlockPos> found = new ArrayList<>();
        BlockState originState = world.getBlockState(origin);
        if (!(originState.getBlock() instanceof ObstacleBlock)
                || !(world.getBlockEntity(origin) instanceof ObstacleBlockEntity originBe)) {
            return found;
        }
        Block block = originState.getBlock();
        long seed = originBe.groupSeed();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.toImmutable());
        seen.add(origin.toImmutable());
        while (!queue.isEmpty() && found.size() < MAX_BLOCKS) {
            BlockPos pos = queue.poll();
            found.add(pos);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.offset(direction);
                if (seen.add(next) && world.getBlockState(next).isOf(block)
                        && world.getBlockEntity(next) instanceof ObstacleBlockEntity be && be.groupSeed() == seed) {
                    queue.add(next);
                }
            }
        }
        return found;
    }

    /** Opens the group. Returns how many blocks changed. */
    public static int open(ServerWorld world, BlockPos origin) {
        return setState(world, origin, ObstacleState.OPEN);
    }

    /** Reseals the group. Returns how many blocks changed. */
    public static int seal(ServerWorld world, BlockPos origin) {
        return setState(world, origin, ObstacleState.SEALED);
    }

    /**
     * Puts the group back as new (a dungeon reset, {@code /dndobstacle reset}): reseals it and clears its
     * {@linkplain ObstacleType#progressKeys() progress}, even on blocks that were still sealed, such as a
     * part-doused Holy Ward. Returns how many blocks were resealed.
     */
    public static int reset(ServerWorld world, BlockPos origin) {
        List<BlockPos> group = group(world, origin);
        int changed = seal(world, origin);
        for (BlockPos pos : group) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof ObstacleBlock block
                    && world.getBlockEntity(pos) instanceof ObstacleBlockEntity be) {
                boolean cleared = false;
                for (String key : block.type().progressKeys()) {
                    if (be.data().contains(key)) {
                        be.data().remove(key);
                        cleared = true;
                    }
                }
                if (cleared) {
                    be.markDirty();
                }
            }
        }
        return changed;
    }

    private static int setState(ServerWorld world, BlockPos origin, ObstacleState target) {
        int changed = 0;
        long now = world.getTime();
        for (BlockPos pos : group(world, origin)) {
            BlockState state = world.getBlockState(pos);
            if (state.get(ObstacleBlock.STATE) == target) {
                continue;
            }
            world.setBlockState(pos, state.with(ObstacleBlock.STATE, target), Block.NOTIFY_ALL);
            if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be) {
                be.setOpenedAt(target == ObstacleState.OPEN ? now : 0L);
            }
            ObstacleType type = ((ObstacleBlock) state.getBlock()).type();
            if (target == ObstacleState.OPEN) {
                type.openEffects(world, pos, changed == 0);
            } else {
                type.sealEffects(world, pos, changed == 0);
            }
            changed++;
        }
        return changed;
    }

    /**
     * Opens the group for {@code ticks} (a raised portcullis): it reseals that long from now, even if
     * it was already open. Returns the group size.
     */
    public static int openFor(ServerWorld world, BlockPos origin, int ticks) {
        open(world, origin);
        List<BlockPos> group = group(world, origin);
        long now = world.getTime();
        for (BlockPos pos : group) {
            if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be) {
                be.setResealTicks(ticks);
                be.setOpenedAt(now);
            }
        }
        return group.size();
    }

    /** Whether the group at {@code origin} is open and will reseal on a timer. */
    public static boolean openOnTimer(World world, BlockPos origin) {
        return !ObstacleBlock.isSealed(world.getBlockState(origin))
                && world.getBlockEntity(origin) instanceof ObstacleBlockEntity be && be.resealTicks() > 0;
    }

    /** Sets the tier of every block in the group. Returns the group size. */
    public static int setTier(World world, BlockPos origin, Tier tier) {
        List<BlockPos> group = group(world, origin);
        for (BlockPos pos : group) {
            if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be) {
                be.setTier(tier);
            }
        }
        return group.size();
    }

    /** A per-type integer kept in the group's {@code Data} (read from {@code origin}; 0 if unset). */
    public static int dataInt(World world, BlockPos origin, String key) {
        return world.getBlockEntity(origin) instanceof ObstacleBlockEntity be ? be.data().getInt(key) : 0;
    }

    /**
     * Writes a per-type integer to the {@code Data} of every block in the group, so it reads the same
     * whichever block is clicked next (a Holy Ward's potion count, a Blood Seal's payments).
     */
    public static void setDataInt(World world, BlockPos origin, String key, int value) {
        for (BlockPos pos : group(world, origin)) {
            if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be) {
                be.data().putInt(key, value);
                be.markDirty();
            }
        }
    }

    /** Sets the reseal time of every block in the group. Returns the group size. */
    public static int setResealTicks(World world, BlockPos origin, int ticks) {
        List<BlockPos> group = group(world, origin);
        for (BlockPos pos : group) {
            if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be) {
                be.setResealTicks(ticks);
            }
        }
        return group.size();
    }
}
