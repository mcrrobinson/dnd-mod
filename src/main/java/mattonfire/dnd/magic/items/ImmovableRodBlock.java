package mattonfire.dnd.magic.items;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The Immovable Rod, fixed in place: a solid, unbreakable iron bar that pistons can't move. Right-clicking it
 * (anyone, with any hand) presses its button and it lets go; otherwise {@link ImmovableRodItem} schedules it
 * to return after 60 s through {@code ScheduledBlockRestore}. Drops nothing: the rod item never leaves
 * the player's inventory.
 */
public class ImmovableRodBlock extends Block {
    private static final VoxelShape SHAPE = Block.createCuboidShape(5, 0, 5, 11, 16, 11);

    public ImmovableRodBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.BLOCK;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (!world.isClient) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 1.0F, 1.4F);
            DnDClasses.LOGGER.info("[Magic] {} released the Immovable Rod at {}", player.getEntityName(),
                    pos.toShortString());
        }
        return ActionResult.success(world.isClient);
    }
}
