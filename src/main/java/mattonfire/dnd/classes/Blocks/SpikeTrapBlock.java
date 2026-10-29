package mattonfire.dnd.classes.Blocks;

import java.util.Locale;
import mattonfire.dnd.classes.SkillChecks.TrapDisarm;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A spike trap tile (design 10, 4.1): it looks like the floor round it (stone, coarse dirt or basalt, {@link #FLOOR}).
 * Stepping on any tile of the strip sets its {@link TrapKind#SPIKES} trap off, and iron spikes shoot up out of every
 * tile ({@link #EXTENDED}) for 2 s. The trap's state is in its buried trigger ({@link TrapTriggerBlockEntity}).
 * Unbreakable; a Rogue disarms it by sneak-using a tile.
 */
public class SpikeTrapBlock extends Block {
    public static final BooleanProperty EXTENDED = BooleanProperty.of("extended");
    public static final EnumProperty<Floor> FLOOR = EnumProperty.of("floor", Floor.class);
    /** The tile and, while out, the spikes' outline (no collision: they're hit, not stood on). */
    private static final VoxelShape SPIKES_OUT = VoxelShapes.union(VoxelShapes.fullCube(),
            Block.createCuboidShape(2, 16, 2, 14, 28, 14));

    /** What the tile is dressed as. */
    public enum Floor implements StringIdentifiable {
        STONE, COARSE_DIRT, BASALT;

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public SpikeTrapBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(EXTENDED, false).with(FLOOR, Floor.STONE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(EXTENDED, FLOOR);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(EXTENDED) ? SPIKES_OUT : VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
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
