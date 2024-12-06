package mattonfire.dnd.classes.DoubleJump;

import mattonfire.dnd.classes.DnDClasses;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;

@SuppressWarnings("unused")
public class DoubleJumpClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_DOUBLEJUMP_EFFECTS_PACKET_ID,
                (client, handler, buf, responseSender) -> {
                    client.execute(() -> {
                        PlayerEntity effectPlayer = client.player.getEntityWorld().getPlayerByUuid(buf.readUuid());
                        if (effectPlayer != null) {
                            DoubleJumpEffect.play(effectPlayer, effectPlayer);
                        }
                    });
                });
    }
}