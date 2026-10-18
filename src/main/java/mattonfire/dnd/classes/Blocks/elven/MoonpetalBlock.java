package mattonfire.dnd.classes.Blocks.elven;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * A white flower that's a closed bud by day and opens its silver petals at night ({@link #OPEN}). It checks the
 * time every {@link #CHECK_TICKS} once it has been placed or randomly ticked (worldgen places it without either,
 * so a structure's moonpetals catch up on their first random tick).
 */
public class MoonpetalBlock extends FlowerBlock {
    public static final BooleanProperty OPEN = BooleanProperty.of("open");
    static final int CHECK_TICKS = 200;

    public MoonpetalBlock(Settings settings) {
        super(StatusEffects.NIGHT_VISION, 5, settings);
        this.setDefaultState(this.getDefaultState().with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    /** Whether it's night where the flower is (in a dimension with a day cycle). */
    public static boolean isNight(WorldView world) {
        if (!(world instanceof World w) || !w.getDimension().hasSkyLight() || w.getDimension().hasFixedTime()) {
            return false;
        }
        long time = w.getTimeOfDay() % 24000L;
        return time >= 13000L && time < 23000L;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(OPEN, isNight(ctx.getWorld()));
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient && !oldState.isOf(this)) {
            world.scheduleBlockTick(pos, this, CHECK_TICKS);
        }
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        this.update(state, world, pos);
        if (!world.getBlockTickScheduler().isQueued(pos, this)) {
            world.scheduleBlockTick(pos, this, CHECK_TICKS);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        this.update(state, world, pos);
        world.scheduleBlockTick(pos, this, CHECK_TICKS);
    }

    private void update(BlockState state, ServerWorld world, BlockPos pos) {
        boolean night = isNight(world);
        if (state.get(OPEN) != night) {
            world.setBlockState(pos, state.with(OPEN, night), Block.NOTIFY_LISTENERS);
        }
    }
}
