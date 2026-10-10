package mattonfire.dnd.classes.Client;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Downed.DownedEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.network.PacketByteBuf;

/**
 * The local player's side of being Downed: inventory and container screens stay closed, and holding the
 * power-up key tells the server you're giving up (it counts the 3 s). The crawl pose, no jumping and no
 * sprinting come from common mixins reading the synced Downed bits.
 */
public final class DownedClient {
    private static boolean sentHolding;

    private DownedClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(DownedClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> sentHolding = false);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        boolean downed = Downed.is(client.player);
        if (downed && client.currentScreen instanceof HandledScreen<?>) {
            client.player.closeHandledScreen();
        }
        boolean holding = downed && DndClassesClient.POWER_UP_KEY != null && DndClassesClient.POWER_UP_KEY.isPressed();
        if (holding != sentHolding) {
            sentHolding = holding;
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeBoolean(holding);
            ClientPlayNetworking.send(DownedEvents.C2S_GIVE_UP, buf);
        }
    }
}
