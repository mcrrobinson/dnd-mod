package mattonfire.dnd.classes.Blocks.dwarven;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

/**
 * A slender chamfered column, thinner than a block. Stacked columns join up: the bottom one gets a stepped base,
 * the top one a capital, the ones between are plain shaft, and a column on its own has both. The part follows the
 * blocks above and below, so a stack sorts itself out as it's built or broken.
 */
public class RunicColumnBlock extends Block {
    public enum Part implements StringIdentifiable {
        SINGLE("single"), BASE("base"), SHAFT("shaft"), CAPITAL("capital");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }

        /** The part for a column with (or without) another column above and below it. */
        public static Part of(boolean above, boolean below) {
            return above ? (below ? SHAFT : BASE) : (below ? CAPITAL : SINGLE);
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.of("part", Part.class);

    /** The shaft: ten wide with its corners chamfered off. */
    private static final VoxelShape SHAFT = VoxelShapes.union(Block.createCuboidShape(3, 0, 4, 13, 16, 12),
            Block.createCuboidShape(4, 0, 3, 12, 16, 13));
    private static final VoxelShape FOOT = VoxelShapes.union(Block.createCuboidShape(1, 0, 1, 15, 3, 15),
            Block.createCuboidShape(2, 3, 2, 14, 5, 14));
    private static final VoxelShape HEAD = VoxelShapes.union(Block.createCuboidShape(2, 11, 2, 14, 13, 14),
            Block.createCuboidShape(1, 13, 1, 15, 16, 15));
    private static final VoxelShape BASE_SHAPE = VoxelShapes.union(SHAFT, FOOT);
    private static final VoxelShape CAPITAL_SHAPE = VoxelShapes.union(SHAFT, HEAD);
    private static final VoxelShape SINGLE_SHAPE = VoxelShapes.union(SHAFT, FOOT, HEAD);

    public RunicColumnBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(PART, Part.SINGLE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(PART)) {
            case BASE -> BASE_SHAPE;
            case SHAFT -> SHAFT;
            case CAPITAL -> CAPITAL_SHAPE;
            case SINGLE -> SINGLE_SHAPE;
        };
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.withPart(ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() == Direction.Axis.Y) {
            return this.withPart(world, pos);
        }
        return state;
    }

    private BlockState withPart(BlockView world, BlockPos pos) {
        return this.getDefaultState().with(PART, Part.of(world.getBlockState(pos.up()).isOf(this),
                world.getBlockState(pos.down()).isOf(this)));
    }
}
