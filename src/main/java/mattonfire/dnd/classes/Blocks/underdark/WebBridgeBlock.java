package mattonfire.dnd.classes.Blocks.underdark;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Web Bridge Planks: a woven silk walkway, the top half of a block like a top slab, so a bridge between two drow
 * towers is flush with their floors. Light and quick to cut (scaffolding's hardness).
 */
public class WebBridgeBlock extends Block {
    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 8, 0, 16, 16, 16);

    public WebBridgeBlock(Settings settings) {
        super(settings);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasSidedTransparency(BlockState state) {
        return true;
    }
}
