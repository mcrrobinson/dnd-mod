package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Client.Hud.DownedHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

/** Downed party members within 64 blocks are drawn with the glow outline, so you can find them through walls. */
@Mixin(MinecraftClient.class)
public abstract class DownedOutlineMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void dndclasses$downedAllyOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (DownedHud.outlined(entity)) {
            cir.setReturnValue(true);
        }
    }
}
