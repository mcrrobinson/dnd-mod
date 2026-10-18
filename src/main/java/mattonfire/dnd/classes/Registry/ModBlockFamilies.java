package mattonfire.dnd.classes.Registry;

import net.minecraft.block.Blocks;

/**
 * The mod's decorative building materials (see {@link BlockFamily} and docs/dev/building-blocks.md). Each one
 * is also listed in {@code tools/block_families.py}, which writes its resources.
 */
public final class ModBlockFamilies {
    /** The dwarves' fortress stone: deepslate bricks with gold-inlaid runes. */
    public static final BlockFamily RUNIC_DEEPSLATE_BRICKS = BlockFamily
            .builder("runic_deepslate_bricks", Blocks.DEEPSLATE_BRICKS).stoneShapes().build();

    private ModBlockFamilies() {
    }

    public static void register() {
        for (BlockFamily family : BlockFamily.all()) {
            family.register();
        }
    }
}
