package mattonfire.dnd.classes.Blocks;

import mattonfire.dnd.classes.SkillChecks.TrapDisarm;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Floor over a pit trap, with hairline cracks if you look closely. Stepping on it sets the pit off; half a
 * second later every tile of the pit gives way ({@link TrapTriggerBlockEntity}). Unbreakable, and put back
 * when the dungeon resets.
 */
public class CrumblingFloorBlock extends Block {
    public CrumblingFloorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return TrapDisarm.onUse(world, pos, player, hand);
    }

    @Override
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        TrapDisarm.onProjectileHit(world, hit.getBlockPos(), projectile);
    }
}
