package mattonfire.dnd.classes.Enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

/**
 * Vampiric: melee hits heal the wielder for a share of the damage dealt
 * (see {@link #HEAL_FRACTION_PER_LEVEL} and {@code VampiricMixin}).
 */
public class VampiricEnchantment extends Enchantment {
    /** Fraction of the damage dealt that is healed, per enchantment level. */
    public static final float HEAL_FRACTION_PER_LEVEL = 0.1f;

    public VampiricEnchantment(Rarity rarity, EnchantmentTarget enchantmentTarget,
            EquipmentSlot... equipmentSlots) {
        super(rarity, enchantmentTarget, equipmentSlots);
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    public static float healFraction(int level) {
        return HEAL_FRACTION_PER_LEVEL * level;
    }
}
