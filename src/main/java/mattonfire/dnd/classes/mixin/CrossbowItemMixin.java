package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.magic.MagicGear;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

// +N crossbows: +10% arrow damage per +1
@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {
    @Inject(method = "createArrow", at = @At("RETURN"))
    private static void dnd$magicArrowDamage(World world, LivingEntity entity, ItemStack crossbow, ItemStack arrow,
            CallbackInfoReturnable<PersistentProjectileEntity> cir) {
        MagicGear.applyArrowBonus(crossbow, cir.getReturnValue());
    }
}
