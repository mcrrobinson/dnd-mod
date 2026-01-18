package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.EnchantmentScreenHandler;

@Mixin(EnchantmentScreenHandler.class)
public interface EnchantmentScreenHandlerInvoker {
    @Invoker("onContentChanged")
    void invokeOnContentChanged(Inventory inventory);
}
