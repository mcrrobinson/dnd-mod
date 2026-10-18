package mattonfire.dnd.classes.Blocks.adventuring;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * A Wanted Poster pinned to a wall: yellowed parchment, a red WANTED header and a crude sketch of one of
 * {@value #SKETCHES} villains ({@link #SKETCH}, picked at random when it's put up). Dressing, the wall-hung cousin of
 * the bounty notice: right-click it to read who's wanted and for how much. It hangs on the block behind it and drops
 * off when that goes, like a ladder.
 */
public class WantedPosterBlock extends HorizontalFacingBlock {
    public static final int SKETCHES = 4;
    public static final IntProperty SKETCH = IntProperty.of("sketch", 0, SKETCHES - 1);
    private static final VoxelShape NORTH = Block.createCuboidShape(1, 1, 15, 15, 15, 16);
    private static final VoxelShape SOUTH = Block.createCuboidShape(1, 1, 0, 15, 15, 1);
    private static final VoxelShape WEST = Block.createCuboidShape(15, 1, 1, 16, 15, 15);
    private static final VoxelShape EAST = Block.createCuboidShape(0, 1, 1, 1, 15, 15);

    public WantedPosterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(FACING, Direction.NORTH).with(SKETCH, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, SKETCH);
    }

    /** The poster facing {@code facing} (out from its wall) with sketch {@code sketch}. */
    public BlockState poster(Direction facing, int sketch) {
        return this.getDefaultState().with(FACING, facing).with(SKETCH, Math.floorMod(sketch, SKETCHES));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction facing = state.get(FACING);
        BlockPos wall = pos.offset(facing.getOpposite());
        return world.getBlockState(wall).isSideSolidFullSquare(world, wall, facing);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        int sketch = ctx.getWorld().getRandom().nextInt(SKETCHES);
        for (Direction dir : ctx.getPlacementDirections()) {
            if (dir.getAxis().isHorizontal()) {
                BlockState state = this.poster(dir.getOpposite(), sketch);
                if (state.canPlaceAt(ctx.getWorld(), ctx.getBlockPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.get(FACING).getOpposite() && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighbor, world, pos, neighborPos);
    }

    /** Reading it: who's wanted, and the reward, on the action bar. */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            player.sendMessage(Text.translatable("block.dndclasses.wanted_poster.read." + state.get(SKETCH))
                    .formatted(Formatting.GOLD), true);
        }
        return ActionResult.success(world.isClient);
    }
}
