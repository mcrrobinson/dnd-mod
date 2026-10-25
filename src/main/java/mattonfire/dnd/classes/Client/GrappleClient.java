package mattonfire.dnd.classes.Client;

import mattonfire.dnd.classes.SkillChecks.Grapple;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * While the server says we're held ({@link Grapple#S2C_HELD}), each jump keypress sends a struggle
 * ({@link Grapple#C2S_STRUGGLE}); the server rolls the escape check and enforces the cooldown.
 */
@Environment(EnvType.CLIENT)
public final class GrappleClient {
    private static boolean held;

    private GrappleClient() {
    }

    public static boolean isHeld() {
        return held;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Grapple.S2C_HELD, (client, handler, buf, sender) -> {
            boolean now = buf.readBoolean();
            client.execute(() -> {
                if (now && !held && client.player != null) {
                    client.player.sendMessage(Text.translatable("grapple.dndclasses.held_hint"), true);
                }
                held = now;
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> held = false);
        ClientTickEvents.END_CLIENT_TICK.register(GrappleClient::tick);
    }

    private static void tick(MinecraftClient client) {
        // Vanilla only reads the jump key's held state, so its press count is ours to drain
        boolean pressed = false;
        while (client.options.jumpKey.wasPressed()) {
            pressed = true;
        }
        if (pressed) {
            ConditionClient.onJumpPressed(client); // Prone: stand up
        }
        if (pressed && held && client.player != null && client.currentScreen == null) {
            ClientPlayNetworking.send(Grapple.C2S_STRUGGLE, PacketByteBufs.empty());
        }
    }
}
