package mattonfire.dnd.classes.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.SetPlayerClass;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
	@Inject(
		at = @At("RETURN"), 
		method = "respawnPlayer"
	)
	public void respawnPlayer(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable info)
	{
		PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
		if(alive)
		{
			int classID = ((PlayerEntityExt)(PlayerEntity)player).dndClassExist();
			passedData.writeInt(classID);
			if(classID!=0){
				SetPlayerClass.setPlayerClass(null, player, classID);
			}
			ServerPlayNetworking.send(player, DnDClasses.S2C_CLASS_QUERY_PACKET_ID, passedData);
		} else {
			passedData.writeInt(0);
			ServerPlayNetworking.send(player, DnDClasses.S2C_CLASS_QUERY_PACKET_ID, passedData);
		}
	}
}