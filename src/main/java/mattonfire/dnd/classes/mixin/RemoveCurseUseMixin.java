package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.magic.RemoveCurse;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.HitResult;

/**
 * A Cleric's sneak + use with an empty hand on the air casts Remove Curse on themselves. The client sends no
 * packet for using an empty hand, so this sends {@link RemoveCurse#C2S_SELF}; the server checks everything again.
 */
@Mixin(MinecraftClient.class)
public abstract class RemoveCurseUseMixin {
    @Shadow
    public ClientPlayerEntity player;

    @Shadow
    public HitResult crosshairTarget;

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void dndclasses$removeCurseSelf(CallbackInfo ci) {
        ClientPlayerEntity p = this.player;
        if (p == null || !p.isSneaking() || !p.getMainHandStack().isEmpty())
            return;
        if (crosshairTarget != null && crosshairTarget.getType() != HitResult.Type.MISS)
            return;
        if (!(p instanceof PlayerEntityExt ext) || ext.getDndClass() != DndCharacter.CLERIC)
            return;
        ClientPlayNetworking.send(RemoveCurse.C2S_SELF, PacketByteBufs.empty());
    }
}
