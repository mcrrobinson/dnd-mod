package mattonfire.dnd.classes.Blocks.underdark;

import mattonfire.dnd.world.underdark.UnderdarkRifts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Rift Anchor: a crying-obsidian-like column (three blocks, the top one crowned with a violet crystal, light 10)
 * that stands at every rift landing and carved Underdark pocket (design 9, 2.6). Right-clicking it in the Underdark
 * starts a 3 s pull back up to the Overworld ({@link UnderdarkRifts#useAnchor}). It can't be broken or moved.
 */
public class RiftAnchorBlock extends Block {
    /** The crystal-topped block at the head of the column. */
    public static final BooleanProperty TOP = BooleanProperty.of("top");

    public RiftAnchorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(TOP, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(TOP);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {
            UnderdarkRifts.useAnchor(serverPlayer, pos);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.BLOCK;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(TOP) || random.nextInt(3) != 0) {
            return;
        }
        world.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                pos.getY() + 1.0, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.05, 0.0);
    }
}
