package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.PlayerEntityExt;

@Mixin(Entity.class)
public abstract class LavaDamageMixin {

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void onLavaDamage(CallbackInfo ci) {
        // Cast this object to an Entity to access its methods
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof PlayerEntityExt))
            return;

        World world = entity.getEntityWorld();

        // Check if the entity is in lava

        if (entity.isInLava() && !world.isClient && (((PlayerEntityExt) entity).dndClassExist() == 8)) {

            DamageSources damageSources = world.getDamageSources();

            // Cool to add custom damage source to give funny message.
            entity.damage(damageSources.generic(), 20.F);
        }
    }
}
