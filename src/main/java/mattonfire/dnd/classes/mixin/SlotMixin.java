package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

@Mixin(Slot.class)
public abstract class SlotMixin {
    // A helper to read the current stack in this slot
    @Shadow
    public abstract ItemStack getStack();

    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void onTakePotion(
            PlayerEntity player,
            ItemStack stack,
            CallbackInfo ci) {
        // 'this' is a Slot; we just cast it
        Slot slot = (Slot) (Object) this;
        Inventory inventory = slot.inventory;
        System.out.println("TAKING!");

        // Check if slot belongs to the Brewing Stand block-entity
        // or the brewing-stand container inventory
        if (inventory instanceof BrewingStandBlockEntity) {
            // Also check if the taken stack is a potion
            if (stack.getItem() instanceof PotionItem) {
                // Your custom logic here
                player.sendMessage(
                        Text.literal("You just took a potion from the brewing stand!"),
                        true);
            }
        }
    }
}
