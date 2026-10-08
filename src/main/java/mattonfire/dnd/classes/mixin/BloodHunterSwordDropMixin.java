package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Misc.BloodHunterSwords;

@Mixin(ScreenHandler.class)
public class BloodHunterSwordDropMixin {

    // Inventory screens: Q over a slot (THROW) and clicking outside the window with a
    // sword on the cursor. Runs on both sides so the client doesn't predict the drop;
    // the server resyncs the slots after a cancelled click anyway.
    @Inject(method = "internalOnSlotClick", at = @At("HEAD"), cancellable = true)
    private void dnd$keepBloodHunterSwords(int slotIndex, int button, SlotActionType actionType, PlayerEntity player,
            CallbackInfo ci) {
        ScreenHandler handler = (ScreenHandler) (Object) this;
        boolean blocked = false;
        if (actionType == SlotActionType.THROW && slotIndex >= 0 && slotIndex < handler.slots.size()
                && handler.getCursorStack().isEmpty()) {
            blocked = BloodHunterSwords.blockDrop(player, handler.getSlot(slotIndex).getStack());
        } else if ((actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE)
                && slotIndex == ScreenHandler.EMPTY_SPACE_SLOT_INDEX) {
            blocked = BloodHunterSwords.blockDrop(player, handler.getCursorStack());
        }
        if (blocked) {
            ci.cancel();
        }
    }
}
