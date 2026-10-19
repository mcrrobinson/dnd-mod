package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalConnectingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

/**
 * The Tent Stake: a short spruce stake driven into the ground, with a guy rope running up from it to each
 * neighbouring block with a solid side (a tent wall), like a fence connects. The rope has no collision, so
 * only the stake itself gets in the way. See docs/structures/goblin-camps.md.
 */
public class TentStakeBlock extends HorizontalConnectingBlock {
    /** The stake alone: 2 px square, 10 px high. */
    private static final VoxelShape STAKE = Block.createCuboidShape(7, 0, 7, 9, 10, 9);

    public TentStakeBlock(Settings settings) {
        // Outline: the stake plus a low strip along each rope, so it can be aimed at.
        super(1.0F, 1.0F, 10.0F, 10.0F, 10.0F, settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(NORTH, false).with(EAST, false)
                .with(SOUTH, false).with(WEST, false).with(WATERLOGGED, false));
    }

    /** Whether a rope runs from a stake to {@code neighbour}, which is on its {@code dir} side. */
    public static boolean ropesTo(BlockState neighbour, BlockView world, BlockPos neighbourPos, Direction dir) {
        return neighbour.isSideSolidFullSquare(world, neighbourPos, dir.getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return STAKE;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockView world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = this.getDefaultState()
                .with(WATERLOGGED, world.getFluidState(pos).getFluid() == Fluids.WATER);
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos at = pos.offset(dir);
            state = state.with(FACING_PROPERTIES.get(dir), ropesTo(world.getBlockState(at), world, at, dir));
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        if (direction.getAxis().isHorizontal()) {
            return state.with(FACING_PROPERTIES.get(direction), ropesTo(neighborState, world, neighborPos, direction));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
    }
}
