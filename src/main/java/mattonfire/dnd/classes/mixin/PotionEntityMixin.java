package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import mattonfire.dnd.classes.PotionImmunity;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.util.hit.HitResult;

// Splash potions apply potion-sourced effects when they land, and Potions of Healing douse Holy Wards.
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
        PotionEntity self = (PotionEntity) (Object) this;
        if (self.getWorld() instanceof ServerWorld world) {
            ObstacleTypes.HOLY_WARD.onPotionBurst(world, hitResult.getPos(), self.getStack(), self.getOwner());
        }
    }
}
