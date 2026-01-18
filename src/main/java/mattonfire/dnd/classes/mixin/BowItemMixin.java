package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModEffects;

@Mixin(BowItem.class)
public class BowItemMixin {

    // Custom pull progress for Rangers (increased speed, 3.f)
    private float getCustomPullProgress(int useTicks) {
        float f = (float) useTicks / 3.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    // Redirecting static method to allow custom pull progress for Rangers
    @Redirect(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BowItem;getPullProgress(I)F"))
    private float redirectStaticGetPullProgress(int useTicks, ItemStack stack, World world, LivingEntity user,
            int remainingUseTicks) {
        if (user instanceof PlayerEntityExt playerEntity) {
            if (playerEntity.getDndClass() == DndCharacter.RANGER) {
                return getCustomPullProgress(useTicks);
            } else {
                return BowItem.getPullProgress(useTicks);
            }
        } else {
            return BowItem.getPullProgress(useTicks);
        }
    }

    // Bypassing infinity check whilst using Arrow Storm
    @ModifyVariable(method = "onStoppedUsing", at = @At(value = "STORE", ordinal = 0))
    private boolean modifyInfinityFlag(boolean originalBl, ItemStack stack, World world, LivingEntity user,
            int remainingUseTicks) {
        return originalBl || (user instanceof PlayerEntity player && player.hasStatusEffect(ModEffects.ARROW_STORM));
    }

    // Allows bypassing arrow check whilst using Arrow Storm
    @ModifyVariable(method = "use", at = @At(value = "STORE", ordinal = 0))
    private boolean modifyHasProjectileFlag(boolean originalBl, World world, PlayerEntity user, Hand hand) {
        return originalBl || user.hasStatusEffect(ModEffects.ARROW_STORM);
    }
}