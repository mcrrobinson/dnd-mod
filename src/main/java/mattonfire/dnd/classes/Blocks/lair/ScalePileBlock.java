package mattonfire.dnd.classes.Blocks.lair;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;

/**
 * A Shed Scales Pile: 1-8 layers of a dragon's shed scales that stack like snow. It never melts (its settings
 * don't tick randomly) and, unlike snow, lies on ice too. Mining it gives that dragon's scales, one per layer,
 * not the block ({@code loot_tables/blocks/<storm|frost>_scale_pile.json}).
 */
public class ScalePileBlock extends SnowBlock {
    public ScalePileBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        if (below.isOf(this)) {
            return below.get(LAYERS) == MAX_LAYERS;
        }
        return Block.isFaceFullSquare(below.getCollisionShape(world, pos.down()), Direction.UP);
    }
}
