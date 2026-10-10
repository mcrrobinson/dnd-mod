package mattonfire.dnd.classes.Blocks;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.SkillChecks.TrapDisarm;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A dungeon trap's hidden pressure tile: it looks like the floor round it. Its block entity
 * ({@link TrapTriggerBlockEntity}) holds the trap. Once the trap has gone off (or been disarmed) the tile sits
 * a little lower ({@link #ARMED} false) until the dungeon resets. Unbreakable.
 */
public class TrapTriggerBlock extends BlockWithEntity {
    public static final BooleanProperty ARMED = BooleanProperty.of("armed");
    private static final VoxelShape PRESSED = Block.createCuboidShape(0, 0, 0, 16, 15, 16);

    public TrapTriggerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(ARMED, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ARMED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(ARMED) ? super.getOutlineShape(state, world, pos, context) : PRESSED;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TrapTriggerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, TrapBlocks.TRAP_TRIGGER_ENTITY, TrapTriggerBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return TrapDisarm.onUse(world, pos, player, hand);
    }

    @Override
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        TrapDisarm.onProjectileHit(world, hit.getBlockPos(), projectile);
    }
}
