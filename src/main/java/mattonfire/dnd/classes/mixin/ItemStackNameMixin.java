package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.magic.MagicNames;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

// Magic items: the rarity colour, "+N" prefix and "Unidentified <type>" name. The colour set here beats
// the vanilla rarity colour that getTooltip / InGameHud wrap around getName().
@Mixin(ItemStack.class)
public abstract class ItemStackNameMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void dnd$magicName(CallbackInfoReturnable<Text> cir) {
        Text name = MagicNames.decorateName((ItemStack) (Object) this, cir.getReturnValue());
        if (name != null)
            cir.setReturnValue(name);
    }
}
