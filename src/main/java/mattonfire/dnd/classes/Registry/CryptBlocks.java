package mattonfire.dnd.classes.Registry;

import java.util.List;
import mattonfire.dnd.classes.Blocks.CarvedFacingBlock;
import mattonfire.dnd.classes.Blocks.GraveCandleClusterBlock;
import mattonfire.dnd.classes.Blocks.PhylacteryPedestalBlock;
import mattonfire.dnd.classes.Blocks.SarcophagusBlock;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarpetBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * The Crypt's one-off building blocks (Building Blocks: Crypt dungeons). Ossuary Bricks, the one material
 * with shapes, is a family in {@link ModBlockFamilies}. Resources: {@code tools/crypt_blocks.py}
 * (models, loot, tags, lang) and {@code tools/crypt_block_textures.py}; see docs/items/crypt-blocks.md.
 */
public final class CryptBlocks {
    /** A carved stone panel with a shrouded figure, set into crypt walls. */
    public static final CarvedFacingBlock FUNERARY_RELIEF = new CarvedFacingBlock(
            FabricBlockSettings.copyOf(Blocks.CHISELED_STONE_BRICKS));
    /** A wall block with a recess holding a wrapped body (not opaque, so the recess is lit). */
    public static final CarvedFacingBlock BURIAL_NICHE_WALL = new CarvedFacingBlock(
            FabricBlockSettings.copyOf(Blocks.STONE_BRICKS).nonOpaque());
    public static final SarcophagusBlock SARCOPHAGUS = new SarcophagusBlock(
            FabricBlockSettings.copyOf(Blocks.POLISHED_DEEPSLATE).nonOpaque());
    public static final GraveCandleClusterBlock GRAVE_CANDLE_CLUSTER = new GraveCandleClusterBlock(
            FabricBlockSettings.copyOf(Blocks.CANDLE).luminance(GraveCandleClusterBlock.LIGHT).nonOpaque());
    /** Dark stone with a ring of green necrotic runes, faintly glowing (light 4). */
    public static final Block LICH_SIGIL_FLOOR = new Block(
            FabricBlockSettings.copyOf(Blocks.POLISHED_DEEPSLATE).luminance(4));
    public static final Block GRAVE_MOSS = new Block(FabricBlockSettings.copyOf(Blocks.MOSS_BLOCK));
    public static final CarpetBlock GRAVE_MOSS_CARPET = new CarpetBlock(FabricBlockSettings.copyOf(Blocks.MOSS_CARPET));
    public static final PhylacteryPedestalBlock PHYLACTERY_PEDESTAL = new PhylacteryPedestalBlock(
            FabricBlockSettings.copyOf(Blocks.POLISHED_BLACKSTONE).luminance(PhylacteryPedestalBlock.LIGHT).nonOpaque());

    private static final List<Block> ALL = List.of(FUNERARY_RELIEF, BURIAL_NICHE_WALL, SARCOPHAGUS,
            GRAVE_CANDLE_CLUSTER, LICH_SIGIL_FLOOR, GRAVE_MOSS, GRAVE_MOSS_CARPET, PHYLACTERY_PEDESTAL);
    private static final List<String> IDS = List.of("funerary_relief", "burial_niche_wall", "sarcophagus",
            "grave_candle_cluster", "lich_sigil_floor", "grave_moss", "grave_moss_carpet", "phylactery_pedestal");

    private CryptBlocks() {
    }

    /** The blocks in creative-tab order. */
    public static List<Block> all() {
        return ALL;
    }

    /** The blocks the client draws on the cutout layer (the candle cluster's wax puddle). */
    public static List<Block> cutoutBlocks() {
        return List.of(GRAVE_CANDLE_CLUSTER);
    }

    public static void register() {
        for (int i = 0; i < ALL.size(); i++) {
            Identifier id = new Identifier(DnDClasses.MOD_ID, IDS.get(i));
            Registry.register(Registries.BLOCK, id, ALL.get(i));
            Registry.register(Registries.ITEM, id, new BlockItem(ALL.get(i), new FabricItemSettings()));
        }
    }
}
