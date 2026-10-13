package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** A player holding a loaded crossbow aims it; vanilla only checks for its own crossbow, this lets the Gnomish Repeater in. */
@Mixin(PlayerEntityRenderer.class)
public abstract class CrossbowArmPoseMixin {
    @Redirect(method = "getArmPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
    private static boolean dnd$anyCrossbow(ItemStack stack, Item item) {
        return item == Items.CROSSBOW ? stack.getItem() instanceof CrossbowItem : stack.isOf(item);
    }
}
