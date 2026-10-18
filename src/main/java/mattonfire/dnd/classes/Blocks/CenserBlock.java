package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.LanternBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * The Chain-Hung Censer: a small brass censer of red embers that hangs from a chain or stands like a lantern,
 * trailing smoke. Placement and support are the lantern's.
 */
public class CenserBlock extends LanternBlock {
    public CenserBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + (state.get(HANGING) ? 0.6D : 0.5D);
        double z = pos.getZ() + 0.5D;
        world.addParticle(ParticleTypes.SMOKE, x + random.nextGaussian() * 0.05D, y,
                z + random.nextGaussian() * 0.05D, 0.0D, 0.02D, 0.0D);
        if (random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5D) * 0.25D, y - 0.15D,
                    z + (random.nextDouble() - 0.5D) * 0.25D, 0.0D, 0.0D, 0.0D);
        }
    }
}
