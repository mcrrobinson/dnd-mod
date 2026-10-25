package mattonfire.dnd.classes.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.entity.drow.DrowAllies;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

/**
 * Spiders and cave spiders never target drow (the Spider Houses' alliance, {@link DrowAllies}): not on sight, not
 * in revenge, not when told to. The call is dropped, so a spider keeps whatever target it had. On {@link MobEntity}
 * because spiders inherit {@code setTarget} from it.
 */
@Mixin(MobEntity.class)
public abstract class SpiderTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void dndclasses$spidersSpareDrow(@Nullable LivingEntity target, CallbackInfo ci) {
        if (DrowAllies.spiderSpares((MobEntity) (Object) this, target)) {
            ci.cancel();
        }
    }
}
