package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;

/**
 * A chiseled wall block with a slit, facing the corridor: it shoots its trap's darts. It does nothing on its
 * own; the trap's trigger tile ({@link TrapTriggerBlockEntity}) fires it. Unbreakable.
 */
public class DartLauncherBlock extends HorizontalFacingBlock {
    public DartLauncherBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
}
