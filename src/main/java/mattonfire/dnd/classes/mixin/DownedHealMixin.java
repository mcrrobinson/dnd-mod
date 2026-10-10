package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import mattonfire.dnd.classes.Downed.Revives;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;

/**
 * Regeneration and Instant Health heals count as outside heals, so they revive a Downed player (see
 * {@link Revives}). A Downed player's own Regeneration is removed when they go down.
 */
@Mixin(StatusEffect.class)
public class DownedHealMixin {
    @Redirect(method = { "applyUpdateEffect", "applyInstantEffect" }, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;heal(F)V"))
    private void dnd$outsideHeal(LivingEntity entity, float amount) {
        Revives.effectHeal(entity, amount);
    }
}
