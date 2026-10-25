package mattonfire.dnd.classes.Registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.underdark.FaerzressStoneBlock;
import mattonfire.dnd.classes.Blocks.underdark.UnderdarkFungusBlock;
import mattonfire.dnd.classes.Blocks.underdark.ChainCageBlock;
import mattonfire.dnd.classes.Blocks.underdark.DuergarBrazierBlock;
import mattonfire.dnd.classes.Blocks.underdark.SpidersilkCurtainBlock;
import mattonfire.dnd.classes.Blocks.underdark.WebBridgeBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.GlazedTerracottaBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The Underdark's natural blocks (design 9 T1; see docs/items/underdark-blocks.md). Zurkhwood Planks and its shapes
 * are a family in {@link ModBlockFamilies#ZURKHWOOD_PLANKS}. Resources: {@code tools/underdark_blocks.py}; textures:
 * {@code tools/underdark_block_textures.py}. Later Underdark tickets add their blocks here (T3 plants, T6 building
 * blocks: their one-offs below, resources in {@code tools/underdark_blocks.py}, textures in
 * {@code tools/underdark_building_textures.py}; see docs/items/underdark-blocks.md).
 */
public final class UnderdarkBlocks {
    /** The giant zurkhwood's purple-grey ribbed stalk: a log of Nether-like, non-flammable fungal wood. */
    public static final PillarBlock ZURKHWOOD_STEM = new PillarBlock(
            FabricBlockSettings.copyOf(Blocks.CRIMSON_STEM).mapColor(MapColor.PURPLE));
    /** The zurkhwood's dusky violet-brown flat cap, tan gills underneath (vanilla mushroom-block faces). */
    public static final MushroomBlock ZURKHWOOD_CAP = new MushroomBlock(
            FabricBlockSettings.copyOf(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.TERRACOTTA_PURPLE));
    /** A pale blue-white glowing cap (light 13): the fungal forests' light. */
    public static final MushroomBlock NIGHTLIGHT_CAP = new MushroomBlock(
            FabricBlockSettings.copyOf(Blocks.RED_MUSHROOM_BLOCK).mapColor(MapColor.LIGHT_BLUE)
                    .luminance(state -> 13));
    /** Dark blue-grey spongy floor with white spore specks. Doesn't spread or turn to dirt. */
    public static final Block FUNGAL_LOAM = new Block(FabricBlockSettings.of(Material.SOLID_ORGANIC, MapColor.TERRACOTTA_BLUE)
            .strength(0.6F).sounds(BlockSoundGroup.NYLIUM));
    /** Deepslate veined with glowing violet faerzress (light 5). */
    public static final FaerzressStoneBlock FAERZRESS_STONE = new FaerzressStoneBlock(
            FabricBlockSettings.copyOf(Blocks.DEEPSLATE).mapColor(MapColor.PURPLE).luminance(state -> 5));
    /** A small glowing blue mushroom (light 6), happy in any light. */
    public static final UnderdarkFungusBlock BLUECAP = new UnderdarkFungusBlock(fungus(MapColor.BLUE).luminance(state -> 6));
    /** A small grey-tan mushroom (food in T3). */
    public static final UnderdarkFungusBlock TRILLIMAC = new UnderdarkFungusBlock(fungus(MapColor.PALE_YELLOW));

    // --- Building blocks (design 9 T6). The ones with shapes are families in ModBlockFamilies: Drowstone Bricks,
    // Polished Faerzress, Greysteel Plate and Basalt Forge Bricks. ---
    /** Drowstone carved with a spider sigil and a glowing violet inlay (light 4). Faces you, like glazed terracotta. */
    public static final GlazedTerracottaBlock SPIDER_SIGIL_STONE = new GlazedTerracottaBlock(FabricBlockSettings
            .copyOf(Blocks.POLISHED_BLACKSTONE_BRICKS).mapColor(MapColor.BLACK).luminance(state -> 4));
    /** A black-iron lantern burning with a violet faerie-fire flame (light 12). Stands or hangs. */
    public static final LanternBlock FAERIE_LANTERN = new LanternBlock(FabricBlockSettings.copyOf(Blocks.SOUL_LANTERN)
            .luminance(state -> 12));
    /** Pale silver spidersilk, woven thick (wool's hardness and sound). */
    public static final Block SPIDERSILK_BLOCK = new Block(FabricBlockSettings.copyOf(Blocks.WHITE_WOOL)
            .mapColor(MapColor.LIGHT_GRAY));
    public static final CarpetBlock SPIDERSILK_CARPET = new CarpetBlock(FabricBlockSettings.copyOf(Blocks.WHITE_CARPET)
            .mapColor(MapColor.LIGHT_GRAY));
    /** Silk hung like a pane: walk through it, slowly ({@link SpidersilkCurtainBlock}). */
    public static final SpidersilkCurtainBlock SPIDERSILK_CURTAIN = new SpidersilkCurtainBlock(FabricBlockSettings
            .copyOf(Blocks.WHITE_WOOL).mapColor(MapColor.LIGHT_GRAY).strength(0.4F).noCollision().nonOpaque());
    /** A woven silk walkway, a top-slab-high plank (scaffolding's hardness: breaks at once). */
    public static final WebBridgeBlock WEB_BRIDGE = new WebBridgeBlock(FabricBlockSettings.copyOf(Blocks.WHITE_WOOL)
            .mapColor(MapColor.LIGHT_GRAY).strength(0.0F).nonOpaque());
    /** The Holds' coal bowl (light 14): smokes, and burns all but duergar ({@link DuergarBrazierBlock}). */
    public static final DuergarBrazierBlock DUERGAR_BRAZIER = new DuergarBrazierBlock(FabricBlockSettings
            .copyOf(Blocks.IRON_BLOCK).mapColor(MapColor.BLACK).strength(3.5F).nonOpaque().luminance(state -> 14)
            .sounds(BlockSoundGroup.LANTERN));
    /** The captive pens' barred gate, lockable ({@link ChainCageBlock}). */
    public static final ChainCageBlock CHAIN_CAGE = new ChainCageBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS)
            .strength(5.0F, 6.0F).nonOpaque());
    /** Duergar steel: a dark crafting metal. 9 make a Greysteel Plate; the duergar sell and drop it. */
    public static final Item DUERGAR_STEEL_INGOT = new Item(new FabricItemSettings());

    /** Mobs a Duergar Brazier doesn't burn. Holds the duergar's faction tag when that exists (optional entry). */
    public static final TagKey<EntityType<?>> BRAZIER_IMMUNE = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "brazier_immune"));
    /** Mobs a Spidersilk Curtain doesn't slow: spiders, and the drow's faction tag when that exists. */
    public static final TagKey<EntityType<?>> WEB_WALKERS = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "web_walkers"));

    private UnderdarkBlocks() {
    }

    /** A small mushroom's settings: no collision, breaks instantly, no random ticks (it never spreads). */
    private static FabricBlockSettings fungus(MapColor color) {
        return FabricBlockSettings.of(Material.PLANT, color).noCollision().breakInstantly()
                .sounds(BlockSoundGroup.FUNGUS);
    }

    /** Every block, in creative-tab order, with its id path. */
    public static Map<String, Block> all() {
        Map<String, Block> blocks = new LinkedHashMap<>();
        blocks.put("zurkhwood_stem", ZURKHWOOD_STEM);
        blocks.put("zurkhwood_cap", ZURKHWOOD_CAP);
        blocks.put("nightlight_cap", NIGHTLIGHT_CAP);
        blocks.put("fungal_loam", FUNGAL_LOAM);
        blocks.put("faerzress_stone", FAERZRESS_STONE);
        blocks.put("bluecap", BLUECAP);
        blocks.put("trillimac", TRILLIMAC);
        return blocks;
    }

    /** The building blocks (T6) without shapes, in creative-tab order (DnD Building Blocks, after the families). */
    public static Map<String, Block> buildingBlocks() {
        Map<String, Block> blocks = new LinkedHashMap<>();
        blocks.put("spider_sigil_stone", SPIDER_SIGIL_STONE);
        blocks.put("faerie_lantern", FAERIE_LANTERN);
        blocks.put("spidersilk_block", SPIDERSILK_BLOCK);
        blocks.put("spidersilk_carpet", SPIDERSILK_CARPET);
        blocks.put("spidersilk_curtain", SPIDERSILK_CURTAIN);
        blocks.put("web_bridge", WEB_BRIDGE);
        blocks.put("duergar_brazier", DUERGAR_BRAZIER);
        blocks.put("chain_cage", CHAIN_CAGE);
        return blocks;
    }

    /** The building blocks the client draws cut out (see-through gaps). */
    public static List<Block> buildingCutoutBlocks() {
        return List.of(FAERIE_LANTERN, SPIDERSILK_CURTAIN, WEB_BRIDGE, DUERGAR_BRAZIER, CHAIN_CAGE);
    }

    /** Blocks the client draws cut out (the small mushrooms' cross models). */
    public static List<Block> cutoutBlocks() {
        return List.of(BLUECAP, TRILLIMAC);
    }

    public static void register() {
        all().forEach((name, block) -> {
            Identifier id = new Identifier(DnDClasses.MOD_ID, name);
            Registry.register(Registries.BLOCK, id, block);
            Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
        });
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            for (Block block : all().values()) {
                entries.add(block);
            }
        });
        registerBuildingBlocks();
    }

    private static void registerBuildingBlocks() {
        buildingBlocks().forEach((name, block) -> {
            Identifier id = new Identifier(DnDClasses.MOD_ID, name);
            Registry.register(Registries.BLOCK, id, block);
            Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
        });
        Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, "duergar_steel_ingot"), DUERGAR_STEEL_INGOT);
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.BUILDING_BLOCKS_ITEMGROUP).register(entries -> {
            for (Block block : buildingBlocks().values()) {
                entries.add(block);
            }
        });
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries ->
                entries.add(DUERGAR_STEEL_INGOT));
    }
}
