package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.random.Random;

@Mixin(EnchantmentScreenHandler.class)
public interface EnchantmentScreenHandlerAccessor {
    @Accessor("random")
    Random getRandom(); // Expose the private 'random' field

    @Accessor("seed")
    Property getSeed(); // Expose the private 'seed' field

    @Accessor("inventory")
    Inventory getInventory(); // Expose the private 'player' field

    @Accessor("enchantmentPower")
    int[] getEnchantmentPower(); // Expose the private 'enchantmentPower' field

    @Accessor("context")
    ScreenHandlerContext getContext(); // Expose the private 'context' field
}
