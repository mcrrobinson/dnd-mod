package mattonfire.dnd.classes.Blocks;

import mattonfire.dnd.classes.SkillChecks.TrapDisarm;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A cracked floor tile with a grate, next to a flame trap's trigger. Stepping on it sets the trap off as
 * the trigger does; while the trap burns it's {@code lit} and glows. Unbreakable.
 */
public class FlameVentBlock extends Block {
    public FlameVentBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(Properties.LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.LIT);
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
