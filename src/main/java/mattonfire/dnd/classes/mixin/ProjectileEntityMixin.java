package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.Misc.Returning;
import mattonfire.dnd.classes.Progression.Classes.RogueSkills;
import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.entity.Entity;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.util.math.Vec3d;

/**
 * Rogue's Danger Sense: a projectile can't hit a player who has the effect, so arrows,
 * tridents, fireballs and shulker bullets fly through them. Every projectile's entity
 * collision goes through {@code canHit} (subclasses call super). The first time one
 * would really have hit (its path this tick crosses the player's box), the Rogue sidesteps.
 *
 * Returning throwables (snowballs, eggs, ender pearls...): every thrown-item
 * projectile calls {@code super.onCollision} before its own effect, so the item
 * is handed back here and the projectile's normal effect still happens.
 */
@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
    @Unique
    private boolean dnd$returned;

    @Unique
    private boolean dnd$dodged;

    @Inject(method = "canHit(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void dnd$dangerSense(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof PlayerEntity player) || !player.hasStatusEffect(ModEffects.DANGER_SENSE)) {
            return;
        }
        cir.setReturnValue(false);
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        if (dnd$dodged || self.world.isClient) {
            return;
        }
        // canHit is asked of everything near the path, so only react to a real hit.
        Vec3d from = self.getPos();
        Vec3d to = from.add(self.getVelocity());
        if (player.getBoundingBox().expand(0.3).raycast(from, to).isPresent()
                || player.getBoundingBox().intersects(self.getBoundingBox())) {
            dnd$dodged = true;
            RogueSkills.sidestep(player, self);
        }
    }

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
