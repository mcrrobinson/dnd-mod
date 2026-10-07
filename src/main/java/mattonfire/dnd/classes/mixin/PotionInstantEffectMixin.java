package mattonfire.dnd.classes.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.PotionImmunity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;

// Instant potion effects (healing, harming) skip addStatusEffect, so block them here too.
@Mixin(StatusEffect.class)
public class PotionInstantEffectMixin {
    @Inject(method = "applyInstantEffect", at = @At("HEAD"), cancellable = true)
    private void dnd$blockPotionInstantEffect(@Nullable Entity source, @Nullable Entity attacker,
            LivingEntity target, int amplifier, double proximity, CallbackInfo ci) {
        if (PotionImmunity.shouldBlock(target, (StatusEffect) (Object) this)) {
            ci.cancel();
        }
    }
}
