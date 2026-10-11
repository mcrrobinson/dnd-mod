package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Items.racial.RacialGear;
import mattonfire.dnd.classes.Items.racial.SilencedVibrations;
import net.minecraft.entity.Entity;

/**
 * Elven Chain: a sneaking Elf in the full set occludes their vibrations, like an entity wrapped in wool, so
 * sculk sensors, shriekers and wardens ignore everything they do (steps, block breaks, hits, eating). The
 * items and experience a block they break drops are silenced for a few seconds too
 * ({@link RacialGear#onEntitySpawned}), so they don't set sensors off when they land. Item entities override
 * {@code occludeVibrationSignals}, so {@code ElvenChainItemVibrationMixin} checks the same there.
 */
@Mixin(Entity.class)
public abstract class ElvenChainVibrationMixin implements SilencedVibrations {
    @Unique
    private long dnd$silentUntil;

    @Override
    public void dnd$silenceVibrationsUntil(long gameTime) {
        this.dnd$silentUntil = gameTime;
    }

    @Override
    public boolean dnd$vibrationsSilenced() {
        return this.dnd$silentUntil != 0L && ((Entity) (Object) this).getWorld().getTime() < this.dnd$silentUntil;
    }

    @Inject(method = "occludeVibrationSignals", at = @At("HEAD"), cancellable = true)
    private void dnd$elvenChainSilence(CallbackInfoReturnable<Boolean> cir) {
        if (RacialGear.silencesVibrations((Entity) (Object) this) || this.dnd$vibrationsSilenced()) {
            cir.setReturnValue(true);
        }
    }
}
