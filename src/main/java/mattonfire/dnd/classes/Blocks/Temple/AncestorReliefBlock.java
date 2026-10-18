package mattonfire.dnd.classes.Blocks.Temple;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;

/**
 * Ancestor Relief: carved stone with a dragon's head in profile, the Dragon Temples' pillar stone. Its carved
 * face turns towards whoever places it, like a furnace.
 */
public class AncestorReliefBlock extends HorizontalFacingBlock {
    public AncestorReliefBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
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
