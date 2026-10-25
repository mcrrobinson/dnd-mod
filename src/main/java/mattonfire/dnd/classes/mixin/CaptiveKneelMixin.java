package mattonfire.dnd.classes.mixin;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mobs drawn with a humanoid model crouch when their pose is crouching: captives kneel in their pens
 * ({@link mattonfire.dnd.world.underdark.Captives}). Players set the flag themselves; no mob sets it otherwise.
 */
@Mixin(BipedEntityModel.class)
public abstract class CaptiveKneelMixin<T extends LivingEntity> {
    @Shadow
    public boolean sneaking;

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void dndclasses$kneel(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw,
                                  float headPitch, CallbackInfo ci) {
        if (entity instanceof MobEntity) {
            this.sneaking = entity.getPose() == EntityPose.CROUCHING;
        }
    }
}
