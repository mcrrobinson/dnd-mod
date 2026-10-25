package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.underdark.RiftAnchorBlock;
import mattonfire.dnd.classes.Blocks.underdark.RiftVeilBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The Underdark Rifts' two blocks (design 9 T2; see docs/structures/underdark-rifts.md): the Rift Veil pool you fall
 * through and the Rift Anchor column that pulls you back up. Both are unbreakable, so they're creative-only items.
 * Kept apart from {@link UnderdarkBlocks} so the rifts ticket and the flora/building-block tickets don't collide.
 * Resources: {@code tools/rift_blocks.py}; textures: {@code tools/rift_block_textures.py}.
 */
public final class RiftBlocks {
    public static final RiftVeilBlock RIFT_VEIL = new RiftVeilBlock(FabricBlockSettings.of(Material.PORTAL, MapColor.BLACK)
            .noCollision().strength(-1.0F, 3600000.0F).dropsNothing().luminance(state -> 8)
            .sounds(BlockSoundGroup.AMETHYST_BLOCK));
    public static final RiftAnchorBlock RIFT_ANCHOR = new RiftAnchorBlock(FabricBlockSettings.copyOf(Blocks.CRYING_OBSIDIAN)
            .mapColor(MapColor.PURPLE).strength(-1.0F, 3600000.0F).dropsNothing().luminance(state -> 10));

    private RiftBlocks() {
    }

    public static void register() {
        register("rift_veil", RIFT_VEIL);
        register("rift_anchor", RIFT_ANCHOR);
        // Mobs treat the veil like a damaging block: they won't walk into it, and keep off its edge if they can.
        LandPathNodeTypesRegistry.register(RIFT_VEIL, PathNodeType.DAMAGE_OTHER, PathNodeType.DANGER_OTHER);
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            entries.add(RIFT_VEIL);
            entries.add(RIFT_ANCHOR);
        });
    }

    private static void register(String name, net.minecraft.block.Block block) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
    }
}
