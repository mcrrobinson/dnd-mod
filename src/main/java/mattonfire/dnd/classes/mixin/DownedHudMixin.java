package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Client.Hud.DownedHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;

/**
 * The downed HUD's pieces that sit inside vanilla's: the red vignette under everything else, and the hearts row
 * drawn empty (a Downed player is kept at 1 HP, which would otherwise show half a heart).
 */
@Mixin(InGameHud.class)
public abstract class DownedHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void dndclasses$downedVignette(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        DownedHud.renderVignette(matrices, tickDelta);
    }

    // renderHealthBar(matrices, player, x, y, lines, regeneratingHeartIndex, maxHealth, lastHealth, health,
    // absorption, blinking): int args 4 and 5 are lastHealth and health.
    @ModifyVariable(method = "renderHealthBar", at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int dndclasses$downedLastHealth(int lastHealth) {
        return DownedHud.emptyHearts() ? 0 : lastHealth;
    }

    @ModifyVariable(method = "renderHealthBar", at = @At("HEAD"), argsOnly = true, ordinal = 5)
    private int dndclasses$downedHealth(int health) {
        return DownedHud.emptyHearts() ? 0 : health;
    }
}
