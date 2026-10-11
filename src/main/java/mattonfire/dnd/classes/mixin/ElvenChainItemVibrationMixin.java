package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Items.racial.SilencedVibrations;
import net.minecraft.entity.ItemEntity;

/**
 * Elven Chain: {@code ItemEntity} overrides {@code occludeVibrationSignals} (wool items dampen) without
 * calling {@code Entity}'s, so the drops of a block a sneaking Elf broke are silenced here as well.
 */
@Mixin(ItemEntity.class)
public abstract class ElvenChainItemVibrationMixin {
    @Inject(method = "occludeVibrationSignals", at = @At("HEAD"), cancellable = true)
    private void dnd$elvenChainSilentDrop(CallbackInfoReturnable<Boolean> cir) {
        if (((SilencedVibrations) this).dnd$vibrationsSilenced()) {
            cir.setReturnValue(true);
        }
    }
}
