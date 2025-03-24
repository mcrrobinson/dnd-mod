package mattonfire.dnd.classes.Items;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorItem.Type;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvents;

import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.sound.SoundEvent;


public class ModArmorMaterials {
    private static final int[] BASE_DURABILITY = {13, 15, 16, 11}; // Corresponds to [boots, leggings, chestplate, helmet]
    private static final int[] PROTECTION_VALUES = {2, 4, 6, 2}; // Corresponds to [boots, leggings, chestplate, helmet]

    public static final ArmorMaterial PINK_GARNET = new ArmorMaterial() {
        @Override public int getEnchantability() { return 20; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.ofItems(ModItems.PINK_GARNET_BOOTS); }
        @Override public String getName() { return "dndclasses:pink_garnet"; }
        @Override public float getToughness() { return 0; }
        @Override public float getKnockbackResistance() { return 0; }
        @Override public int getDurability(Type type) { return BASE_DURABILITY[type.getEquipmentSlot().getEntitySlotId()]; }
        @Override public int getProtection(Type type) { return PROTECTION_VALUES[type.getEquipmentSlot().getEntitySlotId()]; }
    };
}
