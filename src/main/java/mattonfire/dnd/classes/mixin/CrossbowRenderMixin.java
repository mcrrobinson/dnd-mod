package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * First-person crossbow animations (loading, holding a loaded crossbow, which hand draws it) check for the vanilla
 * crossbow item only; this lets any {@link CrossbowItem} (the Gnomish Repeater) through those checks. Every other
 * {@code isOf} in the same methods is left alone.
 */
@Mixin(HeldItemRenderer.class)
public abstract class CrossbowRenderMixin {
    @Redirect(method = {"isChargedCrossbow", "getUsingItemHandRenderType", "getHandRenderType"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
    private static boolean dnd$anyCrossbowStatic(ItemStack stack, Item item) {
        return dnd$isOf(stack, item);
    }

    @Redirect(method = "renderFirstPersonItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
    private boolean dnd$anyCrossbow(ItemStack stack, Item item) {
        return dnd$isOf(stack, item);
    }

    private static boolean dnd$isOf(ItemStack stack, Item item) {
        return item == Items.CROSSBOW ? stack.getItem() instanceof CrossbowItem : stack.isOf(item);
    }
}
