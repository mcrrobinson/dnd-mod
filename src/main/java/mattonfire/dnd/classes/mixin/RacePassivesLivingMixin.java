package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mattonfire.dnd.classes.Race.RacePassives;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Racial resistances to damage and effects ({@link RacePassives}). */
@Mixin(LivingEntity.class)
public abstract class RacePassivesLivingMixin {
    /** Hellish Resistance, Draconic Resistance and Gnome Cunning, before armour. */
    @ModifyArg(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"), index = 1)
    private float dnd$racialResistance(DamageSource source, float amount) {
        return RacePassives.resist((LivingEntity) (Object) this, source, amount);
    }

    /** Dwarven Resilience (poison) and a Frost Dragonborn's Freeze last half as long. */
    @ModifyVariable(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private StatusEffectInstance dnd$racialEffectDuration(StatusEffectInstance effect) {
        return RacePassives.shortenEffect((LivingEntity) (Object) this, effect);
    }
}
