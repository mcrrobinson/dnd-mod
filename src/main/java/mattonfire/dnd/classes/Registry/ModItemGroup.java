package mattonfire.dnd.classes.Registry;

import mattonfire.dnd.classes.DnDClasses;
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
            entries.add(ModItems.CLASS_GUIDEBOOK);
            entries.add(ModItems.STAFF_OF_ICE);
            entries.add(ModItems.STAFF_OF_FIRE);
            entries.add(ModItems.STAFF_OF_LIGHTNING);
            entries.add(ModItems.MONK_STAFF);
            entries.add(ModItems.MUSIC_DISC_STEEL_ON_STEEL);
            entries.add(ModItems.MUSIC_DISC_AWAKE_CART);
            entries.add(ModItems.MUSIC_DISC_TOOTH_AND_CLAW);
            entries.add(ModItems.MUSIC_DISC_SILENT_FOOTSTEPS);
            entries.add(ModItems.ASSASSIN_BOOTS);
            entries.add(ModItems.ASSASSIN_CHESTPLATE);
            entries.add(ModItems.ASSASSIN_HELMET);
            entries.add(ModItems.ASSASSIN_LEGGINGS);
            entries.add(ModItems.ROGUE_HELMET);
            entries.add(ModItems.ROGUE_CHESTPLATE);
            entries.add(ModItems.ROGUE_LEGGINGS);
            entries.add(ModItems.ROGUE_BOOTS);
            entries.add(ModItems.WIZARD_HELMET);
            entries.add(ModItems.WIZARD_CHESTPLATE);
            entries.add(ModItems.WIZARD_LEGGINGS);
            entries.add(ModItems.WIZARD_BOOTS);
            entries.add(ModItems.CLERIC_HELMET);
            entries.add(ModItems.CLERIC_CHESTPLATE);
            entries.add(ModItems.CLERIC_LEGGINGS);
            entries.add(ModItems.CLERIC_BOOTS);
            entries.add(ModItems.BLOOD_HUNTER_HELMET);
            entries.add(ModItems.BLOOD_HUNTER_CHESTPLATE);
            entries.add(ModItems.BLOOD_HUNTER_LEGGINGS);
            entries.add(ModItems.BLOOD_HUNTER_BOOTS);
            entries.add(ModItems.ASSASSIN_HELMET);
            entries.add(ModItems.ASSASSIN_CHESTPLATE);
            entries.add(ModItems.ASSASSIN_LEGGINGS);
            entries.add(ModItems.ASSASSIN_BOOTS);
            entries.add(ModItems.GOLDEN_HORNS_HELMET);
            entries.add(ModItems.GOLDEN_HORNS_CHESTPLATE);
            entries.add(ModItems.GOLDEN_HORNS_LEGGINGS);
            entries.add(ModItems.GOLDEN_HORNS_BOOTS);
            entries.add(ModItems.HOLY_ARMOR_HELMET);
            entries.add(ModItems.HOLY_ARMOR_CHESTPLATE);
            entries.add(ModItems.HOLY_ARMOR_LEGGINGS);
            entries.add(ModItems.HOLY_ARMOR_BOOTS);
            entries.add(ModItems.KNIGHT_HELMET);
            entries.add(ModItems.KNIGHT_CHESTPLATE);
            entries.add(ModItems.KNIGHT_LEGGINGS);
            entries.add(ModItems.KNIGHT_BOOTS);
            entries.add(ModItems.PRISMARINE_HELMET);
            entries.add(ModItems.PRISMARINE_CHESTPLATE);
            entries.add(ModItems.PRISMARINE_LEGGINGS);
            entries.add(ModItems.PRISMARINE_BOOTS);
            entries.add(ModItems.ROBE_HELMET);
            entries.add(ModItems.ROBE_CHESTPLATE);
            entries.add(ModItems.ROBE_LEGGINGS);
            entries.add(ModItems.ROBE_BOOTS);
            entries.add(ModItems.STEAMPUNK_HELMET);
            entries.add(ModItems.STEAMPUNK_CHESTPLATE);
            entries.add(ModItems.STEAMPUNK_LEGGINGS);
            entries.add(ModItems.STEAMPUNK_BOOTS);
            entries.add(ModItems.WARRIOR_HELMET);
            entries.add(ModItems.WARRIOR_CHESTPLATE);
            entries.add(ModItems.WARRIOR_LEGGINGS);
            entries.add(ModItems.WARRIOR_BOOTS);
            entries.add(ModItems.WITHER_HELMET);
            entries.add(ModItems.WITHER_CHESTPLATE);
            entries.add(ModItems.WITHER_LEGGINGS);
            entries.add(ModItems.WOODEN_HELMET);
            entries.add(ModItems.WOODEN_CHESTPLATE);
            entries.add(ModItems.WOODEN_LEGGINGS);
            entries.add(ModItems.WOODEN_BOOTS);
            entries.add(ModItems.HOBBIT_SPAWN_EGG);
            entries.add(ModItems.MOUNTAIN_DWARF_SPAWN_EGG);
            entries.add(ModItems.GOBLIN_WARLORD_SPAWN_EGG);
            entries.add(ModItems.MAGMAMUNCHER_ALPHA_SPAWN_EGG);
            entries.add(ModItems.EMBER_WYVERN_SPAWN_EGG);
        });
    }
}