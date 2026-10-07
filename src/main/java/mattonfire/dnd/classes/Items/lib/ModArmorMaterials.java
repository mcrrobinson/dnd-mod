package mattonfire.dnd.classes.Items.lib;

import net.minecraft.item.ArmorItem.Type;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public enum ModArmorMaterials implements ArmorMaterial {
    PRISMARINE("prismarine"),
    HOLY("holy_armor"),
    ROBE("robe"),
    STEAMPUNK("steam_punk"),
    WARRIOR("warrior"),
    WITHER("wither"),
    WOODEN("wooden");

    private final String name;

    ModArmorMaterials(String name) {
        this.name = name;
    }

    @Override
    public int getDurability(Type type) {
        return 0;
    }

    @Override
    public int getEnchantability() {
        return 0;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_GENERIC;
    }

    @Override
    public float getKnockbackResistance() {
        return 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getProtection(Type type) {
        return 0;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return null;
    }

    @Override
    public float getToughness() {
        return 0;
    }
}
