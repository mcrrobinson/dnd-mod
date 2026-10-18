package mattonfire.dnd.classes.Blocks.adventuring;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * A heavy plank tavern table: a 3-px top on four square legs. Tables side by side join into one long table (of any
 * wood); a leg only stands at a corner where neither side beside it is joined.
 */
public class TavernTableBlock extends TavernFurnitureBlock {
    private static final VoxelShape TOP = Block.createCuboidShape(0, 13, 0, 16, 16, 16);

    public TavernTableBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false)
                .with(WEST, false));
    }

    @Override
    protected boolean joins(BlockState state, Direction dir, BlockState neighbor) {
        return neighbor.getBlock() instanceof TavernTableBlock;
    }

    @Override
    protected VoxelShape buildShape(BlockState state) {
        VoxelShape shape = TOP;
        boolean n = state.get(NORTH);
        boolean s = state.get(SOUTH);
        boolean e = state.get(EAST);
        boolean w = state.get(WEST);
        if (!n && !w) {
            shape = VoxelShapes.union(shape, Block.createCuboidShape(1, 0, 1, 4, 13, 4));
        }
        if (!n && !e) {
            shape = VoxelShapes.union(shape, Block.createCuboidShape(12, 0, 1, 15, 13, 4));
        }
        if (!s && !w) {
            shape = VoxelShapes.union(shape, Block.createCuboidShape(1, 0, 12, 4, 13, 15));
        }
        if (!s && !e) {
            shape = VoxelShapes.union(shape, Block.createCuboidShape(12, 0, 12, 15, 13, 15));
        }
        return shape;
    }
}
