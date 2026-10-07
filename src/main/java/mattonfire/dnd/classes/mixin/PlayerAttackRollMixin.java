package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.SkillChecks.AttackRolls;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** Natural 20 attack rolls double the hit's damage (see {@link AttackRolls}). */
@Mixin(PlayerEntity.class)
public abstract class PlayerAttackRollMixin {
    @ModifyArg(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"), index = 1)
    private float dndclasses$criticalDamage(float amount) {
        return AttackRolls.criticalDamage((PlayerEntity) (Object) this, amount);
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void dndclasses$endAttack(Entity target, CallbackInfo ci) {
        AttackRolls.endAttack((PlayerEntity) (Object) this, target);
    }
}
