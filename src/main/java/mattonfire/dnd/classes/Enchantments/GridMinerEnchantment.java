package mattonfire.dnd.classes.Enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;

/**
 * Mines connected blocks (see GridMiner); level II reaches further. Registered with the
 * DIGGER target and narrowed to pickaxes and shovels, the only tools it works with.
 */
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

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof PickaxeItem || stack.getItem() instanceof ShovelItem;
    }
}
