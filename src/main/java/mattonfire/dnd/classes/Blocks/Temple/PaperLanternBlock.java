package mattonfire.dnd.classes.Blocks.Temple;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Paper Lantern: a cream paper lantern with red caps and a warm glow. Like a vanilla lantern it stands on a
 * block or hangs from one ({@link LanternBlock#HANGING}), and can be waterlogged.
 */
public class PaperLanternBlock extends LanternBlock {
    private static final VoxelShape STANDING = VoxelShapes.union(Block.createCuboidShape(4, 1, 4, 12, 10, 12),
            Block.createCuboidShape(5, 0, 5, 11, 11, 11));
    private static final VoxelShape HANGING_SHAPE = VoxelShapes.union(Block.createCuboidShape(4, 4, 4, 12, 13, 12),
            Block.createCuboidShape(5, 3, 5, 11, 14, 11), Block.createCuboidShape(7, 14, 7, 9, 16, 9));

    public PaperLanternBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(HANGING) ? HANGING_SHAPE : STANDING;
    }
}
