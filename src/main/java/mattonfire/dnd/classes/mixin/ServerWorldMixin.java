package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Rest.BedRest;
import mattonfire.dnd.entity.DragonPart;
import mattonfire.dnd.entity.DragonPartTracker;
import mattonfire.dnd.entity.MultipartDragon;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

/**
 * Lets the server resolve a dragon part id from a player's attack packet, like ender dragon parts,
 * and gives bed long rests when the night is skipped.
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @Inject(method = "getDragonPart", at = @At("RETURN"), cancellable = true)
    private void dnd$findDragonPart(int id, CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        for (MultipartDragon dragon : ((DragonPartTracker) (Object) this).dnd$getDragons()) {
            for (DragonPart part : dragon.getParts()) {
                if (part.getId() == id) {
                    cir.setReturnValue(part);
                    return;
                }
            }
        }
    }

    /** The night is being skipped: everyone fully asleep gets their bed long rest before waking. */
    @Inject(method = "wakeSleepingPlayers", at = @At("HEAD"))
    private void dnd$bedRestOnNightSkip(CallbackInfo ci) {
        BedRest.onSleepersWoken((ServerWorld) (Object) this);
    }
}
