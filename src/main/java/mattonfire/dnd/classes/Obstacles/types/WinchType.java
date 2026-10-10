package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleState;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

/**
 * A winch that works another obstacle group (its target): a portcullis, or a chasm's bridge. The
 * target's position is stored in the winch's block entity ({@code Data.Target}), set by
 * {@code ObstaclePlacer.winch} or {@code /dndobstacle link}. A winch with no target (one placed by
 * hand) links itself to the nearest block of its target type within {@link #linkRadius()} the
 * first time it's used. Winches are machines: solid even when "open" (wedged), and unbreakable.
 */
public abstract class WinchType extends ObstacleType {
    public static final String TARGET_KEY = "Target";

    private final ObstacleType target;

    protected WinchType(Identifier id, ObstacleType target) {
        super(id);
        this.target = target;
    }

    /** The obstacle type this winch works. */
    public final ObstacleType targetType() {
        return target;
    }

    /** How far a hand-placed winch looks for its target. */
    public abstract int linkRadius();

    @Override
    public boolean breakable() {
        return false;
    }

    @Override
    public boolean solid(ObstacleState state) {
        return true;
    }

    @Override
    public boolean ownWhoText() {
        return true;
    }

    private boolean isTarget(BlockView world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof ObstacleBlock block && block.type() == target;
    }

    /**
     * Links the winch at {@code winch} to the target group at {@code targetPos}. Returns false if either
     * isn't the right kind of obstacle.
     */
    public boolean link(BlockView world, BlockPos winch, BlockPos targetPos) {
        if (!isTarget(world, targetPos) || !(world.getBlockEntity(winch) instanceof ObstacleBlockEntity be)
                || !(world.getBlockState(winch).getBlock() instanceof ObstacleBlock block) || block.type() != this) {
            return false;
        }
        be.data().put(TARGET_KEY, NbtHelper.fromBlockPos(targetPos));
        be.markDirty();
        return true;
    }

    /** The target group's position: the stored one if it's still there, else the nearest one (then stored). */
    @Nullable
    public BlockPos target(BlockView world, BlockPos winch) {
        if (!(world.getBlockEntity(winch) instanceof ObstacleBlockEntity be)) {
            return null;
        }
        NbtCompound data = be.data();
        if (data.contains(TARGET_KEY)) {
            BlockPos stored = NbtHelper.toBlockPos(data.getCompound(TARGET_KEY));
            if (isTarget(world, stored)) {
                return stored;
            }
        }
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int r = linkRadius();
        for (BlockPos pos : BlockPos.iterate(winch.add(-r, -r, -r), winch.add(r, r, r))) {
            double distance = pos.getSquaredDistance(winch);
            if (distance <= (double) r * r && distance < bestDistance && isTarget(world, pos)) {
                best = pos.toImmutable();
                bestDistance = distance;
            }
        }
        if (best != null) {
            data.put(TARGET_KEY, NbtHelper.fromBlockPos(best));
            be.markDirty();
        }
        return best;
    }
}
