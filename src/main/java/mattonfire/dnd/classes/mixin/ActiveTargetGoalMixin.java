package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import draylar.identity.api.PlayerHostility;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;

@Mixin(ActiveTargetGoal.class)
public abstract class ActiveTargetGoalMixin extends TrackTargetGoalMixin {

    @Shadow
    protected LivingEntity targetEntity;

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void ignoreBards(CallbackInfo ci) {
        if (this.mob instanceof Monster
                && this.targetEntity instanceof PlayerEntity) {
            PlayerEntity targetPlayer = (PlayerEntity) this.targetEntity;

            boolean hasHostility = PlayerHostility.hasHostility(targetPlayer);

            if (targetPlayer instanceof PlayerEntityExt) {
                PlayerEntityExt playerEntity = (PlayerEntityExt) targetPlayer;
                DndCharacter dndClass = playerEntity.getDndClass();
                if (dndClass == DndCharacter.BARD) {
                    // only cancel if the player does not have hostility (attacked the mob)
                    if (!hasHostility) {
                        this.stop();
                        ci.cancel();
                    }
                } else if (dndClass == DndCharacter.NECROMANCER && this.mob.getGroup().equals(EntityGroup.UNDEAD)) {
                    this.stop();
                    ci.cancel();
                }

            }

        }
    }

    @Override
    protected void identity_shouldContinue(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob instanceof Monster && this.targetEntity instanceof PlayerEntity) {
            PlayerEntity targetPlayer = (PlayerEntity) this.targetEntity;
            boolean hasHostility = PlayerHostility.hasHostility(targetPlayer);
            if (targetPlayer instanceof PlayerEntityExt) {
                PlayerEntityExt playerEntity = (PlayerEntityExt) targetPlayer;
                DndCharacter dndClass = playerEntity.getDndClass();
                if (dndClass == DndCharacter.BARD) {
                    // only cancel if the player does not have hostility (attacked the mob)
                    if (!hasHostility) {
                        cir.setReturnValue(false);
                    }
                }

            }
        }
    }
}
