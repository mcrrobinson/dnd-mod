package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.world.World;

/** Milk ends Poisoned (and only Poisoned: conditions aren't status effects). */
@Mixin(MilkBucketItem.class)
public abstract class ConditionMilkMixin {
    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void dnd$milkEndsPoisoned(ItemStack stack, World world, LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!world.isClient) {
            Conditions.remove(user, Condition.POISONED);
        }
    }
}
