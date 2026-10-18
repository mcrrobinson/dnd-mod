package mattonfire.dnd.classes.Blocks.adventuring;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * A plank tavern bench along {@link #AXIS}: a 3-px seat on two board legs. Benches end to end on the same axis join
 * into one long bench (of any wood), with legs only at its ends. Placed running across the way the player looks, so
 * it lines up beside the table in front of them.
 */
public class TavernBenchBlock extends TavernFurnitureBlock {
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

    public TavernBenchBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(AXIS, Direction.Axis.X).with(NORTH, false)
                .with(EAST, false).with(SOUTH, false).with(WEST, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(AXIS);
    }

    /** The bench running along {@code axis}, not yet joined to anything. */
    public BlockState along(Direction.Axis axis) {
        return this.getDefaultState().with(AXIS, axis);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction.Axis axis = ctx.getHorizontalPlayerFacing().rotateYClockwise().getAxis();
        return this.connect(this.along(axis), ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    protected boolean joins(BlockState state, Direction dir, BlockState neighbor) {
        Direction.Axis axis = state.get(AXIS);
        return dir.getAxis() == axis && neighbor.getBlock() instanceof TavernBenchBlock && neighbor.get(AXIS) == axis;
    }

    @Override
    protected VoxelShape buildShape(BlockState state) {
        boolean x = state.get(AXIS) == Direction.Axis.X;
        VoxelShape shape = x ? Block.createCuboidShape(0, 6, 3, 16, 9, 13) : Block.createCuboidShape(3, 6, 0, 13, 9, 16);
        if (x) {
            if (!state.get(WEST)) {
                shape = VoxelShapes.union(shape, Block.createCuboidShape(1, 0, 4, 3, 6, 12));
            }
            if (!state.get(EAST)) {
                shape = VoxelShapes.union(shape, Block.createCuboidShape(13, 0, 4, 15, 6, 12));
            }
        } else {
            if (!state.get(NORTH)) {
                shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 0, 1, 12, 6, 3));
            }
            if (!state.get(SOUTH)) {
                shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 0, 13, 12, 6, 15));
            }
        }
        return shape;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        BlockState out = super.rotate(state, rotation);
        if (rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90) {
            out = out.with(AXIS, state.get(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return out;
    }
}
