package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Misc.ArtificerCrafting;

/**
 * Shift-click crafting: enchant the result stack while it is still in the
 * result slot, before vanilla moves it into the inventory.
 */
@Mixin({ CraftingScreenHandler.class, PlayerScreenHandler.class })
public class CraftingQuickMoveMixin {
    @Inject(method = "quickMove", at = @At("HEAD"))
    private void dnd$artificerAutoEnchant(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        ScreenHandler handler = (ScreenHandler) (Object) this;
        if (index < 0 || index >= handler.slots.size()) {
            return;
        }
        Slot slot = handler.slots.get(index);
        // Equipment doesn't stack, so it only moves if there is a free slot.
        // Skipping otherwise stops re-rolling by shift-clicking with a full inventory.
        if (slot instanceof CraftingResultSlot && slot.hasStack() && player.getInventory().getEmptySlot() != -1) {
            ArtificerCrafting.tryAutoEnchant(player, slot.getStack());
        }
    }
}
