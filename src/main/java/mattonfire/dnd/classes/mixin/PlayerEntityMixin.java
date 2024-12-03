package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.ExtendedSwordItem;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.FluidTags;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("rawtypes")
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends Entity implements PlayerEntityExt {
    private ItemStack selectedItem;
    boolean dropEntireStack;
    private int dndClass = 0;

    public PlayerEntityMixin(EntityType<?> type, World world) {
        super(type, world);
        // TODO Auto-generated constructor stub
    }

    public void setDndClass(int classID) {
        this.dndClass = classID;
        System.out.println(this.dndClass);
    }

    public int dndClassExist() {
        return this.dndClass;
    }

    @Inject(at = @At("HEAD"), method = "dropSelectedItem")
    private void dropSelectedItem(boolean dropEntireStack, CallbackInfoReturnable info) {
        System.out.println("Item dropped");
        if (this.selectedItem.getItem() instanceof ExtendedSwordItem) {
            System.out.println("Extended Dong dropped");
            ((ExtendedSwordItem) this.selectedItem.getItem()).SetRange(false);
            ExtendedSwordItem.active = false;
        }
    }

    // Some creatures can't swim... here is that. Probably a better way of doing
    // it.
    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo info) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (((PlayerEntityExt) player).dndClassExist() == 4) {
            if (isSubmergedIn(FluidTags.WATER) && !player.isCreative() && !player.abilities.flying) {
                this.setVelocity(0.0D, -0.5, 0.0D);
            }
        }
    }
}