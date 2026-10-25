package mattonfire.dnd.classes.Blocks.underdark;

import net.minecraft.block.BlockState;
import net.minecraft.block.GlowLichenBlock;
import net.minecraft.item.ItemPlacementContext;

/**
 * Faerie Lichen: glow lichen's violet, brighter (light 9) Underdark cousin. Grows on any face and spreads with bone
 * meal like glow lichen; more of it placed on it adds faces.
 */
public class FaerieLichenBlock extends GlowLichenBlock {
    public FaerieLichenBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        // Glow lichen only lets more glow lichen add faces; this lets more Faerie Lichen do it.
        return !context.getStack().isOf(asItem()) || hasAnyOpenFace(state);
    }

    private static boolean hasAnyOpenFace(BlockState state) {
        for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.values()) {
            if (!hasDirection(state, direction)) {
                return true;
            }
        }
        return false;
    }
}
