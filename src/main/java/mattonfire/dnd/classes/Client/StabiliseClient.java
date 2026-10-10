package mattonfire.dnd.classes.Client;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Downed.Stabilise;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Tells the server which Downed player the local player is holding use (right-click) on, for
 * {@link Stabilise}: the entity id when it changes, -1 when they let go, and a refresh every second so a hold
 * the server interrupted (a hit) starts again.
 */
public final class StabiliseClient {
    private static int sentTarget = -1;
    private static int sinceSent;

    private StabiliseClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(StabiliseClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> sentTarget = -1);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        int target = -1;
        if (client.options.useKey.isPressed() && client.currentScreen == null && !Downed.is(client.player)
                && !client.player.isUsingItem()
                && client.player.getMainHandStack().getUseAction() == UseAction.NONE
                && client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity other && Downed.is(other)) {
            target = other.getId();
        }
        sinceSent++;
        if (target != sentTarget || target >= 0 && sinceSent >= 20) {
            sentTarget = target;
            sinceSent = 0;
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeVarInt(target);
            ClientPlayNetworking.send(Stabilise.C2S_HELP_HOLD, buf);
        }
    }
}
