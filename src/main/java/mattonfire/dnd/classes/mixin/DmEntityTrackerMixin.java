package mattonfire.dnd.classes.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.dm.DmTracked;
import mattonfire.dnd.dm.DmVeil;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The DM veil: a veiled DM is never sent to non-DM clients. Their tracker stops tracking them for that viewer
 * (which despawns them on that client), so no model, armour, held item, name tag or particles get through.
 */
@Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker")
public abstract class DmEntityTrackerMixin implements DmTracked {
    @Shadow
    @Final
    Entity entity;

    @Shadow
    public abstract void stopTracking(ServerPlayerEntity player);

    @Shadow
    public abstract void updateTrackedStatus(List<ServerPlayerEntity> players);

    @Inject(method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"), cancellable = true)
    private void dnd$hideVeiledDm(ServerPlayerEntity viewer, CallbackInfo ci) {
        if (viewer != this.entity && DmVeil.hiddenFrom(this.entity, viewer)) {
            this.stopTracking(viewer);
            ci.cancel();
        }
    }

    @Override
    public void dnd$updateTrackedStatus(List<ServerPlayerEntity> players) {
        this.updateTrackedStatus(players);
    }
}
