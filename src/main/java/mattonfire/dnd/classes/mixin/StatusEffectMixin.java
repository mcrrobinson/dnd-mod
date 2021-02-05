package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(StatusEffect.class)
public class StatusEffectMixin {
    @Inject(
		at = @At("HEAD"), 
        method = "applyUpdateEffect",
        cancellable = true
	)
    public void applyUpdateEffect(LivingEntity entity, int amplifier, CallbackInfo info) 
    {
        if((StatusEffect)(Object)this == StatusEffects.POISON)
        {
            System.out.println("Poisoned...");
            if(entity instanceof PlayerEntity)
            {
                System.out.println("Player was poisoned...");
                if(((PlayerEntityExt)(PlayerEntity)entity).dndClassExist()==2)
                {
                    System.out.println("Player with the right class was poisoned...");
                    if (entity.getHealth() < entity.getMaxHealth())
                    {
                        entity.heal(1.f);
                    }
                    info.cancel();
                }
            }
        }
    }
}