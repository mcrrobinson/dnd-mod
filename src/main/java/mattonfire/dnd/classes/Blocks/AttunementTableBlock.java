package mattonfire.dnd.classes.Blocks;

import mattonfire.dnd.classes.Progression.Progression;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * The only place a player can change which skills they have equipped. Using
 * it opens the skill tree in attunement mode.
 */
public class AttunementTableBlock extends Block {
    public AttunementTableBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Progression.openAttunement(serverPlayer, pos);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Enchanting-table glyphs drifting up off the top.
        for (int i = 0; i < 2; i++) {
            world.addParticle(ParticleTypes.ENCHANT, pos.getX() + random.nextDouble(), pos.getY() + 1.6,
                    pos.getZ() + random.nextDouble(), random.nextDouble() - 0.5, -0.6, random.nextDouble() - 0.5);
        }
        if (random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }
}
