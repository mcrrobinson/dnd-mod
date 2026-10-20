package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Rest.RestSeatEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Sneaking doesn't drop a player off a {@link RestSeatEntity}: they sit down while sneaking, so
 * vanilla would stand them straight back up. The seat ends the rest on a fresh sneak instead.
 */
@Mixin(PlayerEntity.class)
public abstract class RestSeatDismountMixin {
    @Inject(method = "shouldDismount", at = @At("HEAD"), cancellable = true)
    private void dndclasses$stayOnRestSeat(CallbackInfoReturnable<Boolean> cir) {
        if (RestSeatEntity.isSeated((PlayerEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
