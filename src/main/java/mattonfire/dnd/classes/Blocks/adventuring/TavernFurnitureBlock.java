package mattonfire.dnd.classes.Blocks.adventuring;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

/**
 * Tavern tables and benches: plank furniture that joins up with the same kind of furniture beside it, dropping the
 * legs on the joined side so a row reads as one long table or bench. Which sides join is kept in the
 * {@link #NORTH}/{@link #EAST}/{@link #SOUTH}/{@link #WEST} properties, worked out on placement and on neighbour
 * updates (structure pieces mark them for post-processing, as they do fences).
 */
public abstract class TavernFurnitureBlock extends Block {
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty WEST = Properties.WEST;

    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    protected TavernFurnitureBlock(Settings settings) {
        super(settings);
    }

    public static BooleanProperty side(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    /** Whether this furniture in {@code state} joins up with {@code neighbor} on its {@code dir} side. */
    protected abstract boolean joins(BlockState state, Direction dir, BlockState neighbor);

    /** The block's shape for {@code state}, built once per state. */
    protected abstract VoxelShape buildShape(BlockState state);

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    /** {@code state} with every side joined up to what's beside it in the world. */
    protected BlockState connect(BlockState state, BlockView world, BlockPos pos) {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            state = state.with(side(dir), this.joins(state, dir, world.getBlockState(pos.offset(dir))));
        }
        return state;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.connect(this.getDefaultState(), ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis().isHorizontal()) {
            return state.with(side(direction), this.joins(state, direction, neighbor));
        }
        return super.getStateForNeighborUpdate(state, direction, neighbor, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return this.shapes.computeIfAbsent(state, this::buildShape);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        BlockState out = state;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            out = out.with(side(rotation.rotate(dir)), state.get(side(dir)));
        }
        return out;
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        BlockState out = state;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            out = out.with(side(mirror.apply(dir)), state.get(side(dir)));
        }
        return out;
    }
}
