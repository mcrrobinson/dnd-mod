package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import mattonfire.dnd.classes.PotionImmunity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.util.hit.HitResult;

// Splash potions apply potion-sourced effects when they land.
@Mixin(PotionEntity.class)
public class PotionEntityMixin {
    @WrapMethod(method = "onCollision")
    private void dnd$markPotionSource(HitResult hitResult, Operation<Void> original) {
        PotionImmunity.begin();
        try {
            original.call(hitResult);
        } finally {
            PotionImmunity.end();
        }
    }
}
