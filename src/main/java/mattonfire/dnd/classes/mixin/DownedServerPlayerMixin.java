package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Downed.Downed;
import net.minecraft.server.network.ServerPlayerEntity;

/** Downed players can't drop items with Q (so they can't dump their gear). */
@Mixin(ServerPlayerEntity.class)
public class DownedServerPlayerMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void dnd$noDownedDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (Downed.is(player)) {
            player.currentScreenHandler.syncState();
            cir.setReturnValue(false);
        }
    }
}
