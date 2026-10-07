package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.AttunementTableBlock;
import mattonfire.dnd.classes.Blocks.FastBrewingStandBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
        public static final FastBrewingStandBlock FAST_BREWING_STAND_BLOCK = new FastBrewingStandBlock(
                        FabricBlockSettings.of(Material.METAL).strength(0.5F));

        public static final AttunementTableBlock ATTUNEMENT_TABLE = new AttunementTableBlock(
                        FabricBlockSettings.of(Material.WOOD).strength(2.5F).luminance(state -> 7)
                                        .sounds(BlockSoundGroup.AMETHYST_BLOCK));

        public static void registerBlocks() {
                Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, "attunement_table"),
                                ATTUNEMENT_TABLE);
                Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, "attunement_table"),
                                new BlockItem(ATTUNEMENT_TABLE, new FabricItemSettings()));
                Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, "fast_brewing_stand"),
                                FAST_BREWING_STAND_BLOCK);
                Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, "fast_brewing_stand"),
                                new BlockItem(FAST_BREWING_STAND_BLOCK, new FabricItemSettings().maxCount(64)));
        }
}
