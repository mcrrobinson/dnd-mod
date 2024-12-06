package mattonfire.dnd.classes.gui;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.SetPlayerClass;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;

@SuppressWarnings("unused")
public class GUIClient implements ClientModInitializer {
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
    }
}