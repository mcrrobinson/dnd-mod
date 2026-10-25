package mattonfire.dnd.classes.Blocks.underdark;

import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * A small Underdark mushroom (Bluecap, Trillimac). Unlike a vanilla mushroom it ignores light and never spreads: it
 * stands on Fungal Loam, mycelium, moss, dirt and the cave stones (deepslate, tuff, stone) wherever it's placed.
 */
public class UnderdarkFungusBlock extends PlantBlock {
    private static final VoxelShape SHAPE = Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 10.0, 12.0);

    public UnderdarkFungusBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(UnderdarkBlocks.FUNGAL_LOAM) || floor.isOf(Blocks.MYCELIUM) || floor.isOf(Blocks.MOSS_BLOCK)
                || floor.isOf(Blocks.DEEPSLATE) || floor.isOf(Blocks.TUFF) || floor.isOf(Blocks.STONE)
                || floor.isOf(UnderdarkBlocks.FAERZRESS_STONE) || floor.isIn(BlockTags.DIRT);
    }
}
