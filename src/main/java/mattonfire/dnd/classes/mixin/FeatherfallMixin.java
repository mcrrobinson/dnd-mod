package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Featherfall;
import net.minecraft.entity.LivingEntity;

/** Featherfall boots: cut the fall damage vanilla computes. */
@Mixin(LivingEntity.class)
public abstract class FeatherfallMixin {
    @Inject(method = "computeFallDamage", at = @At("RETURN"), cancellable = true)
    private void dndclasses$featherfall(float fallDistance, float damageMultiplier,
            CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Featherfall.reduceFallDamage((LivingEntity) (Object) this, cir.getReturnValueI()));
    }
}
