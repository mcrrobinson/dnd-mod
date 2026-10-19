package mattonfire.dnd.classes.Registry;

import java.util.EnumMap;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.CarpetBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

/**
 * Blocks of the desolate settlements ({@link mattonfire.dnd.world.gen.Desolation}): a Tattered Carpet for each dye
 * colour, worn, stained and torn through, which the settlements' carpets turn into. Resources and textures from
 * {@code tools/tattered_carpet_textures.py}; drawn on the cutout layer so the floor shows through the holes.
 */
public final class DesolateBlocks {
    private static final Map<DyeColor, CarpetBlock> TATTERED_CARPETS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            Block carpet = Registries.BLOCK.get(new Identifier(color.getName() + "_carpet"));
            TATTERED_CARPETS.put(color, new CarpetBlock(FabricBlockSettings.copyOf(carpet).nonOpaque()));
        }
    }

    private DesolateBlocks() {
    }

    public static CarpetBlock tatteredCarpet(DyeColor color) {
        return TATTERED_CARPETS.get(color);
    }

    public static Iterable<CarpetBlock> tatteredCarpets() {
        return TATTERED_CARPETS.values();
    }

    public static void register() {
        FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
        for (Map.Entry<DyeColor, CarpetBlock> entry : TATTERED_CARPETS.entrySet()) {
            Identifier id = new Identifier(DnDClasses.MOD_ID, "tattered_" + entry.getKey().getName() + "_carpet");
            Registry.register(Registries.BLOCK, id, entry.getValue());
            Registry.register(Registries.ITEM, id, new BlockItem(entry.getValue(), new FabricItemSettings()));
            flammable.add(entry.getValue(), 60, 20);
        }
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.BUILDING_BLOCKS_ITEMGROUP).register(entries -> {
            for (CarpetBlock carpet : TATTERED_CARPETS.values()) {
                entries.add(carpet);
            }
        });
    }
}
