package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "addStatusEffect", at = @At("HEAD"), cancellable = true)
    private void onAddStatusEffect(StatusEffectInstance effectInstance, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof PlayerEntity) {
            PlayerEntityExt playerEntity = (PlayerEntityExt) entity;
            switch (playerEntity.dndClassExist()) {
                case 7:
                    cir.setReturnValue(false); // Paladins are immune
                    break;
                case 12:
                    if (effectInstance.getEffectType() == ModEffects.FREEZE) {
                        cir.setReturnValue(false); // Prevent the effect from being applied
                    }
                    break;
                case 9:
                    if (effectInstance.getEffectType() == StatusEffects.POISON) {
                        cir.setReturnValue(false); // Prevent the effect from being applied
                    }
                    break;

                default:
                    break;
            }
        }
    }
}
