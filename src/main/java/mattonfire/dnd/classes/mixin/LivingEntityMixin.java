package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.world.World;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    /**
     * Intercept the second argument ("float amount") whenever
     * LivingEntity.applyDamage(...) is called
     * inside LivingEntity.damage(...). We then modify the amount based on our
     * conditions.
     */
    @ModifyArg(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"), index = 1 // float
                                                                                                                                                                                 // amount
                                                                                                                                                                                 // is
                                                                                                                                                                                 // the
                                                                                                                                                                                 // second
                                                                                                                                                                                 // parameter
                                                                                                                                                                                 // (index=1)
    )
    private float modifyDamageAmount(DamageSource source, float originalAmount) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof PlayerEntity player) {

            if (attacker instanceof PlayerEntityExt playerEntity) {
                if (playerEntity.getDndClass() != DndCharacter.BLOODHUNTER) {
                    return originalAmount;
                }
            }

            // Check if it's night (time between 13000 and 23000)
            long time = player.getWorld().getTimeOfDay() % 24000;
            boolean isNight = time >= 13000 && time <= 23000;

            // Check if using a sword
            if (player.getMainHandStack().getItem() instanceof SwordItem) {
                if (isNight) {
                    return originalAmount * 2.0F;
                } else {
                    return originalAmount * 0.5F;
                }
            }
        }
        return originalAmount;
    }

    @Inject(method = "addStatusEffect", at = @At("HEAD"), cancellable = true)
    private void onAddStatusEffect(StatusEffectInstance effectInstance, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof PlayerEntity) {
            if (entity instanceof PlayerEntityExt) {
                PlayerEntityExt playerEntity = (PlayerEntityExt) entity;

                // TODO: This isn't a permanant solution. We should never really allow the user
                // to spawn without a DND Class.
                if (playerEntity.getDndClass() == null) {
                    return;
                }
                switch (playerEntity.getDndClass()) {
                    case PALADIN:
                        cir.setReturnValue(false); // Paladins are immune
                        break;
                    case WIZARD:
                        if (effectInstance.getEffectType() == ModEffects.FREEZE) {
                            cir.setReturnValue(false); // Prevent the effect from being applied
                        }
                        break;
                    case ROGUE:
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
}
