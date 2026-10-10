package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.MobEntity;

@Mixin(TrackTargetGoal.class)
public abstract class TrackTargetGoalMixin {
    @Shadow
    @Final
    protected MobEntity mob;

    @Shadow
    public abstract void stop();

    /** The Beacon curse: a mob chasing a cursed player keeps after them from twice as far. */
    @Inject(method = "getFollowRange", at = @At("RETURN"), cancellable = true)
    private void dndclasses$beaconFollowRange(CallbackInfoReturnable<Double> cir) {
        double range = mattonfire.dnd.magic.Curse.modifyFollowRange(this.mob.getTarget(), cir.getReturnValueD());
        if (range != cir.getReturnValueD())
            cir.setReturnValue(range);
    }

    @Inject(method = "shouldContinue", at = @At("RETURN"), cancellable = true)
    protected void identity_shouldContinue(CallbackInfoReturnable<Boolean> cir) {
        // NO-OP
    }
}
