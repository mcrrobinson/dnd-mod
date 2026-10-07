package mattonfire.dnd.classes.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.PotionImmunity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;

// Drops potion-sourced effects the entity is immune to (see PotionImmunity).
@Mixin(LivingEntity.class)
public class PotionImmunityMixin {
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void dnd$blockPotionEffect(StatusEffectInstance effect, @Nullable Entity source,
            CallbackInfoReturnable<Boolean> cir) {
        if (PotionImmunity.shouldBlock((LivingEntity) (Object) this, effect.getEffectType())) {
            cir.setReturnValue(false);
        }
    }
}
