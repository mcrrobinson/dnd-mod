package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.SkillChecks.Stealth;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Passive Stealth: a sneaking player's detection range shrinks with their passive Stealth ({@link Stealth}). */
@Mixin(LivingEntity.class)
public abstract class StealthMixin {
    @Inject(method = "getAttackDistanceScalingFactor", at = @At("RETURN"), cancellable = true)
    private void dndclasses$passiveStealth(Entity observer, CallbackInfoReturnable<Double> cir) {
        if ((Object) this instanceof PlayerEntity player && player.isSneaking() && !player.getWorld().isClient) {
            cir.setReturnValue(cir.getReturnValueD() * Stealth.factor(player));
        }
    }
}
