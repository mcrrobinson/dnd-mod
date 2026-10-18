package mattonfire.dnd.classes.Blocks.hobbit;

import mattonfire.dnd.tavern.Tavern;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * The Green Dragon's ale: a barrel on its side with a tap in its painted head. Right-click it with a glass bottle
 * to draw a Mug of Ale. It holds {@link #MAX_MUGS} mugs and slowly fills again: each random tick (about once a
 * minute) has a 1 in {@link #REFILL_ODDS} chance to add a mug, so a dry cask is full again in a few in-game hours.
 */
public class AleCaskBlock extends HorizontalFacingBlock {
    public static final int MAX_MUGS = 4;
    public static final int REFILL_ODDS = 4;
    public static final IntProperty MUGS = IntProperty.of("mugs", 0, MAX_MUGS);

    public AleCaskBlock(Settings settings) {
        super(settings.ticksRandomly());
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH)
                .with(MUGS, MAX_MUGS));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, MUGS);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        ItemStack held = player.getStackInHand(hand);
        int mugs = state.get(MUGS);
        if (!held.isOf(Items.GLASS_BOTTLE)) {
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            if (!world.isClient) {
                player.sendMessage(Text.translatable(mugs > 0 ? "block.dndclasses.ale_cask.mugs"
                        : "block.dndclasses.ale_cask.dry", mugs), true);
            }
            return ActionResult.success(world.isClient);
        }
        if (mugs == 0) {
            if (!world.isClient) {
                player.sendMessage(Text.translatable("block.dndclasses.ale_cask.dry"), true);
            }
            return ActionResult.success(world.isClient);
        }
        if (!world.isClient) {
            player.setStackInHand(hand, ItemUsage.exchangeStack(held, player, new ItemStack(Tavern.ALE)));
            world.setBlockState(pos, state.with(MUGS, mugs - 1), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 0.8F);
            world.emitGameEvent(player, GameEvent.FLUID_PICKUP, pos);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int mugs = state.get(MUGS);
        if (mugs < MAX_MUGS && random.nextInt(REFILL_ODDS) == 0) {
            world.setBlockState(pos, state.with(MUGS, mugs + 1), Block.NOTIFY_ALL);
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(MUGS) * 15 / MAX_MUGS;
    }
}
