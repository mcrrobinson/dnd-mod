package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Client.Hud.PowerupOverlay;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    // renderStatusBars only runs when the player has status bars (not creative/spectator)
    // and before chat, so the mana bar layers like health and food.
    @Inject(method = "renderStatusBars", at = @At("TAIL"))
    private void dndclasses$renderManaBar(MatrixStack matrices, CallbackInfo ci) {
        PowerupOverlay.render(matrices);
    }
}
