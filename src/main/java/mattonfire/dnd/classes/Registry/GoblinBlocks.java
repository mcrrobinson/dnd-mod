package mattonfire.dnd.classes.Registry;

import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.JunkPileBlock;
import mattonfire.dnd.classes.Blocks.WarOrderBoardBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The goblin camps' and Goblin Warrens' one-off building blocks (Building Blocks epic); their material,
 * Scrap-lashed Planks, is a block family in {@link ModBlockFamilies}. Listed in the DnD Building Blocks tab
 * after the families. Resources are hand-written, textures from {@code tools/goblin_block_textures.py}. See
 * docs/structures/goblin-camps.md.
 */
public final class GoblinBlocks {
    /** Hide and cloth scraps sewn together: the goblins' tents and bedding. Wool-like. */
    public static final Block PATCHWORK_HIDE = new Block(FabricBlockSettings.copyOf(Blocks.BROWN_WOOL));

    public static final CarpetBlock PATCHWORK_HIDE_CARPET = new CarpetBlock(
            FabricBlockSettings.copyOf(Blocks.BROWN_CARPET));

    /** A heap of broken weapons, pots, bones and a helmet; mining it drops a junk roll (silk touch: itself). */
    public static final JunkPileBlock GOBLIN_JUNK_PILE = new JunkPileBlock(FabricBlockSettings.copyOf(Blocks.GRAVEL)
            .strength(0.8F).sounds(BlockSoundGroup.BONE).nonOpaque());

    /** Cobblestone smeared with a goblin war mark (one of four, picked by position). */
    public static final Block CRUDE_GLYPH_DAUB = new Block(FabricBlockSettings.copyOf(Blocks.COBBLESTONE));

    /** A spruce log with the same war marks: the camps' palisade posts. */
    public static final PillarBlock GLYPH_DAUBED_SPRUCE_LOG = new PillarBlock(
            FabricBlockSettings.copyOf(Blocks.SPRUCE_LOG));

    /** Spruce posts lashed with rope rails and topped with a small skull: the goblin palisade. */
    public static final FenceBlock ROPE_AND_BONE_FENCE = new FenceBlock(FabricBlockSettings.copyOf(Blocks.SPRUCE_FENCE));

    /** Where Goblin War Orders are pinned up; right-click reads today's orders. */
    public static final WarOrderBoardBlock WAR_ORDER_BOARD = new WarOrderBoardBlock(
            FabricBlockSettings.copyOf(Blocks.SPRUCE_PLANKS).strength(1.0F).nonOpaque());

    private GoblinBlocks() {
    }

    /** In creative-tab order. */
    public static List<Block> all() {
        return List.of(PATCHWORK_HIDE, PATCHWORK_HIDE_CARPET, GOBLIN_JUNK_PILE, CRUDE_GLYPH_DAUB,
                GLYPH_DAUBED_SPRUCE_LOG, ROPE_AND_BONE_FENCE, WAR_ORDER_BOARD);
    }

    public static void register() {
        register("patchwork_hide", PATCHWORK_HIDE);
        register("patchwork_hide_carpet", PATCHWORK_HIDE_CARPET);
        register("goblin_junk_pile", GOBLIN_JUNK_PILE);
        register("crude_glyph_daub", CRUDE_GLYPH_DAUB);
        register("glyph_daubed_spruce_log", GLYPH_DAUBED_SPRUCE_LOG);
        register("rope_and_bone_fence", ROPE_AND_BONE_FENCE);
        register("war_order_board", WAR_ORDER_BOARD);

        // Burn like the vanilla blocks they stand in for (wool, carpet, logs, fences, planks).
        FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
        flammable.add(PATCHWORK_HIDE, 30, 60);
        flammable.add(PATCHWORK_HIDE_CARPET, 60, 20);
        flammable.add(GLYPH_DAUBED_SPRUCE_LOG, 5, 5);
        flammable.add(ROPE_AND_BONE_FENCE, 5, 20);
        flammable.add(WAR_ORDER_BOARD, 5, 20);
        for (Block block : ModBlockFamilies.SCRAP_LASHED_PLANKS.blocks()) {
            flammable.add(block, 5, 20);
        }
    }

    private static void register(String name, Block block) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
    }
}
