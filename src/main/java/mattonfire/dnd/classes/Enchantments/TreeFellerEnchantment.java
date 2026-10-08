package mattonfire.dnd.classes.Enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;

/**
 * Fells connected logs (see TreeFeller). Registered with the DIGGER target, which covers every mining
 * tool, and narrowed to axes here, since vanilla has no axe-only target. EnchantingTableMixin checks
 * both, so the table only rolls it on axes; anvils and /enchant use this check directly.
 */
public class TreeFellerEnchantment extends Enchantment {
    public TreeFellerEnchantment(Rarity rarity, EnchantmentTarget enchantmentTarget,
            EquipmentSlot... equipmentSlots) {
        super(rarity, enchantmentTarget, equipmentSlots);
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof AxeItem;
    }
}
