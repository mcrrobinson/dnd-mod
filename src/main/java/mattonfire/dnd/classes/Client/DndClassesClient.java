package mattonfire.dnd.classes.Client;

import org.lwjgl.glfw.GLFW;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.SetPlayerClass;
import mattonfire.dnd.classes.Registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public class DndClassesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        // Once server joined, if joined before it will set their old class.
        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_CLASS_QUERY_PACKET_ID,
                (client, handler, buf, responseSender) -> {
                    int classID = buf.readInt();
                    client.execute(() -> {
                        ((PlayerEntityExt) (PlayerEntity) client.player).setDndClass(classID);
                        SetPlayerClass.setPlayerClass(client, client.player, classID);
                    });
                });

        // Once a class is requested and approved this will set the player class.
        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_APPROVE_CLASS_PICK_PACKET_ID,
                (client, handler, buf, responseSender) -> {
                    int classID = buf.readInt();
                    client.execute(() -> {
                        ((PlayerEntityExt) (PlayerEntity) client.player).setDndClass(classID);
                        SetPlayerClass.setPlayerClass(client, client.player, classID);
                    });
                });

        KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dnd-classes.power-up", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_Z, // The keycode of the key
                "category.dnd-classes.dnd-classes" // The translation key of the keybinding's category.
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                client.player.sendMessage(Text.literal("Imagine Anime power up noises..."), false);
                PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                passedData.writeUuid(client.player.getUuid());
                ClientPlayNetworking.send(DnDClasses.C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID, passedData);
            }
        });
    }
}