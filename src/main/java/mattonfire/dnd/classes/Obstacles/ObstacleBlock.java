package mattonfire.dnd.classes.Obstacles;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A block of a class-gated obstacle. SEALED it's solid and in the way; OPEN it has no collision or
 * outline and only a faint "broken" model, but keeps its block entity so it can be resealed. Each
 * block belongs to one {@link ObstacleType}; right-clicking is handled by {@link ObstacleInteractions}.
 */
public class ObstacleBlock extends BlockWithEntity {
    public static final EnumProperty<ObstacleState> STATE = EnumProperty.of("state", ObstacleState.class);

    private final ObstacleType type;

    public ObstacleBlock(ObstacleType type, Settings settings) {
        super(settings);
        this.type = type;
        setDefaultState(getStateManager().getDefaultState().with(STATE, ObstacleState.SEALED));
    }

    public ObstacleType type() {
        return type;
    }

    public static boolean isSealed(BlockState state) {
        return state.getBlock() instanceof ObstacleBlock && state.get(STATE) == ObstacleState.SEALED;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STATE);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(STATE) == ObstacleState.SEALED ? VoxelShapes.fullCube() : VoxelShapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(STATE) == ObstacleState.SEALED ? VoxelShapes.fullCube() : VoxelShapes.empty();
    }

    @Override
    public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        return stateFrom.isOf(this) && stateFrom.get(STATE) == state.get(STATE)
                || super.isSideInvisible(state, stateFrom, direction);
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        // Open blocks can't be targeted anyway; sealed ones break only if the type has a break fallback
        return type.breakable() ? super.calcBlockBreakingDelta(state, player, world, pos) : 0.0f;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        return ObstacleInteractions.onUse(this, state, world, pos, player, hand);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer
                && !player.isCreative() && state.get(STATE) == ObstacleState.SEALED) {
            type.onBrokenByHand(serverWorld, pos, serverPlayer);
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
            ObstacleIndex.get(serverWorld).remove(pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ObstacleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
            BlockEntityType<T> blockEntityType) {
        return world.isClient ? null
                : checkType(blockEntityType, ObstacleTypes.BLOCK_ENTITY, ObstacleBlockEntity::tick);
    }
}
