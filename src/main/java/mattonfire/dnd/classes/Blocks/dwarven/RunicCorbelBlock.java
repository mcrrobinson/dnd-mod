package mattonfire.dnd.classes.Blocks.dwarven;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * A stepped stone bracket that juts out of a wall: a full-width plate on top, narrowing in two steps down to the
 * wall. {@code facing} is the way it juts out, so the wall is behind it. Placed against the side of a block it juts
 * out of that block; placed on top or underneath, towards whoever placed it.
 */
public class RunicCorbelBlock extends HorizontalFacingBlock {
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            // Written for facing north (wall to the south, z = 16), then turned.
            SHAPES.put(facing, VoxelShapes.union(box(facing, 0, 11, 0, 16, 16, 16), box(facing, 2, 6, 4, 14, 11, 16),
                    box(facing, 5, 0, 7, 11, 6, 16)));
        }
    }

    public RunicCorbelBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    /** A box given as if facing north, turned to {@code facing} about the block's centre. */
    static VoxelShape box(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
        return switch (facing) {
            case SOUTH -> Block.createCuboidShape(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case EAST -> Block.createCuboidShape(16 - z2, y1, x1, 16 - z1, y2, x2);
            case WEST -> Block.createCuboidShape(z1, y1, 16 - x2, z2, y2, 16 - x1);
            default -> Block.createCuboidShape(x1, y1, z1, x2, y2, z2);
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        Direction facing = side.getAxis().isHorizontal() ? side : ctx.getHorizontalPlayerFacing().getOpposite();
        return this.getDefaultState().with(FACING, facing);
    }
}
