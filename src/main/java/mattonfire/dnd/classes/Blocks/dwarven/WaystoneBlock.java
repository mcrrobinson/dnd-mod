package mattonfire.dnd.classes.Blocks.dwarven;

import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.FactionIds;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * A standing stone two blocks tall with a column of gold runes down its front, set on the terrace before a
 * dwarven fortress's gate. Using it names the hold ("Here stands Ironfist Hold") and tells you your standing
 * with the Mountain Dwarves. The clan name is picked from {@link #CLANS} names by the lower block's position,
 * so a stone always gives the same one.
 */
public class WaystoneBlock extends HorizontalFacingBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    /** How many clan names there are ({@code block.dndclasses.waystone.clan.<n>}). */
    public static final int CLANS = 16;

    private static final VoxelShape LOWER_NS = Block.createCuboidShape(3, 0, 4, 13, 16, 12);
    private static final VoxelShape LOWER_EW = Block.createCuboidShape(4, 0, 3, 12, 16, 13);
    private static final VoxelShape UPPER_NS = Block.createCuboidShape(3, 0, 4, 13, 14, 12);
    private static final VoxelShape UPPER_EW = Block.createCuboidShape(4, 0, 3, 12, 14, 13);

    public WaystoneBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH)
                .with(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        boolean ns = state.get(FACING).getAxis() == Direction.Axis.Z;
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            return ns ? LOWER_NS : LOWER_EW;
        }
        return ns ? UPPER_NS : UPPER_EW;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        if (pos.getY() >= world.getTopY() - 1 || !world.getBlockState(pos.up()).canReplace(ctx)) {
            return null;
        }
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            return super.canPlaceAt(state, world, pos);
        }
        BlockState below = world.getBlockState(pos.down());
        return below.isOf(this) && below.get(HALF) == DoubleBlockHalf.LOWER;
    }

    /** Either half goes when the other does (the lower one drops the item, see the loot table). */
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.get(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            return neighborState.isOf(this) && neighborState.get(HALF) != half ? state : Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    /** In creative, breaking the top also takes the bottom without dropping it, as for doors. */
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && player.isCreative() && state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.down();
            BlockState lower = world.getBlockState(below);
            if (lower.isOf(this) && lower.get(HALF) == DoubleBlockHalf.LOWER) {
                world.setBlockState(below, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                world.syncWorldEvent(player, 2001, below, Block.getRawIdFromState(lower));
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        BlockPos lower = state.get(HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
        player.sendMessage(Text.translatable("block.dndclasses.waystone.hold", clanName(lower))
                .formatted(Formatting.GOLD), false);
        Faction dwarves = Factions.get(FactionIds.MOUNTAIN_DWARVES);
        if (dwarves != null) {
            ReputationTier tier = TierEffects.tierWith(player, dwarves);
            player.sendMessage(Text.translatable("block.dndclasses.waystone.standing", dwarves.displayName(),
                    tier.displayName()), false);
        }
        return ActionResult.CONSUME;
    }

    /** The clan named on the stone whose lower block is at {@code pos}. */
    public static Text clanName(BlockPos pos) {
        int index = (int) Math.floorMod(MathHelper.hashCode(pos.getX(), pos.getY(), pos.getZ()) >>> 16, (long) CLANS);
        return Text.translatable("block.dndclasses.waystone.clan." + index);
    }
}
