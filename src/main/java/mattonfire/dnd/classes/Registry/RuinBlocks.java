package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.Blocks.EyeStalkBlock;
import mattonfire.dnd.classes.Blocks.PetrifiedRemainsBlock;
import mattonfire.dnd.classes.Blocks.RubbleBlock;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The Dwarven Ruin's and the Beholder lair's one-off building blocks (the materials with shapes are families in
 * {@link ModBlockFamilies}). Listed in the DnD Building Blocks tab after the families.
 */
public final class RuinBlocks {
    /** Broken stone and dust, 1 to 8 layers deep. */
    public static final Block COLLAPSED_RUBBLE = new RubbleBlock(FabricBlockSettings.of(Material.AGGREGATE, MapColor.DEEPSLATE_GRAY)
            .strength(0.6F).sounds(BlockSoundGroup.GRAVEL));
    /** An adventurer turned to stone by a beholder's gaze. */
    public static final Block PETRIFIED_REMAINS = new PetrifiedRemainsBlock(FabricBlockSettings.copyOf(Blocks.STONE).nonOpaque());
    /** Fleshy stalks with closed eyes, growing near a beholder. */
    public static final Block EYE_STALK_GROWTH = new EyeStalkBlock(FabricBlockSettings.of(Material.PLANT, MapColor.PINK)
            .noCollision().nonOpaque().strength(0.2F).sounds(BlockSoundGroup.WART_BLOCK));

    private RuinBlocks() {
    }

    public static void register() {
        register("collapsed_rubble", COLLAPSED_RUBBLE);
        register("petrified_remains", PETRIFIED_REMAINS);
        register("eye_stalk_growth", EYE_STALK_GROWTH);
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.BUILDING_BLOCKS_ITEMGROUP).register(entries -> {
            entries.add(COLLAPSED_RUBBLE);
            entries.add(PETRIFIED_REMAINS);
            entries.add(EYE_STALK_GROWTH);
        });
    }

    private static void register(String name, Block block) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
    }
}
