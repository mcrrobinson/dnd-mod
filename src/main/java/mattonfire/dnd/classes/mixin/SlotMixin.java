package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;

@Mixin(Slot.class)
public class SlotMixin {

    // Paladins cannot craft. Every way of taking a crafting result (click,
    // shift-click, number-key swap, drop, double-click collect) goes through
    // canTakeItems, on both the client and the server, so blocking it here keeps
    // both sides in agreement and avoids ghost items. The stonecutter and loom
    // output slots don't override canTakeItems, so they are blocked here too
    // (smithing is in SmithingScreenHandlerMixin).
    @Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
    private void dnd$blockPaladinCrafting(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot) (Object) this;
        if (isCraftingOutput(slot, player) && player instanceof PlayerEntityExt ext
                && ext.getDndClass() == DndCharacter.PALADIN) {
            cir.setReturnValue(false);
            if (player instanceof ServerPlayerEntity && slot.hasStack()) {
                player.sendMessage(Text.literal("Paladins cannot craft items!").formatted(Formatting.RED), true);
            }
        }
    }

    private static boolean isCraftingOutput(Slot slot, PlayerEntity player) {
        if (slot instanceof CraftingResultSlot) {
            return true;
        }
        ScreenHandler handler = player.currentScreenHandler;
        if (handler instanceof StonecutterScreenHandler) {
            return slot == handler.getSlot(1); // 0 = input, 1 = output
        }
        return handler instanceof LoomScreenHandler loom && slot == loom.getOutputSlot();
    }
}
