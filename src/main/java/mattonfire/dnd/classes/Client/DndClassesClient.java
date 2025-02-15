package mattonfire.dnd.classes.client;

import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.SetPlayerClass;
import mattonfire.dnd.classes.Misc.DoubleJumpEffect;
import mattonfire.dnd.classes.client.Hud.AchievementMenu;
import mattonfire.dnd.classes.client.Hud.PowerupOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import mattonfire.dnd.classes.client.Render.Color;

public class DndClassesClient implements ClientModInitializer {
    public static final MinecraftClient MC = MinecraftClient.getInstance();
    public static Color chestESPColor = new Color(1, 1, 0, 1);

    private static final KeyBinding OPEN_MENU_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.achievement_menu.open", GLFW.GLFW_KEY_O, "category.achievement_menu"));

    private static void handleClassQuery(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        int classID = buf.readInt();
        client.execute(() -> {
            if (client.player instanceof PlayerEntityExt) {
                ((PlayerEntityExt) (PlayerEntity) client.player).setDndClass(DndCharacter.fromValue(classID));
                SetPlayerClass.setPlayerClass(client, client.player, classID);
            }

        });
    }

    private static void receiveDoubleJumpEffectsRequest(MinecraftClient client, ClientPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender responseSender) {
        client.execute(() -> {
            PlayerEntity effectPlayer = client.player.getEntityWorld().getPlayerByUuid(buf.readUuid());
            if (effectPlayer != null) {
                DoubleJumpEffect.play(client.player, effectPlayer);
            }
        });
    }

    private static void receiveLungeEffectsRequest(MinecraftClient client, ClientPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender responseSender) {
        UUID effectPlayerUuid = buf.readUuid();
        client.execute(() -> {
            PlayerEntity effectPlayer = client.player.getEntityWorld().getPlayerByUuid(effectPlayerUuid);
            if (effectPlayer != null) {
                DoubleJumpEffect.play(client.player, effectPlayer);
            }
        });
    }

    private static void setMana(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        int amount = buf.readInt();

        client.execute(() -> { // Ensure this runs on the main client thread
            if (client.player != null) {
                System.out.println("Setting mana to " + amount);
                ((IEntityDataSaver) client.player).getPersistentData().putInt("mana", amount);
            } else {
                System.out.println("Player is not yet initialized, deferring mana update...");
            }
        });
    }

    private static void removeManor(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        if (client.player != null) {
            ((IEntityDataSaver) client.player).getPersistentData().putInt("mana", 0);
        }
    }

    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU_KEY.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AchievementMenu());
                }
            }

            // Increase their manor

        });

        // WorldRenderEvents.END.register(context -> {
        // for (Line line : RenderUtils.lineToRenderList) {
        // line.Draw(context);
        // }
        // });

        // ClientTickEvents.END_CLIENT_TICK.register(client -> {
        // if (client.player != null) {
        // Vec3d pos = client.player.getEyePos();
        // RenderUtils.lineToRenderList.clear();
        // RenderUtils.drawCircleAtPos(pos, chestESPColor, 3, 36);
        // }
        // });

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

                // Power effect request
                PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                passedData.writeUuid(client.player.getUuid());
                ClientPlayNetworking.send(DnDClasses.C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID, passedData);
            }
        });

        // The response from the server to make the special effects.
        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_DOUBLEJUMP_EFFECTS_PACKET_ID,
                DndClassesClient::receiveDoubleJumpEffectsRequest);

        // The response from the server to make the special effects.
        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_LUNGE_EFFECTS_PACKET_ID,
                DndClassesClient::receiveLungeEffectsRequest);

        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_SYNC_MANA,
                DndClassesClient::setMana);

        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_POWERUP_EFFECTS_PACKET_ID,
                DndClassesClient::removeManor);

        HudRenderCallback.EVENT.register(new PowerupOverlay());
    }

}