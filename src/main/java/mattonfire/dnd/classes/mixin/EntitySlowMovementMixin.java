package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Feats.Feat;
import mattonfire.dnd.classes.Feats.Feats;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * The Mobile feat: while sprinting, cobwebs, sweet berry bushes and powder snow ({@code slowMovement}) and soul sand
 * and honey (the velocity multiplier) don't slow you. Runs on both sides, since the client moves its own player.
 */
@Mixin(Entity.class)
public abstract class EntitySlowMovementMixin {
    @org.spongepowered.asm.mixin.Unique
    private boolean dnd$mobileSprinting() {
        return (Object) this instanceof PlayerEntity player && player.isSprinting()
                && Feats.has(player, Feat.MOBILE);
    }

    @Inject(method = "slowMovement", at = @At("HEAD"), cancellable = true)
    private void dnd$mobileIgnoresWebs(BlockState state, Vec3d multiplier, CallbackInfo ci) {
        if (dnd$mobileSprinting()) {
            ci.cancel();
        }
    }

    @Inject(method = "getVelocityMultiplier", at = @At("RETURN"), cancellable = true)
    private void dnd$mobileIgnoresSoulSand(CallbackInfoReturnable<Float> cir) {
        if (cir.getReturnValueF() < 1.0F && dnd$mobileSprinting()) {
            cir.setReturnValue(1.0F);
        }
    }
}
