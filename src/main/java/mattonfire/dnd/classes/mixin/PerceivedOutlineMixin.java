package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Client.Render.PerceivedMarks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

/** Draws the glow outline round an entity a Search found ({@link PerceivedMarks}), for this player only. */
@Mixin(MinecraftClient.class)
public abstract class PerceivedOutlineMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void dndclasses$searchOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (PerceivedMarks.outlined(entity)) {
            cir.setReturnValue(true);
        }
    }
}
