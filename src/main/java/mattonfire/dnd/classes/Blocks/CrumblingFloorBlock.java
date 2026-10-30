package mattonfire.dnd.classes.Blocks;

import mattonfire.dnd.classes.SkillChecks.TrapDisarm;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import java.util.Locale;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Floor over a pit trap, with hairline cracks if you look closely. Stepping on it sets the pit off; half a
 * second later every tile of the pit gives way ({@link TrapTriggerBlockEntity}). Unbreakable, and put back
 * when the dungeon resets. Kobold pits ({@link Covering#STRAW}) are lidded with straw and hide instead.
 */
public class CrumblingFloorBlock extends Block {
    /** What it's dressed as: dungeon stone, or a kobold pit's straw-and-hide lid. */
    public static final EnumProperty<Covering> COVERING = EnumProperty.of("covering", Covering.class);

    public enum Covering implements StringIdentifiable {
        STONE, STRAW;

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public static Covering byId(String id) {
            for (Covering covering : values()) {
                if (covering.asString().equals(id)) {
                    return covering;
                }
            }
            return STONE;
        }
    }

    public CrumblingFloorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(COVERING, Covering.STONE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(COVERING);
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
