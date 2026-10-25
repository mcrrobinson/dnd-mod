package mattonfire.dnd.classes.Blocks.underdark;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.block.Blocks;

/**
 * Ripplebark: a shelf fungus on a cave wall, attached like cocoa ({@link #FACING} points at the wall). It grows
 * through ages 0-2 on random ticks (1 in 5) or with bone meal. Right-clicking a grown shelf picks 1-2 Ripplebark (food)
 * and leaves it to regrow from age 0; breaking it drops 1, or 2-3 when grown.
 */
public class RipplebarkBlock extends HorizontalFacingBlock implements Fertilizable {
    public static final int MAX_AGE = 2;
    public static final IntProperty AGE = Properties.AGE_2;
    private static final int GROW_ODDS = 5;
    private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            VoxelShape[] byAge = new VoxelShape[MAX_AGE + 1];
            for (int age = 0; age <= MAX_AGE; age++) {
                double half = 3 + age;
                double depth = 3 + age;
                double low = 7 - age;
                double high = 9;
                byAge[age] = shape(facing, half, depth, low, high);
            }
            SHAPES.put(facing, byAge);
        }
    }

    public RipplebarkBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(AGE, 0));
    }

    /** A shelf {@code 2*half} wide sticking {@code depth} out of the wall on side {@code facing}. */
    private static VoxelShape shape(Direction facing, double half, double depth, double low, double high) {
        return switch (facing) {
            case NORTH -> Block.createCuboidShape(8 - half, low, 0, 8 + half, high, depth);
            case SOUTH -> Block.createCuboidShape(8 - half, low, 16 - depth, 8 + half, high, 16);
            case WEST -> Block.createCuboidShape(0, low, 8 - half, depth, high, 8 + half);
            default -> Block.createCuboidShape(16 - depth, low, 8 - half, 16, high, 8 + half);
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.get(state.get(FACING))[state.get(AGE)];
    }

    /** Whether a shelf facing {@code wall} can hang at {@code pos}: the wall block there has a solid face. */
    public static boolean canHang(WorldView world, BlockPos pos, Direction wall) {
        BlockPos support = pos.offset(wall);
        BlockState state = world.getBlockState(support);
        return !state.isOf(Blocks.MAGMA_BLOCK) && state.isSideSolidFullSquare(world, support, wall.getOpposite());
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return canHang(world, pos, state.get(FACING));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = getDefaultState();
        for (Direction direction : ctx.getPlacementDirections()) {
            if (direction.getAxis().isHorizontal()) {
                state = state.with(FACING, direction);
                if (state.canPlaceAt(ctx.getWorld(), ctx.getBlockPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.get(FACING) && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return state.get(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (random.nextInt(GROW_ODDS) == 0) {
            world.setBlockState(pos, state.with(AGE, state.get(AGE) + 1), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (state.get(AGE) < MAX_AGE) {
            return ActionResult.PASS;
        }
        if (!world.isClient) {
            int count = 1 + world.random.nextInt(2);
            dropStack(world, pos, new ItemStack(asItem(), count));
            world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0F,
                    0.7F + world.random.nextFloat() * 0.3F);
            world.setBlockState(pos, state.with(AGE, 0), Block.NOTIFY_LISTENERS);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        return state.get(AGE) < MAX_AGE;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(AGE, state.get(AGE) + 1), Block.NOTIFY_LISTENERS);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }
}
