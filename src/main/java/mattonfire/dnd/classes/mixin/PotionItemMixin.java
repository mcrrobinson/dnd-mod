package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import mattonfire.dnd.classes.PotionImmunity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.world.World;

// Drinking a potion applies potion-sourced effects.
@Mixin(PotionItem.class)
public class PotionItemMixin {
    @WrapMethod(method = "finishUsing")
    private ItemStack dnd$markPotionSource(ItemStack stack, World world, LivingEntity user,
            Operation<ItemStack> original) {
        PotionImmunity.begin();
        try {
            return original.call(stack, world, user);
        } finally {
            PotionImmunity.end();
        }
    }
}
