package mattonfire.dnd.classes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModEffects;

@Mixin(BowItem.class)
public class BowItemMixin {

    // Custom pull progress for Rangers (increased speed, 3.f)
    @Unique
    private static float dnd$rangerPullProgress(int useTicks) {
        float f = (float) useTicks / 3.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    // Rangers draw faster: swap in their pull progress when the bow is released
    @WrapOperation(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BowItem;getPullProgress(I)F"))
    private float dnd$rangerPullProgressOnRelease(int useTicks, Operation<Float> original,
            @Local(argsOnly = true) LivingEntity user) {
        if (user instanceof PlayerEntityExt playerEntity && playerEntity.getDndClass() == DndCharacter.RANGER) {
            return dnd$rangerPullProgress(useTicks);
        }
        return original.call(useTicks);
    }

    // Bypassing infinity check whilst using Arrow Storm
    @ModifyVariable(method = "onStoppedUsing", at = @At(value = "STORE", ordinal = 0))
    private boolean dnd$arrowStormInfinity(boolean originalBl, ItemStack stack, World world, LivingEntity user,
            int remainingUseTicks) {
        return originalBl || (user instanceof PlayerEntity player && player.hasStatusEffect(ModEffects.ARROW_STORM));
    }

    // Allows bypassing arrow check whilst using Arrow Storm
    @ModifyVariable(method = "use", at = @At(value = "STORE", ordinal = 0))
    private boolean dnd$arrowStormHasProjectile(boolean originalBl, World world, PlayerEntity user, Hand hand) {
        return originalBl || user.hasStatusEffect(ModEffects.ARROW_STORM);
    }
}