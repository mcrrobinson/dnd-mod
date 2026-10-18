package mattonfire.dnd.classes.Blocks;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A blackstone lectern with the Brimstone Covenant's contract draped over it, beside the Pactbroker. Right-click
 * reads the Covenant's law (the sanctum's rules) in chat. Shaped like a vanilla lectern; it holds no book.
 */
public class ContractLecternBlock extends HorizontalFacingBlock {
    /** The law's lines, {@code block.dndclasses.contract_lectern.law.<n>}. */
    public static final int LAW_LINES = 4;

    public ContractLecternBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean hasSidedTransparency(BlockState state) {
        return true;
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return LecternBlock.BASE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return LecternBlock.COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> LecternBlock.NORTH_SHAPE;
            case SOUTH -> LecternBlock.SOUTH_SHAPE;
            case EAST -> LecternBlock.EAST_SHAPE;
            default -> LecternBlock.WEST_SHAPE;
        };
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        player.sendMessage(Text.translatable("block.dndclasses.contract_lectern.title")
                .formatted(Formatting.RED, Formatting.BOLD));
        for (int i = 1; i <= LAW_LINES; i++) {
            player.sendMessage(Text.translatable("block.dndclasses.contract_lectern.law." + i)
                    .formatted(Formatting.GOLD));
        }
        world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1.0F, 0.8F);
        DnDClasses.LOGGER.info("[ContractLectern] {} read the Covenant's law at {}", player.getName().getString(),
                pos.toShortString());
        return ActionResult.CONSUME;
    }
}
