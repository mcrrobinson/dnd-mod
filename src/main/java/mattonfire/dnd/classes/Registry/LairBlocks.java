package mattonfire.dnd.classes.Registry;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.lair.FulguriteSpikeBlock;
import mattonfire.dnd.classes.Blocks.lair.ScalePileBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.PaneBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The Dragon &amp; Frost lairs card's one-off building blocks and the dragon scales their scale piles drop (the
 * materials with shapes, Fulgurite, Rimestone and Dragon Nest, are families in {@link ModBlockFamilies}).
 * Resources and textures come from {@code tools/lair_block_textures.py}. The blocks are listed in the DnD Building
 * Blocks tab after the families, the scales in the main DnD tab.
 */
public final class LairBlocks {
    /** A branching glassy tube of fused sand where lightning struck, standing on the storm lair's floor. */
    public static final Block FULGURITE_SPIKE = new FulguriteSpikeBlock(FabricBlockSettings.copyOf(Blocks.SMOOTH_BASALT)
            .strength(0.8F).nonOpaque().noCollision());
    /** Dark blue-grey glass with flickering white cracks of lightning (an animated texture). */
    public static final Block STORM_GLASS = new GlassBlock(FabricBlockSettings.copyOf(Blocks.GLASS)
            .mapColor(MapColor.GRAY).luminance(state -> 2));
    public static final Block STORM_GLASS_PANE = new PaneBlock(FabricBlockSettings.copyOf(Blocks.GLASS_PANE)
            .mapColor(MapColor.GRAY).luminance(state -> 2));
    /** Pale blue-white ice crystals that grow like amethyst, faintly glowing (light 3). */
    public static final Block HOARFROST_CRYSTAL = new AmethystClusterBlock(7, 3, FabricBlockSettings
            .copyOf(Blocks.AMETHYST_CLUSTER).mapColor(MapColor.LIGHT_BLUE_GRAY).sounds(BlockSoundGroup.GLASS)
            .luminance(state -> 3));
    /** Layers of the Lightning Chaser's shed scales, yellow and blue; drop Storm Dragon Scales. */
    public static final Block STORM_SCALE_PILE = scalePile(MapColor.YELLOW);
    /** Layers of the Frost Drake's shed scales, pale blue; drop Frost Dragon Scales. */
    public static final Block FROST_SCALE_PILE = scalePile(MapColor.LIGHT_BLUE);

    /** Shed scales from the scale piles: a crafting material (Storm Glass, Hoarfrost Crystals, the piles). */
    public static final Item STORM_DRAGON_SCALE = new Item(new FabricItemSettings());
    public static final Item FROST_DRAGON_SCALE = new Item(new FabricItemSettings());

    private static final List<Item> ITEMS = new ArrayList<>();

    private LairBlocks() {
    }

    private static Block scalePile(MapColor color) {
        return new ScalePileBlock(FabricBlockSettings.of(Material.METAL, color).strength(0.5F)
                .sounds(BlockSoundGroup.CHAIN));
    }

    public static void register() {
        register("fulgurite_spike", FULGURITE_SPIKE);
        register("storm_glass", STORM_GLASS);
        register("storm_glass_pane", STORM_GLASS_PANE);
        register("hoarfrost_crystal", HOARFROST_CRYSTAL);
        register("storm_scale_pile", STORM_SCALE_PILE);
        register("frost_scale_pile", FROST_SCALE_PILE);
        Registry.register(Registries.ITEM, id("storm_dragon_scale"), STORM_DRAGON_SCALE);
        Registry.register(Registries.ITEM, id("frost_dragon_scale"), FROST_DRAGON_SCALE);

        FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
        fire.add(ModBlockFamilies.DRAGON_NEST.base(), 5, 20);
        fire.add(ModBlockFamilies.DRAGON_NEST.slab(), 5, 20);
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.BUILDING_BLOCKS_ITEMGROUP).register(entries -> ITEMS.forEach(entries::add));
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            entries.add(STORM_DRAGON_SCALE);
            entries.add(FROST_DRAGON_SCALE);
        });
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    private static void register(String name, Block block) {
        Registry.register(Registries.BLOCK, id(name), block);
        Item item = new BlockItem(block, new FabricItemSettings());
        Registry.register(Registries.ITEM, id(name), item);
        ITEMS.add(item);
    }

    /** Blocks the client draws cut out (with see-through pixels). */
    public static List<Block> cutoutBlocks() {
        return List.of(FULGURITE_SPIKE, HOARFROST_CRYSTAL);
    }

    /** Blocks the client draws translucent (tinted glass). */
    public static List<Block> translucentBlocks() {
        return List.of(STORM_GLASS, STORM_GLASS_PANE);
    }
}
