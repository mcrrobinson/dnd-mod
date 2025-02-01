package mattonfire.dnd.classes.client;

import org.lwjgl.glfw.GLFW;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.SetPlayerClass;
import mattonfire.dnd.classes.client.Hud.AchievementMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public class DndClassesClient implements ClientModInitializer {

    private static final KeyBinding OPEN_MENU_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.achievement_menu.open", GLFW.GLFW_KEY_O, "category.achievement_menu"));

    private static void handleClassQuery(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        int classID = buf.readInt();
        client.execute(() -> {
            ((PlayerEntityExt) (PlayerEntity) client.player).setDndClass(DndCharacter.fromValue(classID));
            SetPlayerClass.setPlayerClass(client, client.player, classID);
        });
    }

    @Override
    public void onInitializeClient() {

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU_KEY.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AchievementMenu());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_CLASS_QUERY_PACKET_ID,
                DndClassesClient::handleClassQuery);

        // Once a class is requested and approved this will set the player class.
        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_APPROVE_CLASS_PICK_PACKET_ID,
                DndClassesClient::handleClassQuery);

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