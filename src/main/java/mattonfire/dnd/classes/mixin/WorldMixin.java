package mattonfire.dnd.classes.mixin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.entity.DragonPart;
import mattonfire.dnd.entity.DragonPartTracker;
import mattonfire.dnd.entity.MultipartDragon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * Makes dragon parts visible to hit detection (crosshair targeting, projectiles, sweeps), the same
 * way vanilla adds ender dragon parts to getOtherEntities.
 */
@Mixin(World.class)
public abstract class WorldMixin implements DragonPartTracker {
    @Unique
    private final Set<MultipartDragon> dnd$dragons = new HashSet<>();

    @Override
    public Set<MultipartDragon> dnd$getDragons() {
        return this.dnd$dragons;
    }

    @Inject(method = "getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("RETURN"))
    private void dnd$addDragonParts(Entity except, Box box, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
        if (this.dnd$dragons.isEmpty()) {
            return;
        }
        // A dragon's own projectiles shouldn't collide with its own head/wings
        Entity shooter = except instanceof ProjectileEntity projectile ? projectile.getOwner() : null;
        List<Entity> result = cir.getReturnValue();
        for (MultipartDragon dragon : this.dnd$dragons) {
            if (dragon == except || dragon == shooter) {
                continue;
            }
            // Broad phase: skip dragons whose parts can't reach the query box
            if (dragon instanceof Entity owner) {
                double reach = dragon.getPartLayout().getReach();
                Box own = owner.getBoundingBox();
                if (own.maxX + reach < box.minX || own.minX - reach > box.maxX
                        || own.maxY + reach < box.minY || own.minY - reach > box.maxY
                        || own.maxZ + reach < box.minZ || own.minZ - reach > box.maxZ) {
                    continue;
                }
            }
            for (DragonPart part : dragon.getParts()) {
                if (part.getBoundingBox().intersects(box) && predicate.test(part)) {
                    result.add(part);
                }
            }
        }
    }
}
