package mattonfire.dnd.classes.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.SetPlayerClass;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
	@Inject(
		at = @At("RETURN"), 
		method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V"
	)
	public void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo info) {
		PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
		int classID = ((PlayerEntityExt)player).dndClassExist();
		passedData.writeInt(classID);

		// On rejoin set the player class server side.
		if(classID!=0){
			SetPlayerClass.setPlayerClass(null, player, classID);
		}

		// Send the result to the client.
		ServerPlayNetworking.send(player, DnDClasses.S2C_CLASS_QUERY_PACKET_ID, passedData);
	}

	@Inject(
		at = @At("RETURN"), 
		method = "respawnPlayer"
	)
	public void respawnPlayer(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable info)
	{
		if(!alive)
		{
			PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
			passedData.writeInt(0);
			ServerPlayNetworking.send(player, DnDClasses.S2C_CLASS_QUERY_PACKET_ID, passedData);
		}
	}
}