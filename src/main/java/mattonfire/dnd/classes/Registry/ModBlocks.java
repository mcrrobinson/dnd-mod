package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.ArcaneSealBlock;
import mattonfire.dnd.classes.Blocks.AttunementTableBlock;
import mattonfire.dnd.classes.Blocks.DungeonWardBlock;
import mattonfire.dnd.classes.Blocks.DungeonWardBlockEntity;
import mattonfire.dnd.classes.Blocks.FastBrewingStandBlock;
import mattonfire.dnd.classes.Blocks.HoardCofferBlock;
import mattonfire.dnd.classes.Blocks.HoardCofferBlockEntity;
import mattonfire.dnd.classes.Blocks.RuneBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
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

        /** Invisible, unbreakable marker under each dungeon room's floor (see DungeonWardBlockEntity). */
        public static final DungeonWardBlock DUNGEON_WARD = new DungeonWardBlock(
                        FabricBlockSettings.of(Material.BARRIER).strength(-1.0F, 3600000.0F).dropsNothing().noCollision()
                                        .allowsSpawning((state, world, pos, type) -> false));

        public static final BlockEntityType<DungeonWardBlockEntity> DUNGEON_WARD_ENTITY = FabricBlockEntityTypeBuilder
                        .create(DungeonWardBlockEntity::new, DUNGEON_WARD).build(null);

        /** A ward's doorway seal while a dungeon room's fight is on (see DungeonWardBlockEntity). Unbreakable. */
        public static final ArcaneSealBlock ARCANE_SEAL = new ArcaneSealBlock(
                        FabricBlockSettings.of(Material.GLASS).strength(-1.0F, 3600000.0F).dropsNothing().nonOpaque()
                                        .luminance(state -> 9).sounds(BlockSoundGroup.AMETHYST_BLOCK)
                                        .allowsSpawning((state, world, pos, type) -> false)
                                        .solidBlock((state, world, pos) -> false)
                                        .suffocates((state, world, pos) -> false)
                                        .blockVision((state, world, pos) -> false));

        /** A dungeon treasure vault's per-player loot coffer (see HoardCofferBlockEntity). Unbreakable. */
        public static final HoardCofferBlock HOARD_COFFER = new HoardCofferBlock(
                        FabricBlockSettings.of(Material.WOOD).strength(-1.0F, 3600000.0F).dropsNothing().nonOpaque()
                                        .luminance(state -> 4).sounds(BlockSoundGroup.WOOD));

        public static final BlockEntityType<HoardCofferBlockEntity> HOARD_COFFER_ENTITY = FabricBlockEntityTypeBuilder
                        .create(HoardCofferBlockEntity::new, HOARD_COFFER).build(null);

        /** A rune pillar's turning face in a dungeon puzzle room (see RuneBlock). Unbreakable. */
        public static final RuneBlock RUNE_BLOCK = new RuneBlock(
                        FabricBlockSettings.of(Material.STONE).strength(-1.0F, 3600000.0F).dropsNothing()
                                        .luminance(state -> 5).sounds(BlockSoundGroup.DEEPSLATE_BRICKS)
                                        .allowsSpawning((state, world, pos, type) -> false));

        public static void registerBlocks() {
                Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, "arcane_seal"), ARCANE_SEAL);
                Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, "dungeon_ward"), DUNGEON_WARD);
                Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(DnDClasses.MOD_ID, "dungeon_ward"),
                                DUNGEON_WARD_ENTITY);
                Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, "attunement_table"),
                                ATTUNEMENT_TABLE);
                Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, "attunement_table"),
                                new BlockItem(ATTUNEMENT_TABLE, new FabricItemSettings()));
                Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, "fast_brewing_stand"),
                                FAST_BREWING_STAND_BLOCK);
                Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, "fast_brewing_stand"),
                                new BlockItem(FAST_BREWING_STAND_BLOCK, new FabricItemSettings().maxCount(64)));
                Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, "hoard_coffer"), HOARD_COFFER);
                Registry.register(Registries.BLOCK, new Identifier(DnDClasses.MOD_ID, "rune_block"), RUNE_BLOCK);
                Registry.register(Registries.ITEM, new Identifier(DnDClasses.MOD_ID, "rune_block"),
                                new BlockItem(RUNE_BLOCK, new FabricItemSettings()));
                Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(DnDClasses.MOD_ID, "hoard_coffer"),
                                HOARD_COFFER_ENTITY);
        }
}
