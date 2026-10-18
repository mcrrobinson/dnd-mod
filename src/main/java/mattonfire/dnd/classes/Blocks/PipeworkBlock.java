package mattonfire.dnd.classes.Blocks;

import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

/**
 * Cogsdeep's Pipework: a copper pipe through a brass joint that connects like a fence. It joins other pipework on
 * every side (so runs can climb), and on the four horizontal sides also the solid face of a block, so a run ends
 * in a wall.
 */
public class PipeworkBlock extends ConnectingBlock {
    public PipeworkBlock(Settings settings) {
        super(0.1875F, settings);
        BlockState state = this.stateManager.getDefaultState();
        for (BooleanProperty property : FACING_PROPERTIES.values()) {
            state = state.with(property, false);
        }
        this.setDefaultState(state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    /** Whether a pipe joins the block {@code neighbour} on its {@code side}. */
    public boolean connectsTo(BlockView world, BlockPos neighbourPos, BlockState neighbour, Direction side) {
        return neighbour.isOf(this) || side.getAxis().isHorizontal()
                && neighbour.isSideSolidFullSquare(world, neighbourPos, side.getOpposite());
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = this.getDefaultState();
        BlockPos pos = ctx.getBlockPos();
        for (Map.Entry<Direction, BooleanProperty> entry : FACING_PROPERTIES.entrySet()) {
            BlockPos at = pos.offset(entry.getKey());
            state = state.with(entry.getValue(), this.connectsTo(ctx.getWorld(), at, ctx.getWorld().getBlockState(at), entry.getKey()));
        }
        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return state.with(FACING_PROPERTIES.get(direction), this.connectsTo(world, neighborPos, neighborState, direction));
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        BlockState turned = state;
        for (Direction side : Direction.Type.HORIZONTAL) {
            turned = turned.with(FACING_PROPERTIES.get(rotation.rotate(side)), state.get(FACING_PROPERTIES.get(side)));
        }
        return turned;
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        BlockState turned = state;
        for (Direction side : Direction.Type.HORIZONTAL) {
            turned = turned.with(FACING_PROPERTIES.get(mirror.apply(side)), state.get(FACING_PROPERTIES.get(side)));
        }
        return turned;
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }
}
