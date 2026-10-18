package mattonfire.dnd.classes.Blocks.Temple;

import java.util.function.BiConsumer;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
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
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Prayer Wheel: a brass-and-red drum on an axle, in a row along the Dragon Temple's cloister walk.
 * Right-clicking it sets it spinning for {@link #SPIN_TICKS} (a creak and a chime; the model swaps to an
 * animated texture while {@link #SPINNING}).
 */
public class PrayerWheelBlock extends Block {
    public static final BooleanProperty SPINNING = BooleanProperty.of("spinning");
    /** How long a push keeps the wheel turning. */
    public static final int SPIN_TICKS = 60;
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(2, 0, 2, 14, 2, 14),
            Block.createCuboidShape(3, 2, 3, 13, 14, 13),
            Block.createCuboidShape(2, 14, 2, 14, 16, 14));

    /**
     * Called on the server whenever a player spins a wheel. A hook for a later daily blessing or XP for spinning
     * every wheel in a temple; nothing listens yet.
     */
    public static BiConsumer<ServerPlayerEntity, BlockPos> onSpin = (player, pos) -> {
    };

    public PrayerWheelBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(SPINNING, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SPINNING);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!state.get(SPINNING)) {
            world.setBlockState(pos, state.with(SPINNING, true), Block.NOTIFY_ALL);
        }
        // Another push keeps it turning for the full time.
        world.scheduleBlockTick(pos, this, SPIN_TICKS);
        world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.35F, 0.6F);
        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0F, 0.8F);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            DnDClasses.LOGGER.info("[TempleBlocks] {} spun the prayer wheel at {}", player.getEntityName(),
                    pos.toShortString());
            onSpin.accept(serverPlayer, pos);
        }
        return ActionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(SPINNING)) {
            world.setBlockState(pos, state.with(SPINNING, false), Block.NOTIFY_ALL);
        }
    }
}
