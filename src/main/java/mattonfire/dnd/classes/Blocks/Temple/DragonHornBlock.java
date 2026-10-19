package mattonfire.dnd.classes.Blocks.Temple;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RodBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Dragon Horn: a curved ivory horn, the dragon statues' horns, claws and spines. It points away from the face
 * it's placed against, like an end rod.
 */
public class DragonHornBlock extends RodBlock {
    private static final VoxelShape Y = Block.createCuboidShape(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape Z = Block.createCuboidShape(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 16.0D);
    private static final VoxelShape X = Block.createCuboidShape(0.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);

    public DragonHornBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.UP));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getSide());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING).getAxis()) {
            case X -> X;
            case Z -> Z;
            default -> Y;
        };
    }
}
