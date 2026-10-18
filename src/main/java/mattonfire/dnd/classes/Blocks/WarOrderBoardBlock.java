package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The War Order Board: a crude plank board hung on a wall, with the Warlord's orders pinned to it by
 * daggers. A goblin copy of the hobbits' bounty board. Right-click reads today's orders (lang
 * {@code block.dndclasses.war_order_board.order.<n>}), picked by the board's position and the day, so each
 * board has its own and they change every morning. Hangs in goblin camp chiefs' tents and Goblin Warren
 * antechambers.
 */
public class WarOrderBoardBlock extends HorizontalFacingBlock {
    /** How many orders there are to read (lang keys {@code .order.0} to {@code .order.<ORDERS - 1>}). */
    public static final int ORDERS = 8;

    // Hung on the face of the block behind it; FACING is the way its front looks.
    private static final VoxelShape NORTH = Block.createCuboidShape(1, 1, 14, 15, 15, 16);
    private static final VoxelShape SOUTH = Block.createCuboidShape(1, 1, 0, 15, 15, 2);
    private static final VoxelShape WEST = Block.createCuboidShape(14, 1, 1, 16, 15, 15);
    private static final VoxelShape EAST = Block.createCuboidShape(0, 1, 1, 2, 15, 15);

    public WarOrderBoardBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        Direction facing = side.getAxis().isHorizontal() ? side : ctx.getHorizontalPlayerFacing().getOpposite();
        return this.getDefaultState().with(FACING, facing);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    /** Which order a board at {@code pos} shows on day {@code day}. */
    public static int order(BlockPos pos, long day) {
        return (int) Math.floorMod(MathHelper.hashCode(pos.getX(), pos.getY(), pos.getZ()) + day, (long) ORDERS);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        int order = order(pos, world.getTimeOfDay() / 24000L);
        player.sendMessage(Text.translatable("block.dndclasses.war_order_board.read").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.translatable("block.dndclasses.war_order_board.order." + order)
                .formatted(Formatting.ITALIC, Formatting.GRAY), false);
        world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1.0F, 0.8F);
        return ActionResult.CONSUME;
    }
}
