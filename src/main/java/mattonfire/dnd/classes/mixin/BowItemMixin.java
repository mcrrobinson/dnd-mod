package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import mattonfire.dnd.classes.PlayerEntityExt;

@Mixin(BowItem.class)
public class BowItemMixin {
    // @ModifyArgs(method = "Lnet/minecraft/item/BowItem;onStoppedUsing()V",
    // at = @At(
    // value = "INVOKE",
    // target =
    // "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;setProperties(Lnet/minecraft/entity/Entity;FFFFF)V")
    // )
    // private void injected(Args args) {
    // PlayerEntity playerEntity = args.get(0);
    // float a4 = args.get(4);
    // if(((PlayerEntityExt)playerEntity).dndClassExist()==8){
    // args.set(4, a4*3);
    // }
    // }
}
