package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Race.RacePassives;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;

/** Dwarven Resilience: a Dwarf's poison ticks deal half damage ({@link RacePassives#poisonTick}). */
@Mixin(StatusEffect.class)
public class RacePoisonMixin {
    @Inject(method = "applyUpdateEffect", at = @At("HEAD"), cancellable = true)
    private void dnd$dwarvenResilience(LivingEntity entity, int amplifier, CallbackInfo ci) {
        if ((Object) this == StatusEffects.POISON && RacePassives.poisonTick(entity)) {
            ci.cancel();
        }
    }
}
