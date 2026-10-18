package mattonfire.dnd.classes.Blocks.elven;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** A cluster of pale blue glowing mushrooms (light 7) that grows on dirt, grass and moss. */
public class GlimmercapBlock extends PlantBlock {
    private static final VoxelShape SHAPE = Block.createCuboidShape(3, 0, 3, 13, 8, 13);

    public GlimmercapBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Vec3d offset = state.getModelOffset(world, pos);
        return SHAPE.offset(offset.x, offset.y, offset.z);
    }
}
