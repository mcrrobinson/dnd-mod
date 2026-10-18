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

    // --- Brimstone Sanctums ---
    /** Dark basalt crusted with sulphur crystals: the sanctum's platform, walls and Pact Hall stone. */
    public static final BlockFamily BRIMSTONE = BlockFamily.builder("brimstone", Blocks.BASALT).stoneShapes().build();
    public static final BlockFamily POLISHED_BRIMSTONE = BlockFamily
            .builder("polished_brimstone", Blocks.POLISHED_BASALT).stoneShapes().build();
    public static final BlockFamily BRIMSTONE_BRICKS = BlockFamily
            .builder("brimstone_bricks", Blocks.POLISHED_BLACKSTONE_BRICKS).stoneShapes().build();
    /** Dark brass plates with an ember-red sheen: the Brass Market's counters and the Refuge's anchor frames. */
    public static final BlockFamily HELLBRASS = BlockFamily.builder("hellbrass", Blocks.COPPER_BLOCK).build();
    public static final BlockFamily CUT_HELLBRASS = BlockFamily.builder("cut_hellbrass", Blocks.CUT_COPPER)
            .stairs().slab().build();

    private ModBlockFamilies() {
    }

    public static void register() {
        for (BlockFamily family : BlockFamily.all()) {
            family.register();
        }
    }
}
