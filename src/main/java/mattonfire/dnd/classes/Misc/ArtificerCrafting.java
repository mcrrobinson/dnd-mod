package mattonfire.dnd.classes.Misc;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.random.Random;

/**
 * Artificer passive: crafted tools, weapons and armor have a chance to come out
 * already enchanted.
 */
public class ArtificerCrafting {
    public static final float AUTO_ENCHANT_CHANCE = 0.25f;
    // Enchanting table "level" used for the roll (the table goes up to 30).
    public static final int MIN_ENCHANT_LEVEL = 5;
    public static final int MAX_ENCHANT_LEVEL = 15;

    public static void tryAutoEnchant(PlayerEntity player, ItemStack stack) {
        if (player.getWorld().isClient || stack.isEmpty() || !isEquipment(stack) || !stack.isEnchantable()) {
            return;
        }
        if (!(player instanceof PlayerEntityExt ext) || ext.getDndClass() != DndCharacter.ARTIFICER) {
            return;
        }
        Random random = player.getRandom();
        if (random.nextFloat() >= AUTO_ENCHANT_CHANCE) {
            return;
        }
        int level = MIN_ENCHANT_LEVEL + random.nextInt(MAX_ENCHANT_LEVEL - MIN_ENCHANT_LEVEL + 1);
        EnchantmentHelper.enchant(random, stack, level, false);
    }

    private static boolean isEquipment(ItemStack stack) {
        return stack.getItem() instanceof ToolItem // swords, axes, pickaxes, shovels, hoes
                || stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof RangedWeaponItem // bows, crossbows
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof FishingRodItem;
    }
}
