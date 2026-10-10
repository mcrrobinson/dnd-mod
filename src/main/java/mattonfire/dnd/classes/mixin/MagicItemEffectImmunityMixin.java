package mattonfire.dnd.classes.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.magic.items.BossItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Status effects a magic item makes its bearer immune to: Frostbrand (Freeze), Eye of the Tyrant (anti-magic). */
@Mixin(LivingEntity.class)
public abstract class MagicItemEffectImmunityMixin {
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void dnd$blockMagicItemImmunity(StatusEffectInstance effect, @Nullable Entity source,
            CallbackInfoReturnable<Boolean> cir) {
        if (BossItems.blocksEffect((LivingEntity) (Object) this, effect.getEffectType())) {
            cir.setReturnValue(false);
        }
    }
}
