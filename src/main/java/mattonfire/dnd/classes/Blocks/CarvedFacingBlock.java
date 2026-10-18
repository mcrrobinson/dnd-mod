package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.Direction;

/**
 * A decorative block with a carved front that faces the player who places it (the Crypt's Funerary
 * Relief and Burial Niche Wall). The model's front is on its north side; the blockstate turns it.
 */
public class CarvedFacingBlock extends HorizontalFacingBlock {
    public CarvedFacingBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    /** This block with its front facing {@code front}. */
    public BlockState facing(Direction front) {
        return this.getDefaultState().with(FACING, front);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.facing(ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
