package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;

/** Rangers can't pick up swords: a dropped sword stays on the ground when they walk over it. */
@Mixin(ItemEntity.class)
public abstract class RangerSwordPickupMixin {

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void dndclasses$rangerNoSwords(PlayerEntity player, CallbackInfo ci) {
        if (player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.RANGER
                && ((ItemEntity) (Object) this).getStack().getItem() instanceof SwordItem) {
            ci.cancel();
        }
    }
}
