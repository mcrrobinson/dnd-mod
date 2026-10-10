package mattonfire.dnd.classes.Obstacles;

import net.minecraft.block.Block;
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
}
