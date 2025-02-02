package mattonfire.dnd.classes.Enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public class GridMinerEnchantment extends Enchantment {
    public GridMinerEnchantment(Rarity rarity, EnchantmentTarget enchantmentTarget,
            EquipmentSlot... equipmentSlots) {
        super(rarity, enchantmentTarget, equipmentSlots);
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }
}
