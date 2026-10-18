package mattonfire.dnd.classes.Blocks.elven;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * A rope bridge's deck: birch slats lashed with rope, 3 pixels thick, in the top or bottom half of its block
 * like a slab (so a bridge can sag in half-block steps). {@code axis} is the way you walk along it; the slats
 * lie across it and the ropes run along both edges. It needs nothing under it.
 */
public class WovenRopePlankBlock extends Block {
    public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;
    private static final VoxelShape BOTTOM = Block.createCuboidShape(0, 5, 0, 16, 8, 16);
    private static final VoxelShape TOP = Block.createCuboidShape(0, 13, 0, 16, 16, 16);

    public WovenRopePlankBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(HALF, BlockHalf.BOTTOM).with(AXIS, Direction.Axis.Z));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HALF, AXIS);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(HALF) == BlockHalf.TOP ? TOP : BOTTOM;
    }

    /** Like a slab: the top half when placed against a block's underside or the upper half of its side. */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        boolean top = side == Direction.DOWN
                || side != Direction.UP && ctx.getHitPos().y - ctx.getBlockPos().getY() > 0.5D;
        return this.getDefaultState().with(HALF, top ? BlockHalf.TOP : BlockHalf.BOTTOM)
                .with(AXIS, ctx.getHorizontalPlayerFacing().getAxis());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        if (rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90) {
            return state.with(AXIS, state.get(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return state;
    }
}
