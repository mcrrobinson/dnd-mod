package mattonfire.dnd.classes.mixin;

import net.minecraft.client.network.ClientPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Misc.BloodHunterSwords;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerSwordDropMixin {

    // Q with a sword selected in the hotbar: don't predict the drop or send it.
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void dnd$keepBloodHunterSword(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if (BloodHunterSwords.blockDrop(player, player.getMainHandStack())) {
            cir.setReturnValue(false);
        }
    }
}
