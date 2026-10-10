package mattonfire.dnd.tavern;

import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A tavern's bounty board: a framed board hung on a wall with three notices pinned to it. Use a
 * notice to take it (left, middle or right, by where you click), sneak-use with an empty hand to
 * read the whole board, or use it while holding a finished notice to claim the reward. Fresh
 * notices go up every morning.
 */
public class BountyBoardBlock extends BlockWithEntity {
    public static final int SLOTS = 3;
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty[] NOTICES = {
            BooleanProperty.of("notice_0"), BooleanProperty.of("notice_1"), BooleanProperty.of("notice_2")
    };

    // The board hangs on the face of the block behind it; FACING is the way its front looks.
    private static final VoxelShape NORTH = Block.createCuboidShape(0, 1, 13.5, 16, 15, 16);
    private static final VoxelShape SOUTH = Block.createCuboidShape(0, 1, 0, 16, 15, 2.5);
    private static final VoxelShape WEST = Block.createCuboidShape(13.5, 1, 0, 16, 15, 16);
    private static final VoxelShape EAST = Block.createCuboidShape(0, 1, 0, 2.5, 15, 16);

    public BountyBoardBlock(Settings settings) {
        super(settings);
        BlockState state = this.stateManager.getDefaultState().with(FACING, Direction.NORTH);
        for (BooleanProperty notice : NOTICES) {
            state = state.with(notice, true);
        }
        this.setDefaultState(state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(NOTICES);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        Direction facing = side.getAxis().isHorizontal() ? side : ctx.getHorizontalPlayerFacing().getOpposite();
        return this.getDefaultState().with(FACING, facing);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient && world.getBlockEntity(pos) instanceof BountyBoardBlockEntity board) {
            board.startBare(world);
        }
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
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BountyBoardBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof BountyBoardBlockEntity board)
                || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }
        board.refresh(world);
        state = world.getBlockState(pos);

        ItemStack held = player.getStackInHand(hand);
        // Hostile players can still read the board (sneak), but can't take or hand in notices.
        boolean takesOrClaims = BountyNoticeItem.bounty(held) != null || hand == Hand.MAIN_HAND && !player.isSneaking();
        if (takesOrClaims && shuns(serverPlayer, (ServerWorld) world, pos)) {
            player.sendMessage(Text.translatable("bounty.dndclasses.shunned").formatted(Formatting.RED), true);
            world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return ActionResult.CONSUME;
        }
        if (BountyNoticeItem.bounty(held) != null) {
            BountyRewards.claim(serverPlayer, held, Vec3d.ofCenter(pos));
            return ActionResult.CONSUME;
        }
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (player.isSneaking()) {
            this.read(serverPlayer, state, board);
            return ActionResult.CONSUME;
        }

        int slot = slotAt(state.get(FACING), hit.getPos().subtract(Vec3d.ofCenter(pos)));
        Bounty bounty = board.posted(slot);
        if (bounty == null || !state.get(NOTICES[slot])) {
            player.sendMessage(Text.translatable("bounty.dndclasses.taken_already").formatted(Formatting.GRAY), true);
            return ActionResult.CONSUME;
        }
        world.setBlockState(pos, state.with(NOTICES[slot], false));
        ItemStack notice = BountyNoticeItem.create(bounty);
        if (!player.giveItemStack(notice)) {
            player.dropItem(notice, false);
        }
        player.sendMessage(Text.translatable("bounty.dndclasses.take", bounty.title()).formatted(Formatting.GOLD), false);
        player.sendMessage(bounty.description().copy().formatted(Formatting.GRAY, Formatting.ITALIC), false);
        world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return ActionResult.CONSUME;
    }

    /**
     * Whether the board's people are Hostile to {@code player}: the factions that reward bounties and
     * whose settlement the board is in (any faction that rewards bounties, for a board out in the wild).
     */
    private static boolean shuns(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        java.util.List<Faction> local = new java.util.ArrayList<>();
        java.util.List<Faction> any = new java.util.ArrayList<>();
        for (Faction faction : Factions.all()) {
            if (faction.bountyMinor() == 0 && faction.bountyMajor() == 0) {
                continue;
            }
            any.add(faction);
            if (faction.isInSettlement(world, pos)) {
                local.add(faction);
            }
        }
        return (local.isEmpty() ? any : local).stream()
                .anyMatch(faction -> TierEffects.refusesService(TierEffects.tierWith(player, faction)));
    }

    /** Which notice (0 = left, as seen from the front) the hit point is over. */
    private static int slotAt(Direction facing, Vec3d offset) {
        Direction right = facing.rotateYCounterclockwise();
        double across = offset.x * right.getOffsetX() + offset.z * right.getOffsetZ() + 0.5D;
        return MathHelper.clamp((int) (across * SLOTS), 0, SLOTS - 1);
    }

    private void read(ServerPlayerEntity player, BlockState state, BountyBoardBlockEntity board) {
        player.sendMessage(Text.translatable("bounty.dndclasses.board").formatted(Formatting.GOLD), false);
        for (int i = 0; i < SLOTS; i++) {
            Bounty bounty = board.posted(i);
            Text where = Text.translatable("bounty.dndclasses.slot." + i);
            if (bounty == null || !state.get(NOTICES[i])) {
                player.sendMessage(Text.translatable("bounty.dndclasses.board.taken", where).formatted(Formatting.DARK_GRAY), false);
            } else {
                player.sendMessage(Text.translatable("bounty.dndclasses.board.entry", where, bounty.title(),
                        bounty.description(), BountyNoticeItem.rewardText(bounty)).formatted(Formatting.YELLOW), false);
            }
        }
    }
}
