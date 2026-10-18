package mattonfire.dnd.classes.Blocks.orc;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.OrcEntity;
import mattonfire.dnd.entity.OrcHostility;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Sharpened Stake Wall: a wall of dark oak stakes, the palisade of an Orc Stronghold. {@link #FACING} is its
 * outside (the way the player placing it looks, or away from the hold); anything that walks into it from that
 * side takes {@link #DAMAGE} (like a sweet berry bush, but without the slowing). Orcs and their goblin allies
 * know the way through and aren't hurt, nor is anyone touching it from the inside.
 */
public class StakeWallBlock extends WallBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final float DAMAGE = 1.0F;
    public static final RegistryKey<DamageType> STAKE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "stake"));

    public StakeWallBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        return state == null ? null : state.with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return super.rotate(state, rotation).with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return super.mirror(state, mirror).with(FACING, mirror.apply(state.get(FACING)));
    }

    // WallBlock keys its shapes by state with its own properties only (the default FACING): look them up there.
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return super.getOutlineShape(state.with(FACING, Direction.NORTH), world, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return super.getCollisionShape(state.with(FACING, Direction.NORTH), world, pos, context);
    }

    /** Whether {@code entity} is on the outside of the stake at {@code pos}. */
    public static boolean outside(BlockState state, BlockPos pos, Entity entity) {
        Direction out = state.get(FACING);
        double dx = entity.getX() - (pos.getX() + 0.5D);
        double dz = entity.getZ() - (pos.getZ() + 0.5D);
        return dx * out.getOffsetX() + dz * out.getOffsetZ() > 0.0D;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient || !(entity instanceof LivingEntity) || entity instanceof OrcEntity
                || OrcHostility.isAlly(entity) || entity instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())
                || !outside(state, pos, entity)) {
            return;
        }
        entity.damage(new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(STAKE)), DAMAGE);
    }
}
