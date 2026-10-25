package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Conditions.Conditions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;

/**
 * Incapacitated mobs (Stunned, Paralyzed, Petrified, Incapacitated) stop thinking: no goals, navigation, look or
 * move control, and no melee. Mod mobs that run specials from {@code tick()} check {@link Conditions#canAct}.
 */
@Mixin(MobEntity.class)
public abstract class ConditionMobMixin {
    @Inject(method = "tickNewAi", at = @At("HEAD"), cancellable = true)
    private void dnd$incapacitatedNoAi(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (!Conditions.canAct(self)) {
            // The move control set these last tick; without it they'd keep walking
            self.setForwardSpeed(0.0F);
            self.setSidewaysSpeed(0.0F);
            self.setUpwardSpeed(0.0F);
            self.setJumping(false);
            self.getNavigation().stop();
            ci.cancel();
        }
    }

    @Inject(method = "tryAttack", at = @At("HEAD"), cancellable = true)
    private void dnd$incapacitatedNoAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!Conditions.canAct((MobEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
