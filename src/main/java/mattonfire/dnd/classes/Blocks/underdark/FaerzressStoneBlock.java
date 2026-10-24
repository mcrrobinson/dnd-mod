package mattonfire.dnd.classes.Blocks.underdark;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Deepslate veined with glowing violet faerzress (light 5): now and then a mote of it drifts off an open face. */
public class FaerzressStoneBlock extends Block {
    public FaerzressStoneBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(8) != 0) {
            return;
        }
        Direction side = Direction.random(random);
        BlockPos next = pos.offset(side);
        if (world.getBlockState(next).isOpaqueFullCube(world, next)) {
            return;
        }
        double x = pos.getX() + 0.5 + side.getOffsetX() * 0.6 + (random.nextDouble() - 0.5) * 0.6;
        double y = pos.getY() + 0.5 + side.getOffsetY() * 0.6 + (random.nextDouble() - 0.5) * 0.6;
        double z = pos.getZ() + 0.5 + side.getOffsetZ() * 0.6 + (random.nextDouble() - 0.5) * 0.6;
        world.addParticle(ParticleTypes.PORTAL, x, y, z, 0.0, 0.0, 0.0);
    }
}
