package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mattonfire.dnd.classes.Feats.FeatArrows;
import mattonfire.dnd.magic.MagicGear;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

// +N crossbows: +10% arrow damage per +1. Sharpshooter: crossbow shots are always charged, so Steady Aim applies.
@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {
    @Inject(method = "createArrow", at = @At("RETURN"))
    private static void dnd$magicArrowDamage(World world, LivingEntity entity, ItemStack crossbow, ItemStack arrow,
            CallbackInfoReturnable<PersistentProjectileEntity> cir) {
        MagicGear.applyArrowBonus(crossbow, cir.getReturnValue());
        FeatArrows.mark(cir.getReturnValue(), entity, true);
        FeatArrows.applySteadyDamage(cir.getReturnValue());
    }

    // Steady Aim spreads the shot wider
    @WrapOperation(method = "shoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileEntity;setVelocity(DDDFF)V"))
    private static void dnd$steadyAimSpread(ProjectileEntity projectile, double x, double y, double z, float speed,
            float divergence, Operation<Void> original) {
        original.call(projectile, x, y, z, speed, FeatArrows.divergence(projectile, divergence));
    }
}
