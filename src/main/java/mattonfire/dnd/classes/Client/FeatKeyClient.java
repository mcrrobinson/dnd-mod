package mattonfire.dnd.classes.Client;

import org.lwjgl.glfw.GLFW;

import mattonfire.dnd.classes.Feats.FeatEffects;
import mattonfire.dnd.classes.Feats.FeatPackets;
import mattonfire.dnd.classes.mixin.LivingEntityAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/**
 * The feat toggle key (N): flips Great Weapon Master's Power Attack and Sharpshooter's Steady Aim on the server
 * ({@link FeatPackets#C2S_TOGGLE}). Also resets the local attack cooldown when the server says a Cleave readied
 * the next swing.
 */
@Environment(EnvType.CLIENT)
public final class FeatKeyClient {
    public static final String KEY = "key.dnd-classes.feat-toggle";
    public static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(KEY,
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.dnd-classes.dnd-classes"));

    private FeatKeyClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    ClientPlayNetworking.send(FeatPackets.C2S_TOGGLE, PacketByteBufs.empty());
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(FeatPackets.S2C_CLEAVE, (client, handler, buf, sender) -> client
                .execute(() -> {
                    if (client.player != null) {
                        ((LivingEntityAccessor) client.player)
                                .dnd$setLastAttackedTicks(FeatEffects.CLEAVE_COOLDOWN_TICKS);
                    }
                }));
    }
}
