package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.AbstractGlassBlock;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * The arcane ward that shuts a dungeon doorway while its room's fight is on: a translucent,
 * unbreakable, solid shimmer that lets light through. Room wards place and remove it (from
 * dungeons ticket 2 on); nothing uses it yet.
 */
public class ArcaneSealBlock extends AbstractGlassBlock {
    public ArcaneSealBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(3) == 0) {
            world.addParticle(ParticleTypes.ENCHANT, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), (random.nextDouble() - 0.5D) * 0.5D, random.nextDouble() * 0.3D,
                    (random.nextDouble() - 0.5D) * 0.5D);
        }
    }
}
