package mattonfire.dnd.classes.Registry;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.Blocks;
import net.minecraft.block.WoodType;

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
    // --- Elven Enclaves ---
    /** The Heart Tree's own wood: silver-white bark, pale cream planks with a curving gold-green grain. */
    public static final BlockFamily HEARTWOOD_LOG = BlockFamily.builder("heartwood_log", Blocks.BIRCH_LOG)
            .axisBase().build();
    /** Heartwood bark on every side, as the Heart Tree's trunk and boughs are built. */
    public static final BlockFamily HEARTWOOD_WOOD = BlockFamily.builder("heartwood_wood", Blocks.BIRCH_WOOD)
            .axisBase().build();
    public static final BlockFamily STRIPPED_HEARTWOOD_LOG = BlockFamily
            .builder("stripped_heartwood_log", Blocks.STRIPPED_BIRCH_LOG).axisBase().build();
    public static final BlockFamily HEARTWOOD_PLANKS = BlockFamily.builder("heartwood_planks", Blocks.BIRCH_PLANKS)
            .stem("heartwood").stairs().slab().fence().fenceGate(WoodType.BIRCH).door(BlockSetType.BIRCH)
            .trapdoor(BlockSetType.BIRCH).build();
    /** Pale stone that holds moonlight: it glows faintly (light 4). */
    public static final BlockFamily MOONSTONE = BlockFamily
            .builder("moonstone", FabricBlockSettings.copyOf(Blocks.CALCITE).luminance(state -> 4)).stoneShapes()
            .build();
    public static final BlockFamily POLISHED_MOONSTONE = BlockFamily
            .builder("polished_moonstone", FabricBlockSettings.copyOf(Blocks.CALCITE).luminance(state -> 4)).build();
    public static final BlockFamily MOONSTONE_BRICKS = BlockFamily
            .builder("moonstone_bricks", FabricBlockSettings.copyOf(Blocks.CALCITE).luminance(state -> 4))
            .stoneShapes().build();
    /** Bark grown into a wall, with leaves, tendrils and tiny flowers sprouting from it: elves grow their homes. */
    public static final BlockFamily LIVING_WOOD = BlockFamily.builder("living_wood", Blocks.BIRCH_WOOD).build();
    // --- Dwarven Fortresses ---
    /** Runic brick carved with one large rune in a gold frame: fortress pillar caps and door frames. */
    public static final BlockFamily CHISELED_RUNIC_DEEPSLATE_BRICKS = BlockFamily
            .builder("chiseled_runic_deepslate_bricks", Blocks.CHISELED_DEEPSLATE).build();
    /** Heat-blackened brick with ember mortar, for the fortress forges. Gives off a little light. */
    public static final BlockFamily FORGE_BRICKS = BlockFamily
            .builder("forge_bricks", FabricBlockSettings.copyOf(Blocks.BRICKS).luminance(state -> 3)).stairs().slab()
            .build();
    /** A fluted dark stone column with gold run into its cracks; lies along an axis like a log. */
    public static final BlockFamily GOLD_VEINED_PILLAR = BlockFamily
            .builder("gold_veined_pillar", Blocks.POLISHED_DEEPSLATE).axisBase().build();
    // --- Orc Strongholds ---
    /** Stitched leather panels, the yurts' walls and roofs: brown, pale and grey, like the old wool mix. */
    public static final BlockFamily TANNED_HIDE = BlockFamily.builder("tanned_hide", Blocks.BROWN_WOOL).stairs().slab().build();
    public static final BlockFamily PALE_TANNED_HIDE = BlockFamily.builder("pale_tanned_hide", Blocks.WHITE_WOOL).stairs()
            .slab().build();
    public static final BlockFamily GREY_TANNED_HIDE = BlockFamily.builder("grey_tanned_hide", Blocks.LIGHT_GRAY_WOOL)
            .stairs().slab().build();
    /** Tanned hide with the Bloodtusk mark in red ochre: two crossed tusks under a dripping handprint. */
    public static final BlockFamily PAINTED_HIDE = BlockFamily.builder("painted_hide", Blocks.BROWN_WOOL).build();
    /** Mud bricks daubed with red ochre handprints and streaks: the Fighting Pit. */
    public static final BlockFamily BLOODTUSK_MUD_BRICKS = BlockFamily
            .builder("bloodtusk_mud_bricks", Blocks.MUD_BRICKS).stoneShapes().build();
    /** Glassy black-green lumps with rusty pockets, heaped outside the smithy. */
    public static final BlockFamily SMITHY_SLAG = BlockFamily.builder("smithy_slag", Blocks.BLACKSTONE).build();

    private ModBlockFamilies() {
    }

    public static void register() {
        for (BlockFamily family : BlockFamily.all()) {
            family.register();
        }
    }
}
