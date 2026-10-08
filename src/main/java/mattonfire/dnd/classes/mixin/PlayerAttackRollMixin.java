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
        return AttackRolls.criticalDamage((PlayerEntity) (Object) this, attackTarget, amount);
    }

    /** The entity being attacked, for the crit check inside attack(). */
    private Entity attackTarget;

    @Inject(method = "attack", at = @At("HEAD"))
    private void dndclasses$rememberTarget(Entity target, CallbackInfo ci) {
        attackTarget = target;
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void dndclasses$endAttack(Entity target, CallbackInfo ci) {
        AttackRolls.endAttack((PlayerEntity) (Object) this, target);
        attackTarget = null;
    }
}
