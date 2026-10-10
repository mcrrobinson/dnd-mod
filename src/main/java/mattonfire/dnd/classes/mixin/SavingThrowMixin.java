package mattonfire.dnd.classes.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import mattonfire.dnd.classes.SkillChecks.SaveRules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;

/** The data-driven saving throws ({@link SaveRules}) against damage types and status effects. Players only. */
@Mixin(LivingEntity.class)
public abstract class SavingThrowMixin {
    /** At the head of damage, so the save comes before armour and the Paladin aura. */
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float dnd$saveAgainstDamage(float amount, @Local(argsOnly = true) DamageSource source) {
        return SaveRules.onDamage((LivingEntity) (Object) this, source, amount);
    }

    /** The two-argument overload, where the effect's source is known (the one-argument one passes null). */
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void dnd$saveAgainstEffect(StatusEffectInstance effect, @Nullable Entity source,
            CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) {
            return;
        }
        StatusEffectInstance saved = SaveRules.onEffect(self, effect, source);
        if (saved == effect) {
            return;
        }
        cir.setReturnValue(saved != null && SaveRules.reapply(self, saved, source));
    }
}
