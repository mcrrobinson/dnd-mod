package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

/**
 * Eye-Stalk Growth: fleshy stalks with small closed eyes, a sign a beholder is near. A plant-like cross model
 * that grows on any face of a solid block (floor, wall or ceiling), placed and broken like an amethyst cluster.
 */
public class EyeStalkBlock extends AmethystClusterBlock {
    public EyeStalkBlock(Settings settings) {
        super(9, 3, settings);
    }

    /** No amethyst chime when an arrow hits it. */
    @Override
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
    }
}
