package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Grave Candle Cluster: a puddle of white wax with five burning stubs, always lit (light 6). It stands on
 * the top of a solid block and pops off when that goes, like a candle.
 */
public class GraveCandleClusterBlock extends Block {
    public static final int LIGHT = 6;
    private static final VoxelShape SHAPE = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 9.0D, 14.0D);
    /** The wick tops of the five stubs in the model (x, y, z in pixels; tools/crypt_blocks.py STUBS), where the flames burn. */
    private static final double[][] WICKS = {
            {4.0D, 8.3D, 5.0D}, {7.0D, 6.3D, 9.0D}, {10.0D, 9.3D, 4.0D}, {11.0D, 7.3D, 11.0D}, {5.0D, 5.3D, 11.0D}};

    public GraveCandleClusterBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return Block.sideCoversSmallSquare(world, pos.down(), Direction.UP);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        for (double[] wick : WICKS) {
            double x = pos.getX() + wick[0] / 16.0D;
            double y = pos.getY() + wick[1] / 16.0D;
            double z = pos.getZ() + wick[2] / 16.0D;
            if (random.nextFloat() < 0.3F) {
                world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
            }
            world.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
        }
        if (random.nextFloat() < 0.08F) {
            world.playSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.BLOCK_CANDLE_AMBIENT,
                    SoundCategory.BLOCKS, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
        }
    }
}
