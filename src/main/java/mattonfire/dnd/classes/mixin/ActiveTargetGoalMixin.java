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
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Misc.BloodHunterControl;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.entity.LichEntity;
import mattonfire.dnd.dm.DungeonMaster;

@Mixin(ActiveTargetGoal.class)
public abstract class ActiveTargetGoalMixin extends TrackTargetGoalMixin {

    @Shadow
    protected LivingEntity targetEntity;

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void ignoreBards(CallbackInfo ci) {
        // Players in DM mode (veiled or not) are never targets.
        if (DungeonMaster.isDm(this.targetEntity)) {
            this.stop();
            ci.cancel();
            return;
        }
        if (this.mob instanceof Monster
                && this.targetEntity instanceof PlayerEntity) {
            PlayerEntity targetPlayer = (PlayerEntity) this.targetEntity;

            boolean hasHostility = BloodHunterControl.hasIdentityHostility(targetPlayer);

            if (targetPlayer instanceof PlayerEntityExt) {
                PlayerEntityExt playerEntity = (PlayerEntityExt) targetPlayer;
                DndCharacter dndClass = playerEntity.getDndClass();
                if (dndClass == DndCharacter.BARD) {
                    // only cancel if the player does not have hostility (attacked the mob)
                    if (!hasHostility) {
                        this.stop();
                        ci.cancel();
                    }
                } else if (dndClass == DndCharacter.NECROMANCER && this.mob.getGroup().equals(EntityGroup.UNDEAD)
                        && !LichEntity.defiesNecromancers(this.mob)) {
                    this.stop();
                    ci.cancel();
                }

            }

        }
    }

    @Override
    protected void identity_shouldContinue(CallbackInfoReturnable<Boolean> cir) {
        if (DungeonMaster.isDm(this.targetEntity)) {
            cir.setReturnValue(false);
            return;
        }
        if (this.mob instanceof Monster && this.targetEntity instanceof PlayerEntity) {
            PlayerEntity targetPlayer = (PlayerEntity) this.targetEntity;
            boolean hasHostility = BloodHunterControl.hasIdentityHostility(targetPlayer);
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
