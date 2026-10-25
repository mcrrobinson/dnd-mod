package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.magic.Corrosion;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {

    @Shadow
    @Final
    private Property levelCost;

    public AnvilScreenHandlerMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
            ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    // Repairing or combining corroded gear on an anvil scrapes the rust off (a rename alone doesn't)
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void clearCorrosion(CallbackInfo ci) {
        ItemStack result = this.output.getStack(0);
        ItemStack base = this.input.getStack(0);
        ItemStack addition = this.input.getStack(1);
        if (result.isEmpty() || addition.isEmpty() || Corrosion.level(result) <= 0) {
            return;
        }
        if (addition.isOf(base.getItem()) || base.getItem().canRepair(base, addition)) {
            Corrosion.clear(result);
        }
    }

    // Alchemists cannot apply enchanted books on an anvil
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void blockAlchemistEnchanting(CallbackInfo ci) {
        if (!(this.player instanceof PlayerEntityExt)
                || ((PlayerEntityExt) this.player).getDndClass() != DndCharacter.ALCHEMIST) {
            return;
        }

        ItemStack addition = this.input.getStack(1);
        if (!addition.isOf(Items.ENCHANTED_BOOK) || this.output.getStack(0).isEmpty()) {
            return;
        }

        this.output.setStack(0, ItemStack.EMPTY);
        this.levelCost.set(0);
        if (!this.player.getWorld().isClient) {
            this.player.sendMessage(Text.literal("Alchemists cannot enchant items.").formatted(Formatting.RED), true);
        }
    }
}
