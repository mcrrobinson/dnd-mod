package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Brimstone with a hole in its top that breathes yellow sulphur fumes (client-side particles only, no effect on
 * anyone). Decoration for the Brimstone Sanctum's platform edge and the Alchemist's den.
 */
public class SulphurVentBlock extends Block {
    public SulphurVentBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!world.getBlockState(pos.up()).isAir()) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.02D;
        double z = pos.getZ() + 0.5D;
        // A puff of yellow fumes (entity-effect particles take their colour as the velocity) and a wisp of smoke.
        for (int i = 0; i < 3; i++) {
            world.addParticle(ParticleTypes.ENTITY_EFFECT, x + random.nextGaussian() * 0.12D, y,
                    z + random.nextGaussian() * 0.12D, 0.92D, 0.82D, 0.18D);
        }
        if (random.nextInt(3) == 0) {
            world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.04D, 0.0D);
        }
        if (random.nextInt(40) == 0) {
            world.playSound(x, y, z, SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.BLOCKS, 0.2F,
                    0.6F + random.nextFloat() * 0.3F, false);
        }
    }
}
