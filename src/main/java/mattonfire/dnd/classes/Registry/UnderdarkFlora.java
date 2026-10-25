package mattonfire.dnd.classes.Registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.underdark.BarrelstalkBlock;
import mattonfire.dnd.classes.Blocks.underdark.FaerieLichenBlock;
import mattonfire.dnd.classes.Blocks.underdark.GiantFungusSporesBlock;
import mattonfire.dnd.classes.Blocks.underdark.RipplebarkBlock;
import mattonfire.dnd.classes.Blocks.underdark.SporeDripBlock;
import mattonfire.dnd.classes.Blocks.underdark.WaterorbBlock;
import mattonfire.dnd.world.underdark.feature.UnderdarkFeatures;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlowLichenBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.PlaceableOnWaterItem;
import net.minecraft.item.TallBlockItem;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The Underdark's plants, fungi and lake life (design 9 T3; see docs/items/underdark-blocks.md): giant-fungus spores
 * and gills, Ripplebark, Barrelstalk, Waterorb, Faerie Lichen, Glowing Spore Drip, Pearl-Clam, and the food and goods
 * they give. Kept apart from {@link UnderdarkBlocks} (the foundation's blocks, and the building blocks a later ticket
 * adds there). Resources: {@code tools/underdark_flora.py}; textures: {@code tools/underdark_flora_textures.py}. The
 * giant fungi they grow are {@link UnderdarkFeatures}.
 */
public final class UnderdarkFlora {
    /** The underside of a zurkhwood cap: tan gills that now and then drop Zurkhwood Spores. */
    public static final Block ZURKHWOOD_GILLS = new Block(FabricBlockSettings.copyOf(Blocks.BROWN_MUSHROOM_BLOCK)
            .mapColor(MapColor.TERRACOTTA_WHITE));
    public static final GiantFungusSporesBlock ZURKHWOOD_SPORES = new GiantFungusSporesBlock(
            "underdark/giant_zurkhwood", spores(MapColor.TERRACOTTA_PURPLE).luminance(state -> 0));
    public static final GiantFungusSporesBlock NIGHTLIGHT_SPORES = new GiantFungusSporesBlock(
            "underdark/nightlight_tower", spores(MapColor.LIGHT_BLUE).luminance(state -> 3));
    public static final RipplebarkBlock RIPPLEBARK = new RipplebarkBlock(FabricBlockSettings.of(Material.PLANT,
            MapColor.TERRACOTTA_ORANGE).strength(0.2F, 3.0F).ticksRandomly().nonOpaque().sounds(BlockSoundGroup.FUNGUS));
    public static final BarrelstalkBlock BARRELSTALK = new BarrelstalkBlock(FabricBlockSettings.of(Material.PLANT,
            MapColor.TERRACOTTA_BROWN).strength(0.5F).nonOpaque().sounds(BlockSoundGroup.WART_BLOCK));
    public static final WaterorbBlock WATERORB = new WaterorbBlock(FabricBlockSettings.copyOf(Blocks.LILY_PAD)
            .mapColor(MapColor.CYAN).luminance(state -> 4));
    public static final FaerieLichenBlock FAERIE_LICHEN = new FaerieLichenBlock(FabricBlockSettings.copyOf(
            Blocks.GLOW_LICHEN).mapColor(MapColor.PURPLE).luminance(GlowLichenBlock.getLuminanceSupplier(9)));
    public static final SporeDripBlock SPORE_DRIP = new SporeDripBlock(FabricBlockSettings.of(Material.PLANT,
            MapColor.LIGHT_BLUE).noCollision().breakInstantly().luminance(state -> 6)
            .sounds(BlockSoundGroup.SPORE_BLOSSOM));
    /** A clam shell bedded in the Darklake's floor; breaking it gives a Darklake Pearl. */
    public static final Block PEARL_CLAM = new Block(FabricBlockSettings.copyOf(Blocks.CLAY).mapColor(MapColor.GRAY)
            .strength(1.0F).sounds(BlockSoundGroup.MUD));

