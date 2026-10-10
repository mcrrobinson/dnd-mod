package mattonfire.dnd.classes.Obstacles;

import mattonfire.dnd.classes.Obstacles.types.WinchType;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Places obstacles from structure code (and {@code /dndobstacle place}). Like chest loot in
 * {@code LairPiece}, a block is only written when {@code chunkBox} contains it, so a piece that
 * spans chunks writes each block exactly once. Pass a null box outside worldgen.
 */
public final class ObstaclePlacer {
    private ObstaclePlacer() {
    }

    /** Places one sealed obstacle block. Returns false if it was outside {@code chunkBox}. */
    public static boolean place(WorldAccess world, @Nullable BlockBox chunkBox, BlockPos pos, ObstacleType type,
            Tier tier, boolean critical, long groupSeed) {
        if (chunkBox != null && !chunkBox.contains(pos)) {
            return false;
        }
        world.setBlockState(pos, ObstacleTypes.blockFor(type).getDefaultState(), Block.NOTIFY_ALL);
        if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be) {
            be.configure(tier, critical, groupSeed);
        }
        return true;
    }

    /**
     * Fills a doorway {@code width} wide (from {@code bottomLeft} along {@code along}) and
     * {@code height} tall with one obstacle group. Returns the blocks placed.
     */
    public static int doorway(WorldAccess world, @Nullable BlockBox chunkBox, BlockPos bottomLeft, Direction along,
            int width, int height, ObstacleType type, Tier tier, boolean critical, long groupSeed) {
        return box(world, chunkBox, bottomLeft,
                bottomLeft.offset(along, width - 1).up(height - 1), type, tier, critical, groupSeed);
    }

    /** Fills the box between two corners with one obstacle group. Returns the blocks placed. */
    public static int box(WorldAccess world, @Nullable BlockBox chunkBox, BlockPos from, BlockPos to, ObstacleType type,
            Tier tier, boolean critical, long groupSeed) {
        int placed = 0;
        for (BlockPos pos : BlockPos.iterate(from, to)) {
            if (place(world, chunkBox, pos.toImmutable(), type, tier, critical, groupSeed)) {
                placed++;
            }
        }
        return placed;
    }

    /**
     * Lays a chasm's retracted bridge: the top layer of the gap, {@code length} blocks from
     * {@code nearCorner} along {@code along} and {@code width} blocks to its right. The pit under it is the
     * structure's to dig. Returns the blocks placed.
     */
    public static int chasm(WorldAccess world, @Nullable BlockBox chunkBox, BlockPos nearCorner, Direction along,
            int length, int width, Tier tier, boolean critical, long groupSeed) {
        BlockPos far = nearCorner.offset(along, length - 1).offset(along.rotateYClockwise(), width - 1);
        return box(world, chunkBox, nearCorner, far, ObstacleTypes.CHASM, tier, critical, groupSeed);
    }

    /**
     * Places a winch (a portcullis winch or a bridge winch) linked to the obstacle group at {@code target}.
     * The target must already be placed if it's in the same chunk; if it isn't (or isn't placed yet), the
     * winch finds the nearest one on first use. Returns false if it was outside {@code chunkBox}.
     */
    public static boolean winch(WorldAccess world, @Nullable BlockBox chunkBox, BlockPos pos,
            WinchType type, BlockPos target, Tier tier, long groupSeed) {
        if (!place(world, chunkBox, pos, type, tier, false, groupSeed)) {
            return false;
        }
        if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be) {
            be.data().put(WinchType.TARGET_KEY,
                    NbtHelper.fromBlockPos(target));
            be.markDirty();
        }
        return true;
    }
}
