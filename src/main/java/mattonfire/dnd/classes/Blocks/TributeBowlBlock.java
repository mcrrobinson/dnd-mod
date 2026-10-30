package mattonfire.dnd.classes.Blocks;

import mattonfire.dnd.entity.kobold.KoboldTribute;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A Dragon Tribute Bowl: a gold-rimmed basalt bowl where kobolds leave offerings for their wyrm. Using gold on it
 * (an ingot, a block, a Kobold Trinket) is tribute to the Kobold Tribes ({@link KoboldTribute}).
 */
public class TributeBowlBlock extends Block {
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 2.0D, 13.0D),
            Block.createCuboidShape(1.0D, 2.0D, 1.0D, 15.0D, 7.0D, 15.0D));

    public TributeBowlBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (!KoboldTribute.isTribute(player.getStackInHand(hand))) {
            return ActionResult.PASS;
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            KoboldTribute.offer(serverPlayer, player.getStackInHand(hand), pos);
        }
        return ActionResult.success(world.isClient);
    }
}
