package mattonfire.dnd.classes.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Progression.Classes.PaladinSkills;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;

/** Status effects a subclass feature makes a player immune to (the Devotion Paladin's Purity of Spirit). */
@Mixin(LivingEntity.class)
public abstract class SubclassEffectImmunityMixin {
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void dnd$blockSubclassImmunity(StatusEffectInstance effect, @Nullable Entity source,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player && !player.getWorld().isClient
                && PaladinSkills.blocksEffect(player, effect.getEffectType())) {
            cir.setReturnValue(false);
        }
    }
}
