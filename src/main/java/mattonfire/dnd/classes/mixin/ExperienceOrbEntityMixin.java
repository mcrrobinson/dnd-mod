package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mattonfire.dnd.classes.Party.PartyEvents;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbEntityMixin {

    // Party XP sharing: what's left of a picked-up orb after Mending is split
    // between the collector and their nearby party members.
    @WrapOperation(method = "onPlayerCollision", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;addExperience(I)V"))
    private void dnd$sharePartyXp(PlayerEntity player, int amount, Operation<Void> original) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            PartyEvents.shareXp(serverPlayer, amount, PartyEvents.XP_VANILLA,
                    (member, xp) -> original.call(member, xp));
        } else {
            original.call(player, amount);
        }
    }
}
