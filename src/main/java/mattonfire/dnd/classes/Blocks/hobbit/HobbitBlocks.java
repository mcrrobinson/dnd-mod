package mattonfire.dnd.classes.Blocks.hobbit;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The hobbit villages' one-off and interactive building blocks (Building Blocks epic, Hobbit Villages card): the
 * Larder Shelf, the Ale Cask and the hanging herbs and onions. The materials with shapes (thatch, wattle and daub,
 * ...) are block families in {@link ModBlockFamilies}. Resources: {@code tools/hobbit_blocks.py} and
 * {@code tools/hobbit_block_textures.py}.
 */
public final class HobbitBlocks {
    public static final LarderShelfBlock LARDER_SHELF = new LarderShelfBlock(
            FabricBlockSettings.copyOf(Blocks.BARREL).mapColor(MapColor.BROWN));
    public static final BlockEntityType<LarderShelfBlockEntity> LARDER_SHELF_ENTITY = FabricBlockEntityTypeBuilder
            .create(LarderShelfBlockEntity::new, LARDER_SHELF).build(null);
    public static final AleCaskBlock ALE_CASK = new AleCaskBlock(FabricBlockSettings.copyOf(Blocks.BARREL));
    public static final HangingBundleBlock HANGING_HERBS = bundle(MapColor.GREEN);
    public static final HangingBundleBlock ONION_STRING = bundle(MapColor.PALE_YELLOW);

    private HobbitBlocks() {
    }

    private static HangingBundleBlock bundle(MapColor color) {
        return new HangingBundleBlock(FabricBlockSettings.of(Material.PLANT, color).noCollision().breakInstantly()
                .sounds(BlockSoundGroup.GRASS).nonOpaque());
    }

    public static void register() {
        block("larder_shelf", LARDER_SHELF);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(DnDClasses.MOD_ID, "larder_shelf"),
                LARDER_SHELF_ENTITY);
        block("ale_cask", ALE_CASK);
        block("hanging_herbs", HANGING_HERBS);
        block("onion_string", ONION_STRING);

        // Straw and dried leaves burn like hay.
        FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
        for (Block block : new Block[] { ModBlockFamilies.THATCH.base(), ModBlockFamilies.THATCH.stairs(),
                ModBlockFamilies.THATCH.slab(), ModBlockFamilies.MOSSY_THATCH.base(),
                ModBlockFamilies.MOSSY_THATCH.stairs(), ModBlockFamilies.MOSSY_THATCH.slab(),
                ModBlockFamilies.PIPEWEED_BUNDLE.base() }) {
            flammable.add(block, 60, 20);
        }
        flammable.add(HANGING_HERBS, 60, 100);
        flammable.add(ONION_STRING, 60, 100);

        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.BUILDING_BLOCKS_ITEMGROUP).register(entries -> {
            entries.add(LARDER_SHELF);
            entries.add(ALE_CASK);
            entries.add(HANGING_HERBS);
            entries.add(ONION_STRING);
        });
    }

    private static void block(String name, Block block) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
    }
}
