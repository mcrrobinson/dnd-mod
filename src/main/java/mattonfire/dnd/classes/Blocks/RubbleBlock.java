package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Collapsed Rubble: broken stone and dust in layers of 1 to 8, stacked like snow layers (use more on it to add a
 * layer). Unlike snow it never melts, is solid to its full height and drops one item per layer.
 */
public class RubbleBlock extends Block {
    public static final int MAX_LAYERS = 8;
    public static final IntProperty LAYERS = Properties.LAYERS;
    private static final VoxelShape[] SHAPES = new VoxelShape[MAX_LAYERS + 1];

    static {
        for (int i = 1; i <= MAX_LAYERS; i++) {
            SHAPES[i] = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, i * 2.0, 16.0);
        }
        SHAPES[0] = SHAPES[1];
    }

    public RubbleBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(LAYERS, 1));
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return type == NavigationType.LAND && state.get(LAYERS) <= 4;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        return SHAPES[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(LAYERS)];
    }

    @Override
    public boolean hasSidedTransparency(BlockState state) {
        return true;
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return state.get(LAYERS) == MAX_LAYERS ? 0.2F : 1.0F;
    }

    /** Rests on any full top face, or on a full stack of rubble. */
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        if (below.isOf(this)) {
            return below.get(LAYERS) == MAX_LAYERS;
        }
        return Block.isFaceFullSquare(below.getCollisionShape(world, pos.down()), Direction.UP);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState()
                : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        int layers = state.get(LAYERS);
        if (context.getStack().isOf(this.asItem()) && layers < MAX_LAYERS) {
            return !context.canReplaceExisting() || context.getSide() == Direction.UP;
        }
        // A single layer gives way to other blocks, as snow does
        return layers == 1;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = context.getWorld().getBlockState(context.getBlockPos());
        if (state.isOf(this)) {
            return state.with(LAYERS, Math.min(MAX_LAYERS, state.get(LAYERS) + 1));
        }
        return super.getPlacementState(context);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }
}
