package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.Misc.Returning;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Returning throwables (snowballs, eggs, ender pearls...): every thrown-item
 * projectile calls {@code super.onCollision} before its own effect, so the item
 * is handed back here and the projectile's normal effect still happens.
 */
@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
    @Unique
    private boolean dnd$returned;

    @Inject(method = "onCollision", at = @At("HEAD"))
    private void dnd$returning(HitResult hitResult, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        if (dnd$returned || self.world.isClient || !(self instanceof ThrownItemEntity thrown)) {
            return;
        }
        ItemStack stack = thrown.getStack();
        if (!Returning.hasReturning(stack)) {
            return;
        }
        PlayerEntity player = Returning.returnTarget(self, self.getOwner());
        if (player == null) {
            return;
        }
        dnd$returned = true;
        // Creative players don't use up throwables, so there's nothing to give back.
        Returning.giveBack(player, stack, !player.getAbilities().creativeMode, SoundEvents.ENTITY_ITEM_PICKUP,
                0.4f, 1.6f);
    }
}
