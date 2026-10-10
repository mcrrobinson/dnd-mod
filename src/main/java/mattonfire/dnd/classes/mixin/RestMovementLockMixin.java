package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Rest.RestSnapshot;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;

/**
 * While a rest is in progress (the server's {@link RestSnapshot#sessionKind}), the
 * player sits still: no walking or jumping. Sneaking and the camera still work.
 */
@Mixin(KeyboardInput.class)
public abstract class RestMovementLockMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void dndclasses$lockWhileResting(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (RestSnapshot.client.sessionKind() == 0) {
            return;
        }
        Input input = (Input) (Object) this;
        input.movementForward = 0;
        input.movementSideways = 0;
        input.pressingForward = false;
        input.pressingBack = false;
        input.pressingLeft = false;
        input.pressingRight = false;
        input.jumping = false;
    }
}
