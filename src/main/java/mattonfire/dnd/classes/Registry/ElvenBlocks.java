package mattonfire.dnd.classes.Registry;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.elven.GlimmercapBlock;
import mattonfire.dnd.classes.Blocks.elven.HangingRopePlankBlock;
import mattonfire.dnd.classes.Blocks.elven.MoonpetalBlock;
import mattonfire.dnd.classes.Blocks.elven.SylvanGlyphStoneBlock;
import mattonfire.dnd.classes.Blocks.elven.WovenRopePlankBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.TallBlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * The Elven Enclaves' one-off building blocks (the materials with shapes, Heartwood, Moonstone and Living
 * Wood, are families in {@link ModBlockFamilies}). Resources come from {@code tools/elven_block_textures.py}.
 * They're listed in the DnD Building Blocks tab after the families.
 */
public final class ElvenBlocks {
    /** The Heart Tree's canopy: silver-green leaves, not tinted by the biome. */
    public static final LeavesBlock SILVERLEAF = new LeavesBlock(FabricBlockSettings.copyOf(Blocks.BIRCH_LEAVES));
    /** A rope bridge's deck, slab-thin (see {@link WovenRopePlankBlock}). */
    public static final WovenRopePlankBlock WOVEN_ROPE_PLANK = new WovenRopePlankBlock(
            FabricBlockSettings.copyOf(Blocks.BIRCH_PLANKS).strength(1.0F).nonOpaque());
    /** A Woven Rope Plank hanging on edge from a fallen bridge (see {@link HangingRopePlankBlock}); no item. */
    public static final HangingRopePlankBlock HANGING_ROPE_PLANK = new HangingRopePlankBlock(
            FabricBlockSettings.copyOf(Blocks.BIRCH_PLANKS).strength(1.0F).nonOpaque().noCollision());
    /** Two-high standing stone marking the edge of Sylvan Law; its glyph glows (light 6). */
    public static final SylvanGlyphStoneBlock SYLVAN_GLYPH_STONE = new SylvanGlyphStoneBlock(
            FabricBlockSettings.copyOf(Blocks.MOSSY_COBBLESTONE).luminance(state -> 6).nonOpaque());
    /** Pale blue glowing mushrooms (light 7). */
    public static final GlimmercapBlock GLIMMERCAP = new GlimmercapBlock(
            FabricBlockSettings.copyOf(Blocks.BROWN_MUSHROOM).luminance(state -> 7).ticksRandomly());
    /** A white flower that opens at night. */
    public static final MoonpetalBlock MOONPETAL = new MoonpetalBlock(FabricBlockSettings.copyOf(Blocks.OXEYE_DAISY));
    /** Faintly blue glass with star-like specks and a thin silver frame. */
    public static final GlassBlock STARGLASS = new GlassBlock(FabricBlockSettings.copyOf(Blocks.GLASS));
    public static final PaneBlock STARGLASS_PANE = new PaneBlock(FabricBlockSettings.copyOf(Blocks.GLASS_PANE));

    private static final List<Item> ITEMS = new ArrayList<>();

    private ElvenBlocks() {
    }

    public static void register() {
        register("silverleaf", SILVERLEAF);
        register("woven_rope_plank", WOVEN_ROPE_PLANK);
        Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, "hanging_rope_plank"), HANGING_ROPE_PLANK);
        register("sylvan_glyph_stone", SYLVAN_GLYPH_STONE);
        register("glimmercap", GLIMMERCAP);
        register("moonpetal", MOONPETAL);
        register("starglass", STARGLASS);
        register("starglass_pane", STARGLASS_PANE);

        StrippableBlockRegistry.register(ModBlockFamilies.HEARTWOOD_LOG.base(), ModBlockFamilies.STRIPPED_HEARTWOOD_LOG.base());
        FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
        for (BlockFamily family : List.of(ModBlockFamilies.HEARTWOOD_LOG, ModBlockFamilies.HEARTWOOD_WOOD,
                ModBlockFamilies.STRIPPED_HEARTWOOD_LOG, ModBlockFamilies.LIVING_WOOD)) {
            fire.add(family.base(), 5, 5);
        }
        for (BlockFamily.Shape shape : new BlockFamily.Shape[] { BlockFamily.Shape.BASE, BlockFamily.Shape.STAIRS,
                BlockFamily.Shape.SLAB, BlockFamily.Shape.FENCE, BlockFamily.Shape.FENCE_GATE }) {
            fire.add(ModBlockFamilies.HEARTWOOD_PLANKS.get(shape), 5, 20);
        }
        fire.add(WOVEN_ROPE_PLANK, 5, 20);
        fire.add(HANGING_ROPE_PLANK, 5, 20);
        fire.add(SILVERLEAF, 30, 60);
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.BUILDING_BLOCKS_ITEMGROUP).register(entries -> ITEMS.forEach(entries::add));
    }

    private static void register(String name, Block block) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Item item = block == SYLVAN_GLYPH_STONE ? new TallBlockItem(block, new FabricItemSettings())
                : new BlockItem(block, new FabricItemSettings());
        Registry.register(Registries.ITEM, id, item);
        ITEMS.add(item);
    }

    /** Blocks the client draws cut out (with see-through pixels). */
    public static List<Block> cutoutBlocks() {
        return List.of(WOVEN_ROPE_PLANK, HANGING_ROPE_PLANK, GLIMMERCAP, MOONPETAL);
    }

    /** Blocks the client draws translucent (tinted glass). */
    public static List<Block> translucentBlocks() {
        return List.of(STARGLASS, STARGLASS_PANE);
    }
}
