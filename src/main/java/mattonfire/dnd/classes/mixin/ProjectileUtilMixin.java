package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import mattonfire.dnd.entity.DragonPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Crosshair targeting (raycast) and projectile hits (getEntityCollision) test each entity's bounding
 * box. For a dragon part that box is mostly air, so hand them the small shape the ray actually hits.
 */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {
    @WrapOperation(method = "raycast", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/Box;"))
    private static Box dnd$dragonPartShapeForCrosshair(Entity entity, Operation<Box> original,
                                                      @Local(argsOnly = true, ordinal = 0) Vec3d from, @Local(argsOnly = true, ordinal = 1) Vec3d to) {
        return entity instanceof DragonPart part ? part.shapeHitBy(from, to) : original.call(entity);
    }

    @WrapOperation(method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/Box;"))
    private static Box dnd$dragonPartShapeForProjectile(Entity entity, Operation<Box> original,
                                                       @Local(argsOnly = true, ordinal = 0) Vec3d from, @Local(argsOnly = true, ordinal = 1) Vec3d to) {
        return entity instanceof DragonPart part ? part.shapeHitBy(from, to) : original.call(entity);
    }
}
