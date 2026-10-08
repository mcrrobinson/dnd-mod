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
import mattonfire.dnd.classes.Items.ClassGuidebook;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.SetPlayerClass;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
	@Inject(at = @At("RETURN"), method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V")
	public void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo info) {
		PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
		if (!(player instanceof PlayerEntityExt)) {
			return;
		}

		DndCharacter playerClass = ((PlayerEntityExt) player).getDndClass();
		if (playerClass == null) {
			playerClass = DndCharacter.NONE;
		}

		int classID = playerClass.getValue();
		passedData.writeInt(classID);

		// On rejoin set the player class server side.
		if (playerClass != DndCharacter.NONE) {
			SetPlayerClass.setPlayerClass(null, player, classID);
		}

		// Send the result to the client.
		ServerPlayNetworking.send(player, DnDClasses.S2C_CLASS_QUERY_PACKET_ID, passedData);
	}

	/**
	 * Respawning (after death or leaving the End) makes a new player entity, so
	 * carry the class over and re-apply its stats instead of reopening the picker.
	 */
	@Inject(at = @At("RETURN"), method = "respawnPlayer")
	public void respawnPlayer(ServerPlayerEntity player, boolean alive,
			CallbackInfoReturnable<ServerPlayerEntity> info) {
		ServerPlayerEntity newPlayer = info.getReturnValue();
		if (!(player instanceof PlayerEntityExt oldExt) || !(newPlayer instanceof PlayerEntityExt newExt)) {
			return;
		}

		DndCharacter playerClass = oldExt.getDndClass();
		if (playerClass == null) {
			playerClass = DndCharacter.NONE;
		}
		int classID = playerClass.getValue();

		if (playerClass != DndCharacter.NONE) {
			float health = player.getHealth();
			newExt.setDndClass(playerClass);
			SetPlayerClass.setPlayerClass(null, newPlayer, classID);
			// The new entity got its health while it still had vanilla max health.
			newPlayer.setHealth(alive ? Math.min(health, newPlayer.getMaxHealth()) : newPlayer.getMaxHealth());
			ClassGuidebook.giveIfMissing(newPlayer);
			if (!alive) {
				DnDClasses.sendRespawnHint(newPlayer);
			}
		} else if (alive) {
			return;
		}

		PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
		passedData.writeInt(classID);
		ServerPlayNetworking.send(newPlayer, DnDClasses.S2C_CLASS_QUERY_PACKET_ID, passedData);
	}
}
