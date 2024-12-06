package mattonfire.dnd.classes;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroup {
    public static ItemGroup DND_CLASSES_ITEMGROUP = FabricItemGroup
            .builder(new Identifier(DnDClasses.MOD_ID, "dndclasses"))
            .displayName(Text.translatable("itemgroup.dndclasses"))
            .icon(() -> new ItemStack(ModItems.MONK_STAFF)).build();

    public static void registerItemGroups() {
        // Example of adding to existing Item Group
        ItemGroupEvents.modifyEntriesEvent(DND_CLASSES_ITEMGROUP).register(entries -> {
            entries.add(ModItems.STAFF_OF_ICE);
            entries.add(ModItems.STAFF_OF_FIRE);
            entries.add(ModItems.STAFF_OF_LIGHTNING);
            entries.add(ModItems.MONK_STAFF);
            entries.add(ModItems.MUSIC_DISC_STEEL_ON_STEEL);
            entries.add(ModItems.MUSIC_DISC_AWAKE_CART);
            entries.add(ModItems.MUSIC_DISC_TOOTH_AND_CLAW);
            entries.add(ModItems.MUSIC_DISC_SILENT_FOOTSTEPS);
        });
    }
}