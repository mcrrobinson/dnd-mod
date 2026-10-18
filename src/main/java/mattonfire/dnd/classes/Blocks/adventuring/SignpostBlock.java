package mattonfire.dnd.classes.Blocks.adventuring;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * An Adventurer's Signpost: a wooden post with an arrow-shaped board pointing {@link #FACING}, its text
 * ({@link SignpostBlockEntity}) on both faces of the board. Dungeon entrances put one up naming the dungeon and its
 * Challenge tier. A player's signpost says whatever its item was named in an anvil. Right-click to read it on the
 * action bar.
 */
public class SignpostBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    /** Post and board, for the arrow pointing north (the model's unrotated way). */
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            SHAPES[dir.getHorizontal()] = VoxelShapes.union(rotated(dir, 7, 0, 12, 9, 16, 14),
                    rotated(dir, 7, 8, 0, 9, 15, 16));
        }
    }

    public SignpostBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(FACING, Direction.NORTH));
    }

    /** A box given for the north-pointing model, turned to point {@code dir}. */
    private static VoxelShape rotated(Direction dir, double x1, double y1, double z1, double x2, double y2, double z2) {
        return switch (dir) {
            case NORTH -> Block.createCuboidShape(x1, y1, z1, x2, y2, z2);
            case SOUTH -> Block.createCuboidShape(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case EAST -> Block.createCuboidShape(16 - z2, y1, x1, 16 - z1, y2, x2);
            default -> Block.createCuboidShape(z1, y1, 16 - x2, z2, y2, 16 - x1);
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The arrow points the way the player looks. */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(FACING).getHorizontal()];
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SignpostBlockEntity(pos, state);
    }

    /** A signpost item named in an anvil writes its name on the board. */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!world.isClient && stack.hasCustomName() && world.getBlockEntity(pos) instanceof SignpostBlockEntity post) {
            post.setLines(List.of(stack.getName()));
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof SignpostBlockEntity post) {
            post.refresh((ServerWorld) world);
            Text text = post.lines().isEmpty()
                    ? Text.translatable("block.dndclasses.adventurers_signpost.blank").formatted(Formatting.GRAY)
                    : post.joined();
            player.sendMessage(text, true);
        }
        return ActionResult.success(world.isClient);
    }
}
