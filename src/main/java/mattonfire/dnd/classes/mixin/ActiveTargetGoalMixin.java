package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Misc.BardCompanions;
import mattonfire.dnd.classes.Misc.BloodHunterControl;
import mattonfire.dnd.classes.Misc.ClericHandler;
import mattonfire.dnd.classes.Obstacles.types.GuardianBeastType;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.entity.LichEntity;
import mattonfire.dnd.dm.DungeonMaster;

@Mixin(ActiveTargetGoal.class)
public abstract class ActiveTargetGoalMixin extends TrackTargetGoalMixin {

    @Shadow
    protected LivingEntity targetEntity;

    /**
     * Players this mob must never pick: DMs (veiled or not), Bards it has no grudge against, and Necromancers
     * for undead. They are filtered out of the candidate search itself, so the goal just picks the next one (or
     * nothing). Refusing them later, in {@code start()}, would run {@code stop()}, and that clears whatever
     * target the mob already had: a DM standing closer than a zombie's villager made it drop the villager.
     */
    @ModifyArg(method = "<init>(Lnet/minecraft/entity/mob/MobEntity;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/TargetPredicate;setPredicate(Ljava/util/function/Predicate;)Lnet/minecraft/entity/ai/TargetPredicate;"))
    private Predicate<LivingEntity> dndclasses$skipUnpickable(@Nullable Predicate<LivingEntity> predicate) {
        Predicate<LivingEntity> pickable = target -> !this.dndclasses$neverPick(target);
        return predicate == null ? pickable : predicate.and(pickable);
    }

    /** Also covers a target handed over through {@code setTargetEntity}: the goal doesn't start for it at all. */
    @Inject(method = "canStart", at = @At("RETURN"), cancellable = true)
    private void dndclasses$refuseUnpickable(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && this.dndclasses$neverPick(this.targetEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean dndclasses$neverPick(@Nullable LivingEntity target) {
        // Players in DM mode (veiled or not) are never targets.
        if (DungeonMaster.isDm(target)) {
            return true;
        }
        // The same players MobEntityMixin keeps out of setTarget (a Cleric's circle, a Bard companion's friends,
        // a calmed guardian beast). That refusal swaps the target for null, so it would clear the old one too.
        if (ClericHandler.isRepellingMobs(target) || BardCompanions.blocksTarget(this.mob, target)
                || GuardianBeastType.blocksTarget(this.mob, target)) {
            return true;
        }
        if (this.mob instanceof Monster && target instanceof PlayerEntityExt playerEntity) {
            DndCharacter dndClass = playerEntity.getDndClass();
            if (dndClass == DndCharacter.BARD) {
                // Bards are left alone unless they attacked the mob
                return !BloodHunterControl.hasIdentityHostility((PlayerEntity) target);
            }
            return dndClass == DndCharacter.NECROMANCER && this.mob.getGroup().equals(EntityGroup.UNDEAD)
                    && !LichEntity.defiesNecromancers(this.mob);
        }
        return false;
    }

    @Override
    protected void identity_shouldContinue(CallbackInfoReturnable<Boolean> cir) {
        // Vanilla keeps going on the mob's current target, falling back to the one this goal picked.
        LivingEntity current = this.mob.getTarget() != null ? this.mob.getTarget() : this.targetEntity;
        if (DungeonMaster.isDm(current)) {
            cir.setReturnValue(false);
            return;
        }
        if (this.mob instanceof Monster && current instanceof PlayerEntity) {
            PlayerEntity targetPlayer = (PlayerEntity) current;
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
