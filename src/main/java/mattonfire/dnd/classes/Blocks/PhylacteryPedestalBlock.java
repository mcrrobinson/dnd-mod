package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Phylactery Pedestal: a black stone pedestal with silver bands and a glowing green rune on its top
 * (light 5). The Crypt's reliquary has one under the phylactery, and a Lich raised anywhere else sets
 * one under its phylactery (see {@code LichEntity#createPhylactery}). Its top is a full block high, so
 * whatever stands on it stands at the next block.
 */
public class PhylacteryPedestalBlock extends Block {
    public static final int LIGHT = 5;
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 3.0D, 15.0D),
            Block.createCuboidShape(3.0D, 3.0D, 3.0D, 13.0D, 13.0D, 13.0D),
            Block.createCuboidShape(2.0D, 13.0D, 2.0D, 14.0D, 16.0D, 14.0D));

    public PhylacteryPedestalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
