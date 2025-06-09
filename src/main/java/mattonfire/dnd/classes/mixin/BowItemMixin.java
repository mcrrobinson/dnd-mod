package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;

@Mixin(BowItem.class)
public class BowItemMixin {

    private float getCustomPullProgress(int useTicks) {
        float f = (float) useTicks / 3.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

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
}