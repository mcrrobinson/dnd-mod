package mattonfire.dnd.classes.Items;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
public class ModItemGroups {
    ItemGroup ITEM_GROUP = FabricItemGroup.builder(new Identifier(DnDClasses.MOD_ID, "pink_garnet_items"))
        .displayName(Text.translatable("itemgroup.dndclasses.pink_garnet_items"))
        .icon(() -> new ItemStack(ModItems.PINK_GARNET_BOOTS))
        .build();
        
    public static void registerItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(ModItems.PINK_GARNET_HELMET);
            entries.add(ModItems.PINK_GARNET_CHESTPLATE);
            entries.add(ModItems.PINK_GARNET_LEGGINGS);
            entries.add(ModItems.PINK_GARNET_BOOTS);
        });
}

}
