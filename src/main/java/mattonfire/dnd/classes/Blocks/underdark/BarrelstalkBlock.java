package mattonfire.dnd.classes.Blocks.underdark;

import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Barrelstalk: a bulbous fungus two blocks tall that stores water. While {@link #FILLED}, a glass bottle draws a
 * water bottle from it and a bucket a water bucket; either empties it, and it fills again {@link #REFILL_TICKS} later
 * (a scheduled tick on the lower half).
 */
public class BarrelstalkBlock extends TallPlantBlock {
    public static final BooleanProperty FILLED = BooleanProperty.of("filled");
    /** Five minutes. */
    public static final int REFILL_TICKS = 6000;
    private static final VoxelShape LOWER = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape UPPER = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);

    public BarrelstalkBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FILLED, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FILLED);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(UnderdarkBlocks.FUNGAL_LOAM) || floor.isOf(Blocks.MYCELIUM) || floor.isOf(Blocks.GRAVEL)
                || floor.isOf(Blocks.CLAY) || floor.isOf(Blocks.MOSS_BLOCK) || floor.isOf(Blocks.DEEPSLATE)
                || floor.isOf(Blocks.TUFF) || floor.isIn(BlockTags.DIRT);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        ItemStack held = player.getStackInHand(hand);
        boolean bottle = held.isOf(Items.GLASS_BOTTLE);
        boolean bucket = held.isOf(Items.BUCKET);
        if (!bottle && !bucket) {
            return ActionResult.PASS;
        }
        if (!state.get(FILLED)) {
            return ActionResult.PASS;
        }
        if (!world.isClient) {
            ItemStack water = bottle ? PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER)
                    : new ItemStack(Items.WATER_BUCKET);
            player.setStackInHand(hand, ItemUsage.exchangeStack(held, player, water));
            world.playSound(null, pos, bottle ? SoundEvents.ITEM_BOTTLE_FILL : SoundEvents.ITEM_BUCKET_FILL,
                    SoundCategory.BLOCKS, 1.0F, 0.8F);
            world.emitGameEvent(player, GameEvent.FLUID_PICKUP, pos);
            BlockPos lower = state.get(HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
            setFilled(world, lower, false);
            world.scheduleBlockTick(lower, this, REFILL_TICKS);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(HALF) == DoubleBlockHalf.LOWER && !state.get(FILLED)) {
            setFilled(world, pos, true);
        }
    }

    /** Sets both halves (lower half at {@code lower}) to {@code filled}. */
    private void setFilled(World world, BlockPos lower, boolean filled) {
        BlockState bottom = world.getBlockState(lower);
        BlockState top = world.getBlockState(lower.up());
        if (bottom.isOf(this)) {
            world.setBlockState(lower, bottom.with(FILLED, filled), Block.NOTIFY_LISTENERS);
        }
        if (top.isOf(this)) {
            world.setBlockState(lower.up(), top.with(FILLED, filled), Block.NOTIFY_LISTENERS);
        }
    }
}
