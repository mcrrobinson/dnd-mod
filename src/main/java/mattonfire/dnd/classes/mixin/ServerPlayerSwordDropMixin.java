package mattonfire.dnd.classes.mixin;

import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Misc.BloodHunterSwords;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerSwordDropMixin {

    // Q with a sword selected in the hotbar (server side).
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void dnd$keepBloodHunterSword(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (BloodHunterSwords.blockDrop(player, player.getMainHandStack())) {
            // The client may have removed it already; send the real slot back.
            player.currentScreenHandler.syncState();
            cir.setReturnValue(false);
        }
    }
}
