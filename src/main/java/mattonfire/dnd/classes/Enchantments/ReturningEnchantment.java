package mattonfire.dnd.classes.Enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.TridentItem;

/**
 * Artificer enchantment: a thrown weapon (trident) or throwable (snowball, egg,
 * ender pearl) goes straight back into the thrower's inventory once it lands.
 * The behaviour lives in {@link mattonfire.dnd.classes.Misc.Returning}.
 */
public class ReturningEnchantment extends Enchantment {
    public ReturningEnchantment(Rarity rarity, EquipmentSlot... equipmentSlots) {
        super(rarity, EnchantmentTarget.TRIDENT, equipmentSlots);
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
        return isThrowable(stack);
    }

    public static boolean isThrowable(ItemStack stack) {
        return stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof SnowballItem
                || stack.getItem() instanceof EggItem
                || stack.getItem() instanceof EnderPearlItem;
    }

    @Override
    protected boolean canAccept(Enchantment other) {
        // Loyalty does the same job; a Riptide trident can't be thrown at all.
        return super.canAccept(other) && other != Enchantments.LOYALTY && other != Enchantments.RIPTIDE;
    }
}
