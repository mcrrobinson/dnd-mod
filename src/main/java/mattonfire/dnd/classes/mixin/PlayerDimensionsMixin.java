package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.RaceSize;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Racial hitboxes and eye heights ({@link RaceSize}). Identity's own {@code getDimensions} and
 * {@code getActiveEyeHeight} injects return early at HEAD while the player has a form, so these RETURN
 * injects never see a Wild Shape; {@link RaceSize#body} checks for a form as well.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerDimensionsMixin extends LivingEntity {
    /** The body race the current dimensions were calculated for, to spot a change on the client. */
    @Unique
    private DndRace dnd$sizedFor = DndRace.NONE;

    protected PlayerDimensionsMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void dnd$raceDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        cir.setReturnValue(RaceSize.dimensions((PlayerEntity) (Object) this, cir.getReturnValue()));
    }

    @Inject(method = "getActiveEyeHeight", at = @At("RETURN"), cancellable = true)
    private void dnd$raceEyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(RaceSize.eyeHeight((PlayerEntity) (Object) this, cir.getReturnValue()));
    }

    /**
     * Clients learn other players' races (and their own) from DataTracker updates: recalculate the hitbox
     * when the body race arrives or changes. The server recalculates in {@code setBodyRace}.
     */
    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        DndRace body = ((PlayerEntityExt) this).getBodyRace();
        if (body != this.dnd$sizedFor) {
            this.dnd$sizedFor = body;
            this.calculateDimensions();
        }
    }
}
