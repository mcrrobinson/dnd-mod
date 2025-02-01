package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public class HungerManagerMixin {
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void disableHungerDepreciation(PlayerEntity player, CallbackInfo ci) {
        // Get the player's name
        String playerName = player.getName().getString();

        // Print to console for debugging (optional)
        // System.out.println("Hunger update called for player: " + playerName);

    }
}