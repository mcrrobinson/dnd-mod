package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Misc.ArtificerCrafting;

@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {
    // Normal click / drop out of the result slot. On shift-click the stack has
    // already been moved into the inventory (and is empty here), that path is
    // handled in CraftingQuickMoveMixin.
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void dnd$artificerAutoEnchant(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        ArtificerCrafting.tryAutoEnchant(player, stack);
    }
}
