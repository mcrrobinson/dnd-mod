package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import mattonfire.dnd.classes.PlayerEntityExt;

@Mixin(BowItem.class)
public class BowItemMixin {

    @ModifyArgs(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;setProperties(Lnet/minecraft/entity/Entity;FFFFF)V"))
    private void modifyArrowProperties(Args args) {
        Entity user = args.get(0); // The entity using the bow
        if (user instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity) user;

            float speed = args.get(4); // Get the arrow speed
            if (((PlayerEntityExt) playerEntity).dndClassExist() == 8) { // Custom logic
                args.set(4, speed * 3); // Modify the speed argument
            }
        }
    }
}