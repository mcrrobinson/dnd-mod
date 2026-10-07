package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.LegacySmithingScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;

@Mixin({ SmithingScreenHandler.class, LegacySmithingScreenHandler.class })
public class SmithingScreenHandlerMixin {

    // Paladins cannot craft, which includes smithing. The forging output slot
    // asks canTakeOutput instead of Slot.canTakeItems, so SlotMixin misses it.
    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    private void dnd$blockPaladinSmithing(PlayerEntity player, boolean present,
            CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.PALADIN) {
            cir.setReturnValue(false);
            if (player instanceof ServerPlayerEntity && present) {
                player.sendMessage(Text.literal("Paladins cannot craft items!").formatted(Formatting.RED), true);
            }
        }
    }
}
