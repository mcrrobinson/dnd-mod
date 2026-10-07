package mattonfire.dnd.classes.Enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;

/**
 * Artificer boots enchantment: a stronger Feather Falling. Each level cuts fall
 * damage further, and levels II and III also slow-fall the wearer (see
 * {@link mattonfire.dnd.classes.Featherfall}).
 */
public class FeatherfallEnchantment extends Enchantment {
    public FeatherfallEnchantment(Rarity rarity) {
        super(rarity, EnchantmentTarget.ARMOR_FEET, new EquipmentSlot[] { EquipmentSlot.FEET });
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    // Same power curve as vanilla Feather Falling.
    @Override
    public int getMinPower(int level) {
        return 5 + (level - 1) * 6;
    }

    @Override
    public int getMaxPower(int level) {
        return getMinPower(level) + 6;
    }

    // Featherfall replaces Feather Falling rather than stacking with it.
    @Override
    protected boolean canAccept(Enchantment other) {
        return super.canAccept(other) && other != Enchantments.FEATHER_FALLING;
    }
}
