package mattonfire.dnd.classes.Obstacles.types;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

/**
 * What the craft obstacles use up per attempt: a Clockwork Door takes an iron ingot and a redstone dust, an
 * Alchemical Seal or a Miasma Vent takes a potion. Creative players never pay.
 */
public final class Reagents {
    private Reagents() {
    }

    /**
     * Any brewed potion: drinkable, splash or lingering, with any potion but plain water (and the empty and
     * uncraftable ones). An Awkward Potion counts.
     */
    public static boolean isPotion(ItemStack stack) {
        if (!(stack.getItem() instanceof PotionItem)) {
            return false;
        }
        Potion potion = PotionUtil.getPotion(stack);
        return potion != Potions.EMPTY && potion != Potions.WATER;
    }

    /** Uses up one potion: the one in the main hand, else the off hand, else the first in the inventory. */
    public static boolean takePotion(ServerPlayerEntity player) {
        if (player.isCreative()) {
            return true;
        }
        for (Hand hand : Hand.values()) {
            ItemStack held = player.getStackInHand(hand);
            if (isPotion(held)) {
                held.decrement(1);
                return true;
            }
        }
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (isPotion(stack)) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }

    /** Uses up an iron ingot and a redstone dust, or neither if the player lacks one of them. */
    public static boolean takeIronAndRedstone(ServerPlayerEntity player) {
        if (player.isCreative()) {
            return true;
        }
        PlayerInventory inventory = player.getInventory();
        if (inventory.count(Items.IRON_INGOT) < 1 || inventory.count(Items.REDSTONE) < 1) {
            return false;
        }
        inventory.remove(stack -> stack.isOf(Items.IRON_INGOT), 1, player.playerScreenHandler.getCraftingInput());
        inventory.remove(stack -> stack.isOf(Items.REDSTONE), 1, player.playerScreenHandler.getCraftingInput());
        return true;
    }
}
