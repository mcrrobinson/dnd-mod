package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Downed.DownedCombat;
import net.minecraft.entity.LivingEntity;

/**
 * Mobs can't target Downed players. {@code canTarget} is what target goals ({@code TargetPredicate}),
 * {@code TrackTargetGoal.shouldContinue} and brain attack sensors check, so a mob picks the next player in
 * range, or drops a Downed one it was already chasing. See {@link DownedCombat}.
 */
@Mixin(LivingEntity.class)
public abstract class DownedTargetMixin {
    @Inject(method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void dnd$ignoreDowned(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (DownedCombat.untargetable((LivingEntity) (Object) this, target)) {
            cir.setReturnValue(false);
        }
    }
}