    public static final Item RIPPLEBARK_ITEM = new AliasedBlockItem(RIPPLEBARK, new FabricItemSettings()
            .food(new FoodComponent.Builder().hunger(3).saturationModifier(0.3F).build()));
    public static final Item TRILLIMAC_SLICE = new Item(new FabricItemSettings()
            .food(new FoodComponent.Builder().hunger(2).saturationModifier(0.1F).build()));
    public static final Item ROASTED_TRILLIMAC = new Item(new FabricItemSettings()
            .food(new FoodComponent.Builder().hunger(6).saturationModifier(0.6F).build()));
    /** Glowing spores from a Spore Drip: brewed into an Awkward Potion they make Night Vision. */
    public static final Item GLOW_SPORES = new Item(new FabricItemSettings());
    /** A black pearl from a Darklake Pearl-Clam: a trade good (the duergar buy it). */
    public static final Item DARKLAKE_PEARL = new Item(new FabricItemSettings());

    private UnderdarkFlora() {
    }

    private static FabricBlockSettings spores(MapColor color) {
        return FabricBlockSettings.of(Material.PLANT, color).noCollision().breakInstantly().ticksRandomly()
                .sounds(BlockSoundGroup.FUNGUS);
    }

    /** Every block, in creative-tab order, with its id path. */
    public static Map<String, Block> blocks() {
        Map<String, Block> blocks = new LinkedHashMap<>();
        blocks.put("zurkhwood_gills", ZURKHWOOD_GILLS);
        blocks.put("zurkhwood_spores", ZURKHWOOD_SPORES);
        blocks.put("nightlight_spores", NIGHTLIGHT_SPORES);
        blocks.put("ripplebark", RIPPLEBARK);
        blocks.put("barrelstalk", BARRELSTALK);
        blocks.put("waterorb", WATERORB);
        blocks.put("faerie_lichen", FAERIE_LICHEN);
        blocks.put("spore_drip", SPORE_DRIP);
        blocks.put("pearl_clam", PEARL_CLAM);
        return blocks;
    }

    /** Every item, in creative-tab order, with its id path (the blocks' items included). */
    private static Map<String, Item> items() {
        Map<String, Item> items = new LinkedHashMap<>();
        items.put("zurkhwood_gills", new BlockItem(ZURKHWOOD_GILLS, new FabricItemSettings()));
        items.put("zurkhwood_spores", new BlockItem(ZURKHWOOD_SPORES, new FabricItemSettings()));
        items.put("nightlight_spores", new BlockItem(NIGHTLIGHT_SPORES, new FabricItemSettings()));
        items.put("ripplebark", RIPPLEBARK_ITEM);
        items.put("barrelstalk", new TallBlockItem(BARRELSTALK, new FabricItemSettings()));
        items.put("waterorb", new PlaceableOnWaterItem(WATERORB, new FabricItemSettings()));
        items.put("faerie_lichen", new BlockItem(FAERIE_LICHEN, new FabricItemSettings()));
        items.put("spore_drip", new BlockItem(SPORE_DRIP, new FabricItemSettings()));
        items.put("pearl_clam", new BlockItem(PEARL_CLAM, new FabricItemSettings()));
        items.put("trillimac_slice", TRILLIMAC_SLICE);
        items.put("roasted_trillimac", ROASTED_TRILLIMAC);
        items.put("glow_spores", GLOW_SPORES);
        items.put("darklake_pearl", DARKLAKE_PEARL);
        return items;
    }

    /** Blocks the client draws cut out. */
    public static List<Block> cutoutBlocks() {
        return List.of(ZURKHWOOD_SPORES, NIGHTLIGHT_SPORES, RIPPLEBARK, BARRELSTALK, WATERORB, FAERIE_LICHEN,
                SPORE_DRIP);
    }

    public static void register() {
        blocks().forEach((name, block) -> Registry.register(Registries.BLOCK, id(name), block));
        Map<String, Item> items = items();
        items.forEach((name, item) -> Registry.register(Registries.ITEM, id(name), item));
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            for (Item item : items.values()) {
                entries.add(item);
            }
        });
        UnderdarkFeatures.register();
        // Glow Spores brew Night Vision instead of a golden carrot.
        FabricBrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, Ingredient.ofItems(GLOW_SPORES),
                Potions.NIGHT_VISION);
        CompostingChanceRegistry.INSTANCE.add(TRILLIMAC_SLICE, 0.5F);
        CompostingChanceRegistry.INSTANCE.add(RIPPLEBARK_ITEM, 0.5F);
        CompostingChanceRegistry.INSTANCE.add(ZURKHWOOD_GILLS, 0.65F);
    }

    private static Identifier id(String path) {
        return new Identifier(DnDClasses.MOD_ID, path);
    }
}
