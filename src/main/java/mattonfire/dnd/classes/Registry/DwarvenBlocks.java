package mattonfire.dnd.classes.Registry;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.dwarven.CarvedFaceBlock;
import mattonfire.dnd.classes.Blocks.dwarven.CoinPileBlock;
import mattonfire.dnd.classes.Blocks.dwarven.RunicColumnBlock;
import mattonfire.dnd.classes.Blocks.dwarven.RunicCorbelBlock;
import mattonfire.dnd.classes.Blocks.dwarven.VerticalSlabBlock;
import mattonfire.dnd.classes.Blocks.dwarven.WaystoneBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.PaneBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The Dwarven Fortresses card's one-off building blocks (the materials with shapes are in
 * {@link ModBlockFamilies}). Listed in the DnD Building Blocks tab after the families. Resources come from
 * {@code tools/dwarven_blocks.py}, textures from {@code tools/dwarven_block_textures.py}.
 */
public final class DwarvenBlocks {
    /** A gold hammer-over-anvil sigil in a hexagon, over fortress gates, thrones and doorways. */
    public static final Block CLAN_CREST = new CarvedFaceBlock(FabricBlockSettings.copyOf(Blocks.CHISELED_DEEPSLATE));
    /** A carved, stern bearded dwarf face, set into the great hall's walls. */
    public static final Block ANCESTOR_STATUE = new CarvedFaceBlock(FabricBlockSettings.copyOf(Blocks.POLISHED_DEEPSLATE));
    /** Three keg ends stacked in a frame: one block reads as a wall of kegs. */
    public static final Block ALE_KEG_WALL = new CarvedFaceBlock(FabricBlockSettings.copyOf(Blocks.BARREL));
    /** Layers of coins like snow; drops gold nuggets. */
    public static final Block TREASURY_COIN_PILE = new CoinPileBlock(FabricBlockSettings.of(Material.METAL, MapColor.GOLD)
            .strength(0.6F).sounds(BlockSoundGroup.CHAIN));
    /** Pale silver bars in a diamond lattice: the grilles of the treasury and throne room. */
    public static final Block MITHRAL_LATTICE = new PaneBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS)
            .mapColor(MapColor.WHITE_GRAY));
    /** A two-block standing stone that names the hold and your standing with the dwarves. */
    public static final Block WAYSTONE = new WaystoneBlock(FabricBlockSettings.copyOf(Blocks.STONE)
            .luminance(state -> 5).nonOpaque());
    /** A slender chamfered column with a gold rune band; stacks get a stepped base and a capital. */
    public static final Block RUNIC_COLUMN = new RunicColumnBlock(FabricBlockSettings.copyOf(Blocks.DEEPSLATE_BRICKS));
    /** A stepped bracket jutting out of a wall, under ledges, cornices and lamps. */
    public static final Block RUNIC_CORBEL = new RunicCorbelBlock(FabricBlockSettings.copyOf(Blocks.DEEPSLATE_BRICKS));
    /** Half a runic brick on its edge, for pilasters and wall trim. */
    public static final Block RUNIC_DEEPSLATE_BRICK_VERTICAL_SLAB = new VerticalSlabBlock(
            FabricBlockSettings.copyOf(Blocks.DEEPSLATE_BRICKS));

    private DwarvenBlocks() {
    }

    /** The blocks in tab order. */
    public static List<Block> all() {
        return List.of(CLAN_CREST, ANCESTOR_STATUE, ALE_KEG_WALL, TREASURY_COIN_PILE, MITHRAL_LATTICE, WAYSTONE, RUNIC_COLUMN,
                RUNIC_CORBEL, RUNIC_DEEPSLATE_BRICK_VERTICAL_SLAB);
    }

    public static void register() {
        register("clan_crest", CLAN_CREST);
        register("ancestor_statue", ANCESTOR_STATUE);
        register("ale_keg_wall", ALE_KEG_WALL);
        register("treasury_coin_pile", TREASURY_COIN_PILE);
        register("mithral_lattice", MITHRAL_LATTICE);
        register("waystone", WAYSTONE);
        register("runic_column", RUNIC_COLUMN);
        register("runic_corbel", RUNIC_CORBEL);
        register("runic_deepslate_brick_vertical_slab", RUNIC_DEEPSLATE_BRICK_VERTICAL_SLAB);
    }

    private static void register(String name, Block block) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
    }
}
