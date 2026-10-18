package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Cogsdeep's Blueprint Table: a wooden table with a blueprint pinned on top. Using it opens a crafting grid, as a
 * crafting table does, titled "Blueprint Table". {@link #createScreenHandlerFactory} is the hook for later gnome
 * tinkering (Tinker commissions, Artificer schematics): override or replace it to open their screen instead.
 */
public class BlueprintTableBlock extends HorizontalFacingBlock {
    private static final Text TITLE = Text.translatable("container.dndclasses.blueprint_table");

    public BlueprintTableBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
        player.incrementStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        return ActionResult.CONSUME;
    }

    @Override
    public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory((syncId, inventory, player) ->
                new Grid(syncId, inventory, ScreenHandlerContext.create(world, pos), this), TITLE);
    }

    /** The crafting grid, kept open while the player stays by this table (vanilla's checks for a crafting table). */
    static final class Grid extends CraftingScreenHandler {
        private final ScreenHandlerContext context;
        private final Block table;

        Grid(int syncId, PlayerInventory inventory, ScreenHandlerContext context, Block table) {
            super(syncId, inventory, context);
            this.context = context;
            this.table = table;
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return canUse(this.context, player, this.table);
        }
    }
}
