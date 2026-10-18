package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * The Goblin Junk Pile: a low heap of broken weapons, pots, bones and a rusty helmet. Its drops are in
 * its loot table: a small roll from {@code dndclasses:gameplay/goblin_junk} (iron nuggets, bones,
 * string, rarely an emerald), or the pile itself with silk touch.
 */
public class JunkPileBlock extends Block {
    /** The heap: a wide base and a smaller mound on top, 12 px high in all. */
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 7, 16),
            Block.createCuboidShape(2, 7, 2, 14, 12, 14));

    public JunkPileBlock(Settings settings) {
        super(settings);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }
}
