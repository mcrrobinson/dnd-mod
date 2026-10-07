package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.Warlock;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Vanilla never sends a use packet for an empty hand, so the Warlock's
 * fireball is triggered here on the client and thrown by the server.
 */
@Mixin(MinecraftClient.class)
public abstract class WarlockFireballMixin {
    @Shadow
    public ClientPlayerEntity player;

    @Shadow
    public HitResult crosshairTarget;

    @Shadow
    private int itemUseCooldown;

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void dndclasses$warlockFireball(CallbackInfo ci) {
        if (player == null || !Warlock.isWarlock(player) || !player.getMainHandStack().isEmpty()) {
            return;
        }

        // Keep block and entity interaction (doors, chests, villagers...) working;
        // only throw at the air or at hostile mobs.
        boolean aimingAtAir = crosshairTarget == null || crosshairTarget.getType() == HitResult.Type.MISS;
        boolean aimingAtMonster = crosshairTarget instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof Monster;
        if (!aimingAtAir && !aimingAtMonster) {
            return;
        }

        ClientPlayNetworking.send(Warlock.C2S_WARLOCK_FIREBALL, new PacketByteBuf(Unpooled.buffer()));
        player.swingHand(Hand.MAIN_HAND);
        itemUseCooldown = 4;
        ci.cancel();
    }
}
