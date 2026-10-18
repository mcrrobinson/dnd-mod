package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Cogsdeep's Clockface: a cream clock face in a brass rim whose hour hand shows the in-game time. {@link #HOUR}
 * is the hour on a twelve-hour dial (dawn, time 0, is six o'clock); each value has its own front texture. The block
 * keeps itself right with a block tick at every hour; a random tick starts that chain for clocks a structure placed
 * (they're set without ticks), and puts the hand right after {@code /time set}.
 */
public class ClockfaceBlock extends HorizontalFacingBlock {
    public static final IntProperty HOUR = IntProperty.of("hour", 0, 11);
    /** Ticks in an in-game hour. */
    static final int HOUR_TICKS = 1000;

    public ClockfaceBlock(Settings settings) {
        super(settings.ticksRandomly());
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(HOUR, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HOUR);
    }

    /** The hour the hand shows in {@code world} now, 0 (twelve o'clock) to 11. */
    public static int hourOf(World world) {
        long time = Math.floorMod(world.getTimeOfDay(), 24000L);
        return (int) ((time / HOUR_TICKS + 6) % 12);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(HOUR, hourOf(ctx.getWorld()));
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient && !oldState.isOf(this)) {
            this.scheduleNext(world, pos);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        this.update(state, world, pos);
        this.scheduleNext(world, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        this.update(state, world, pos);
        if (!world.getBlockTickScheduler().isQueued(pos, this)) {
            this.scheduleNext(world, pos);
        }
    }

    private void update(BlockState state, World world, BlockPos pos) {
        int hour = hourOf(world);
        if (state.get(HOUR) != hour) {
            world.setBlockState(pos, state.with(HOUR, hour), Block.NOTIFY_LISTENERS);
        }
    }

    /** A block tick just after the next hour strikes. */
    private void scheduleNext(World world, BlockPos pos) {
        long time = Math.floorMod(world.getTimeOfDay(), (long) HOUR_TICKS);
        world.scheduleBlockTick(pos, this, (int) (HOUR_TICKS - time) + 1);
    }
}
