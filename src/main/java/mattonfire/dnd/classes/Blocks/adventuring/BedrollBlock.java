package mattonfire.dnd.classes.Blocks.adventuring;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A canvas bedroll laid out on the ground: two blocks long like a bed (a foot and a head with a pillow), and slept
 * in like one, so it gives a long rest at night ({@code BedRest}). It never sets the sleeper's spawn point
 * ({@code AdventuringBlocks} vetoes it), doesn't bounce, and in the Nether or the End it just refuses instead of
 * exploding. Drawn from a block model, not the bed's block entity, so it has no block entity.
 */
public class BedrollBlock extends BedBlock {
    /** The roll is 3 px thick; the sleeper lies this much lower than in a bed. */
    public static final double SLEEP_DROP = (9.0D - 3.0D) / 16.0D;
    private static final VoxelShape FOOT = Block.createCuboidShape(0, 0, 0, 16, 3, 16);
    /** The head, with its pillow at the end the bedroll faces. */
    private static final VoxelShape[] HEAD = new VoxelShape[4];

    static {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            double[] pillow = switch (dir) {
                case NORTH -> new double[]{3, 1, 13, 7};
                case SOUTH -> new double[]{3, 9, 13, 15};
                case WEST -> new double[]{1, 3, 7, 13};
                default -> new double[]{9, 3, 15, 13};
            };
            HEAD[dir.getHorizontal()] = VoxelShapes.union(FOOT,
                    Block.createCuboidShape(pillow[0], 3, pillow[1], pillow[2], 6, pillow[3]));
        }
    }

    public BedrollBlock(DyeColor color, Settings settings) {
        super(color, settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(PART) == BedPart.HEAD ? HEAD[state.get(FACING).getHorizontal()] : FOOT;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!isBedWorking(world)) {
            if (!world.isClient) {
                player.sendMessage(Text.translatable("block.dndclasses.bedroll.cant_sleep_here"), true);
            }
            return ActionResult.success(world.isClient);
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    /** Plain ground: full fall damage, no bounce. */
    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.handleFallDamage(fallDistance, 1.0F, world.getDamageSources().fall());
    }

    @Override
    public void onEntityLand(BlockView world, Entity entity) {
        entity.setVelocity(entity.getVelocity().multiply(1.0D, 0.0D, 1.0D));
    }

    /** The bedroll's foot half, facing {@code facing} (its head goes one block that way). */
    public BlockState foot(Direction facing) {
        return this.getDefaultState().with(FACING, facing).with(PART, BedPart.FOOT);
    }

    public BlockState head(Direction facing) {
        return this.getDefaultState().with(FACING, facing).with(PART, BedPart.HEAD);
    }
}
