package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Feats.FeatArrows;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

/** Sharpshooter's past cover: shields don't block a Sharpshooter's arrows. */
@Mixin(LivingEntity.class)
public abstract class ShieldArrowMixin {
    @Inject(method = "blockedByShield", at = @At("HEAD"), cancellable = true)
    private void dnd$pastCover(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (FeatArrows.ignoresShields(source)) {
            cir.setReturnValue(false);
        }
    }
}
