package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;

/** Bards are healed by poison: each poison tick heals 1 health instead of hurting. */
@Mixin(StatusEffect.class)
public class StatusEffectMixin {
    @Inject(at = @At("HEAD"), method = "applyUpdateEffect", cancellable = true)
    public void applyUpdateEffect(LivingEntity entity, int amplifier, CallbackInfo info) {
        if ((Object) this != StatusEffects.POISON || entity.getWorld().isClient
                || !(entity instanceof PlayerEntityExt ext) || ext.getDndClass() != DndCharacter.BARD) {
            return;
        }
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(1.0F);
        }
        info.cancel();
    }
}
