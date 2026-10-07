package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.Misc.Returning;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Returning tridents go straight back into the thrower's inventory once they've
 * hit something (a mob or a block) or fallen out of the world.
 */
@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends PersistentProjectileEntity {
    @Shadow
    private ItemStack tridentStack;
    @Shadow
    private boolean dealtDamage;

    protected TridentEntityMixin(EntityType<? extends PersistentProjectileEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void dnd$returning(CallbackInfo ci) {
        if (this.world.isClient || !Returning.hasReturning(this.tridentStack)) {
            return;
        }
        boolean landed = this.dealtDamage || this.inGround || this.getY() < this.world.getBottomY();
        if (!landed) {
            return;
        }
        // In creative the trident never left the player's inventory.
        boolean giveItem = this.pickupType == PickupPermission.ALLOWED;
        PlayerEntity player = Returning.returnTarget(this, this.getOwner());
        if (player == null) {
            return;
        }
        Returning.giveBack(player, this.tridentStack, giveItem, SoundEvents.ITEM_TRIDENT_RETURN, 1.0f, 1.0f);
        this.discard();
        ci.cancel();
    }
}
