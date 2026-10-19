package mattonfire.dnd.classes.Blocks.elven;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * A {@link WovenRopePlankBlock} hanging on edge: what's left of a rope bridge that snapped and swung down from its
 * platform. A 3-pixel panel of slats against one side of its block with the ropes running up and down; {@code facing}
 * is the way its face looks (away from the side it hangs against). It can be climbed like a ladder, needs nothing to
 * hold it up and drops a Woven Rope Plank. Only desolate enclaves place it; it has no item of its own.
 */
public class HangingRopePlankBlock extends HorizontalFacingBlock {
    private static final VoxelShape NORTH_SIDE = Block.createCuboidShape(0, 0, 0, 16, 16, 3);
    private static final VoxelShape SOUTH_SIDE = Block.createCuboidShape(0, 0, 13, 16, 16, 16);
    private static final VoxelShape WEST_SIDE = Block.createCuboidShape(0, 0, 0, 3, 16, 16);
    private static final VoxelShape EAST_SIDE = Block.createCuboidShape(13, 0, 0, 16, 16, 16);

    public HangingRopePlankBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(FACING, Direction.SOUTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> SOUTH_SIDE;
            case WEST -> EAST_SIDE;
            case EAST -> WEST_SIDE;
            default -> NORTH_SIDE;
        };
    }
}
