package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.WallTorchBlock;
import net.minecraft.particle.ParticleTypes;

/**
 * A black iron wall bracket holding a soul flame: a wall-only soul torch with its own model. Placement, shape,
 * support and flame particles are the wall torch's.
 */
public class SoulfireSconceBlock extends WallTorchBlock {
    public SoulfireSconceBlock(Settings settings) {
        super(settings, ParticleTypes.SOUL_FIRE_FLAME);
    }

    @Override
    public String getTranslationKey() {
        // WallTorchBlock borrows the standing torch's name; this one has its own.
        return net.minecraft.util.Util.createTranslationKey("block",
                net.minecraft.registry.Registries.BLOCK.getId(this));
    }
}
