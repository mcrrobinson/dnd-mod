package mattonfire.dnd.classes.Blocks.underdark;

import mattonfire.dnd.classes.SkillChecks.Lockpicking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Chain Cage: a gate of black iron bars wrapped in chain, one block tall, the door of the drow and duergar captive
 * pens. Closed it's a barred panel across the block, facing the way you faced when you placed it; open it swings
 * against its hinge side ({@code facing} turned clockwise).
 * <ul>
 * <li>Unlocked, anyone opens and shuts it by hand.</li>
 * <li>{@code locked=true} (as the pens place it): only a lockpick success opens it, the same Thieves' Tools roll
 * as a locked chest or door ({@link Lockpicking#pickLock}, DC {@link #LOCK_DC}). A picked cage stays unlocked.</li>
 * <li>Creative: opens and shuts whatever its lock; sneak + use with an empty hand locks or unlocks it.</li>
 * </ul>
 */
public class ChainCageBlock extends HorizontalFacingBlock {
    public static final BooleanProperty OPEN = Properties.OPEN;
    public static final BooleanProperty LOCKED = BooleanProperty.of("locked");
    /** The lock's DC: a sturdy padlock, a little harder than a plain locked chest. */
    public static final int LOCK_DC = 12;

    private static final VoxelShape CLOSED_X = Block.createCuboidShape(0, 0, 7, 16, 16, 9);
    private static final VoxelShape CLOSED_Z = Block.createCuboidShape(7, 0, 0, 9, 16, 16);
    private static final VoxelShape OPEN_WEST = Block.createCuboidShape(0, 0, 0, 2, 16, 16);
    private static final VoxelShape OPEN_EAST = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
    private static final VoxelShape OPEN_NORTH = Block.createCuboidShape(0, 0, 0, 16, 16, 2);
    private static final VoxelShape OPEN_SOUTH = Block.createCuboidShape(0, 0, 14, 16, 16, 16);

    public ChainCageBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false)
                .with(LOCKED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, LOCKED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    /** The side an open cage swings against. */
    public static Direction hingeSide(BlockState state) {
        return state.get(FACING).rotateYClockwise();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!state.get(OPEN)) {
            return state.get(FACING).getAxis() == Direction.Axis.Z ? CLOSED_X : CLOSED_Z;
        }
        return switch (hingeSide(state)) {
            case WEST -> OPEN_WEST;
            case EAST -> OPEN_EAST;
            case NORTH -> OPEN_NORTH;
            default -> OPEN_SOUTH;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return state.get(OPEN);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (player.isCreative()) {
            if (player.isSneaking() && player.getStackInHand(hand).isEmpty()) {
                if (!world.isClient) {
                    boolean locked = !state.get(LOCKED);
                    world.setBlockState(pos, state.with(LOCKED, locked), Block.NOTIFY_ALL);
                    player.sendMessage(Text.translatable(locked ? "block.dndclasses.chain_cage.locked_now"
                            : "block.dndclasses.chain_cage.unlocked_now"), true);
                }
                return ActionResult.success(world.isClient);
            }
            toggle(state, world, pos, player);
            return ActionResult.success(world.isClient);
        }
        if (state.get(LOCKED) && !state.get(OPEN)) {
            if (world.isClient) {
                return ActionResult.SUCCESS;
            }
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.FAIL;
            }
            if (Lockpicking.pickLock(player, world, pos, LOCK_DC, "block.dndclasses.chain_cage.locked")) {
                world.setBlockState(pos, state.with(LOCKED, false).with(OPEN, true), Block.NOTIFY_ALL);
                playSound(world, pos, true);
            }
            return ActionResult.CONSUME;
        }
        toggle(state, world, pos, player);
        return ActionResult.success(world.isClient);
    }

    private static void toggle(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (world.isClient) {
            return;
        }
        boolean open = !state.get(OPEN);
        world.setBlockState(pos, state.with(OPEN, open), Block.NOTIFY_ALL);
        playSound(world, pos, open);
    }

    private static void playSound(World world, BlockPos pos, boolean open) {
        world.playSound(null, pos, open ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE,
                SoundCategory.BLOCKS, 1.0F, 0.8F);
        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 0.8F, 0.9F);
    }
}
