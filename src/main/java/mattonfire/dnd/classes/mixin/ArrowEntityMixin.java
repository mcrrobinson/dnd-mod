package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import mattonfire.dnd.classes.PotionImmunity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;

// Tipped arrows apply potion-sourced effects on hit.
@Mixin(ArrowEntity.class)
public class ArrowEntityMixin {
    @WrapMethod(method = "onHit")
    private void dnd$markPotionSource(LivingEntity target, Operation<Void> original) {
        PotionImmunity.begin();
        try {
            original.call(target);
        } finally {
            PotionImmunity.end();
        }
    }
}
