package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Downed.DownedEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

/** Settles a Downed player who logs out before their data is saved (dying if they weren't stable). */
@Mixin(PlayerManager.class)
public abstract class DownedPlayerManagerMixin {
    @Shadow
    public abstract MinecraftServer getServer();

    @Inject(method = "remove", at = @At("HEAD"))
    private void dnd$settleDowned(ServerPlayerEntity player, CallbackInfo ci) {
        DownedEvents.onLeave(player, getServer());
    }
}
