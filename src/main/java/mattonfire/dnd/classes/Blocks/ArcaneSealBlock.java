package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.AbstractGlassBlock;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * The ward's seal ({@code dndclasses:arcane_seal}): a translucent violet wall a dungeon room's ward
 * puts in its doorways while a fight is on, and takes away again when the room is cleared or
 * abandoned. Solid to players and mobs, lets light through, can't be broken, pushed or blown up.
 *
 * Not to be confused with the class-gate obstacles {@code lesser_arcane_seal} and
 * {@code greater_arcane_seal}, which a Wizard can dispel: nobody can dispel a ward seal.
 */
public class ArcaneSealBlock extends AbstractGlassBlock {
    public ArcaneSealBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(3) == 0) {
            world.addParticle(ParticleTypes.ENCHANT, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0.0D, 0.05D, 0.0D);
        }
    }
}
