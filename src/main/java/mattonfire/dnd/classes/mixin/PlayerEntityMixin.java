package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.ExtendedSwordItem;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(at = @At("HEAD"), method = "dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/ItemEntity;")
    private void onDropItem(ItemStack stack, boolean throwRandomly, CallbackInfoReturnable<ItemEntity> info) {
        System.out.println("Item dropped: " + stack.getItem());

        // Check if the dropped item is of your specific type
        if (stack.getItem() instanceof ExtendedSwordItem) {
            System.out.println("Extended Sword dropped");

            // Perform your custom logic
            ((ExtendedSwordItem) stack.getItem()).SetRange(false);
            ExtendedSwordItem.active = false;
        }
    }

    // Some creatures can't swim... here is that. Probably a better way of doing
    // it.
    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo info) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (((PlayerEntityExt) player).dndClassExist() == 4) {
            if (isSubmergedIn(FluidTags.WATER) && !player.isCreative() &&
                    !player.getAbilities().flying) {
                this.setVelocity(0.0D, -0.5, 0.0D);
            }
        }
    }
}