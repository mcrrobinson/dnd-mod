package mattonfire.dnd.classes.Registry;

import java.util.LinkedHashMap;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.BlueprintTableBlock;
import mattonfire.dnd.classes.Blocks.ClockfaceBlock;
import mattonfire.dnd.classes.Blocks.PipeworkBlock;
import mattonfire.dnd.classes.Blocks.TelescopeLensBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Cogsdeep's one-off building blocks (the materials with shapes, Riveted Brass and Gnomish Bricks, are families in
 * {@link ModBlockFamilies}). Resources: {@code tools/cogsdeep_blocks.py}; textures:
 * {@code tools/cogsdeep_block_textures.py}. See docs/structures/cogsdeep-blocks.md.
 */
public final class CogsdeepBlocks {
    /** A brass panel with two meshing cogs turning behind a cut-out (animated). */
    public static final Block COGWORK_PANEL = new Block(brass());
    /** One big gear filling the face, turning slowly (animated); rows of them make a mechanism. */
    public static final Block GEARWALL = new Block(brass());
    /** A clock face whose hour hand shows the in-game time. */
    public static final ClockfaceBlock CLOCKFACE = new ClockfaceBlock(brass());
    /** Copper pipes through brass joints, joining up like a fence. */
    public static final PipeworkBlock PIPEWORK = new PipeworkBlock(brass().strength(2.0F, 6.0F).nonOpaque());
    /** The end of a brass telescope tube with its glass lens. */
    public static final TelescopeLensBlock TELESCOPE_LENS = new TelescopeLensBlock(brass().strength(2.0F, 6.0F).nonOpaque());
    /** A drafting table with a blueprint pinned on top: a crafting grid for now (see BlueprintTableBlock). */
    public static final BlueprintTableBlock BLUEPRINT_TABLE = new BlueprintTableBlock(
            FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).mapColor(MapColor.BLUE));

    /** Faceted gems in brass settings, light 12, by colour. */
    public static final Map<String, Block> GLOWCRYSTALS = new LinkedHashMap<>();

    static {
        GLOWCRYSTALS.put("red", glowcrystal(MapColor.RED));
        GLOWCRYSTALS.put("blue", glowcrystal(MapColor.BLUE));
        GLOWCRYSTALS.put("green", glowcrystal(MapColor.GREEN));
        GLOWCRYSTALS.put("amber", glowcrystal(MapColor.ORANGE));
    }

    private CogsdeepBlocks() {
    }

    /** Brass: copper's hardness, sound and tool, the colour of gold. */
    static FabricBlockSettings brass() {
        return FabricBlockSettings.copyOf(Blocks.CUT_COPPER).mapColor(MapColor.GOLD);
    }

    private static Block glowcrystal(MapColor color) {
        return new Block(FabricBlockSettings.copyOf(Blocks.AMETHYST_BLOCK).mapColor(color).luminance(state -> 12));
    }

    public static Block glowcrystal(String colour) {
        return GLOWCRYSTALS.get(colour);
    }

    /** Every block, in creative-tab order, with its id path. */
    public static Map<String, Block> all() {
        Map<String, Block> blocks = new LinkedHashMap<>();
        blocks.put("cogwork_panel", COGWORK_PANEL);
        blocks.put("gearwall", GEARWALL);
        blocks.put("pipework", PIPEWORK);
        GLOWCRYSTALS.forEach((colour, block) -> blocks.put("glowcrystal_" + colour, block));
        blocks.put("clockface", CLOCKFACE);
        blocks.put("blueprint_table", BLUEPRINT_TABLE);
        blocks.put("telescope_lens", TELESCOPE_LENS);
        return blocks;
    }

    public static void register() {
        all().forEach((name, block) -> {
            Identifier id = new Identifier(DnDClasses.MOD_ID, name);
            Registry.register(Registries.BLOCK, id, block);
            Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
        });
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.BUILDING_BLOCKS_ITEMGROUP).register(entries -> {
            for (Block block : all().values()) {
                entries.add(block);
            }
        });
    }
}
