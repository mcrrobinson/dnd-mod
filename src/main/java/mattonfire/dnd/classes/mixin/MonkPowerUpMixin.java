package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Misc.PowerUpEffect;
import mattonfire.dnd.classes.Progression.Classes.MonkSkills;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;

/**
 * PowerUpEffect has no monk case (the power-up used to do nothing), so the
 * monk's root skill, Ki Surge, is hooked in here. Can be replaced by a
 * {@code case MONK} in PowerUpEffect.
 */
@Mixin(value = PowerUpEffect.class, remap = false)
public abstract class MonkPowerUpMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private static void monkKiSurge(MinecraftServer server, PlayerEntity player, DndCharacter character,
            CallbackInfoReturnable<Boolean> cir) {
        if (character == DndCharacter.MONK) {
            MonkSkills.kiSurge(player);
            cir.setReturnValue(true);
        }
    }
}
