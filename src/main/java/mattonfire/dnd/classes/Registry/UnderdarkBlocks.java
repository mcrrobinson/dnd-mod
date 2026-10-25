package mattonfire.dnd.classes.Registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.underdark.FaerzressStoneBlock;
import mattonfire.dnd.classes.Blocks.underdark.UnderdarkFungusBlock;
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
 * blocks).
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
    /** A small glowing blue mushroom (light 6), happy in any light. Bone meal grows it into a small nightlight tower. */
    public static final UnderdarkFungusBlock BLUECAP = new mattonfire.dnd.classes.Blocks.underdark.BluecapBlock(
            fungus(MapColor.BLUE).luminance(state -> 6));
    /** A small grey-tan mushroom. Breaking it gives Trillimac Slices (UnderdarkFlora). */
    public static final UnderdarkFungusBlock TRILLIMAC = new UnderdarkFungusBlock(fungus(MapColor.PALE_YELLOW));

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
    }
}
