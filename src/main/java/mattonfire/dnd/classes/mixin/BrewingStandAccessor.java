package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandAccessor {
    @Accessor("brewTime")
    int getBrewTime();

    @Accessor("inventory")
    DefaultedList<ItemStack> dnd$getInventory();

    @Accessor("itemBrewing")
    Item dnd$getItemBrewing();

    @Invoker("canCraft")
    static boolean dnd$canCraft(DefaultedList<ItemStack> slots) {
        throw new AssertionError();
    }
}
