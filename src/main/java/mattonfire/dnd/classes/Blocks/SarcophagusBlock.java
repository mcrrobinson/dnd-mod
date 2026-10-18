package mattonfire.dnd.classes.Blocks;

import mattonfire.dnd.classes.Obstacles.CryptSleepers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
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
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Sarcophagus: a stone coffin two blocks long, with a knight's effigy carved on its lid. It's placed like
 * a bed (the foot where you click, the head beyond it, {@link #FACING} pointing from the foot to the
 * head) and breaking either half breaks both, dropping one from the head.
 *
 * <p>An {@link #OCCUPIED} sarcophagus (only ever placed by the Crypt's dressing) holds an ambush: using
 * it slides the lid and a zombie climbs out after whoever opened it. A player the dead take for one of
 * their own ({@link CryptSleepers#undeadAllied}, a Necromancer and their escort) leaves it sleeping.
 * Either way, an opened sarcophagus is empty for good.
 */
public class SarcophagusBlock extends HorizontalFacingBlock {
    public static final EnumProperty<BedPart> PART = Properties.BED_PART;
    public static final BooleanProperty OCCUPIED = Properties.OCCUPIED;
    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);

    public SarcophagusBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH)
                .with(PART, BedPart.FOOT).with(OCCUPIED, false));
    }

    /** One half of a sarcophagus whose head points {@code toHead} from its foot. */
    public BlockState half(Direction toHead, BedPart part, boolean occupied) {
        return this.getDefaultState().with(FACING, toHead).with(PART, part).with(OCCUPIED, occupied);
    }

    private static Direction towardOtherPart(BedPart part, Direction facing) {
        return part == BedPart.FOOT ? facing : facing.getOpposite();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        if (!state.get(OCCUPIED)) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        ServerWorld server = (ServerWorld) world;
        if (player instanceof ServerPlayerEntity sp && !sp.isCreative() && CryptSleepers.undeadAllied(sp)) {
            sp.sendMessage(Text.translatable("block.dndclasses.sarcophagus.sleeps"), true);
            return ActionResult.CONSUME;
        }
        BlockPos head = state.get(PART) == BedPart.HEAD ? pos : pos.offset(state.get(FACING));
        this.empty(world, pos, state);
        world.playSound(null, head, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 1.0F, 0.6F);
        ZombieEntity zombie = EntityType.ZOMBIE.create(server);
        if (zombie != null) {
            zombie.refreshPositionAndAngles(head.getX() + 0.5D, head.getY() + 1.0D, head.getZ() + 0.5D,
                    state.get(FACING).getOpposite().asRotation(), 0.0F);
            zombie.initialize(server, server.getLocalDifficulty(head), SpawnReason.TRIGGERED, null, null);
            zombie.setPersistent();
            if (!player.isCreative() && !player.isSpectator()) {
                zombie.setTarget(player);
            }
            server.spawnEntityAndPassengers(zombie);
            zombie.playAmbientSound();
        }
        return ActionResult.CONSUME;
    }

    /** Marks both halves empty. */
    private void empty(World world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(OCCUPIED, false), Block.NOTIFY_ALL);
        BlockPos other = pos.offset(towardOtherPart(state.get(PART), state.get(FACING)));
        BlockState otherState = world.getBlockState(other);
        if (otherState.isOf(this)) {
            world.setBlockState(other, otherState.with(OCCUPIED, false), Block.NOTIFY_ALL);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == towardOtherPart(state.get(PART), state.get(FACING))) {
            return neighborState.isOf(this) && neighborState.get(PART) != state.get(PART)
                    ? state.with(OCCUPIED, neighborState.get(OCCUPIED)) : Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    /** In creative, breaking the foot takes the head with it without a drop (the drop is the head's). */
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && player.isCreative() && state.get(PART) == BedPart.FOOT) {
            BlockPos head = pos.offset(state.get(FACING));
            BlockState headState = world.getBlockState(head);
            if (headState.isOf(this) && headState.get(PART) == BedPart.HEAD) {
                world.setBlockState(head, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                world.syncWorldEvent(player, 2001, head, Block.getRawIdFromState(headState));
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing();
        BlockPos head = ctx.getBlockPos().offset(facing);
        World world = ctx.getWorld();
        if (world.getBlockState(head).canReplace(ctx) && world.getWorldBorder().contains(head)) {
            return this.half(facing, BedPart.FOOT, false);
        }
        return null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (!world.isClient) {
            BlockPos head = pos.offset(state.get(FACING));
            world.setBlockState(head, state.with(PART, BedPart.HEAD), Block.NOTIFY_ALL);
            world.updateNeighbors(pos, Blocks.AIR);
            state.updateNeighbors(world, pos, Block.NOTIFY_ALL);
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.DESTROY;
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }
}
