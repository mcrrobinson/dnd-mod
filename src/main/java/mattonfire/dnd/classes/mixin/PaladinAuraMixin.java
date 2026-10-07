package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import mattonfire.dnd.classes.Progression.Classes.PaladinSkills;
import net.minecraft.entity.LivingEntity;

/** Paladin's Aura of Protection, which also shields players of other classes. */
@Mixin(LivingEntity.class)
public abstract class PaladinAuraMixin {
    @ModifyArg(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"), index = 1)
    private float dndclasses$auraOfProtection(float amount) {
        return PaladinSkills.applyAuraOfProtection((LivingEntity) (Object) this, amount);
    }
}
