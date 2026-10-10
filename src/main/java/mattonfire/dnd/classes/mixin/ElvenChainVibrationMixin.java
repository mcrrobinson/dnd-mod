package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Items.racial.RacialGear;
import net.minecraft.entity.Entity;

/**
 * Elven Chain: a sneaking Elf in the full set occludes their vibrations, like an entity wrapped in wool, so
 * sculk sensors, shriekers and wardens ignore everything they do (steps, block breaks, hits, eating).
 */
@Mixin(Entity.class)
public class ElvenChainVibrationMixin {
    @Inject(method = "occludeVibrationSignals", at = @At("HEAD"), cancellable = true)
    private void dnd$elvenChainSilence(CallbackInfoReturnable<Boolean> cir) {
        if (RacialGear.silencesVibrations((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
