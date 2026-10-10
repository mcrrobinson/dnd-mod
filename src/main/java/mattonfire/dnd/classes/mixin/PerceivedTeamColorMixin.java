package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Client.Render.PerceivedMarks;
import net.minecraft.entity.Entity;

/** The Search outline ({@link PerceivedMarks}) is red rather than the team colour. */
@Mixin(Entity.class)
public abstract class PerceivedTeamColorMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void dndclasses$searchOutlineColor(CallbackInfoReturnable<Integer> cir) {
        if (PerceivedMarks.outlined((Entity) (Object) this)) {
            cir.setReturnValue(PerceivedMarks.OUTLINE_COLOR);
        }
    }
}
