package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

/**
 * Rangers zoom in 2x while drawing a bow, on top of the vanilla bow FOV change. Like the spyglass it
 * only applies in first person and ignores the FOV effects setting. The zoom follows the Ranger's
 * 3-tick draw (BowItemMixin), so it's at full strength almost immediately.
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class RangerBowZoomMixin {
    private static final float RANGER_DRAW_TICKS = 3.0F;
    private static final float RANGER_ZOOM = 2.0F;

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void dndclasses$rangerBowZoom(CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        if (!(self instanceof PlayerEntityExt ext) || ext.getDndClass() != DndCharacter.RANGER
                || !self.isUsingItem() || !self.getActiveItem().isOf(Items.BOW)
                || !MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
            return;
        }
        float pull = Math.min(self.getItemUseTime() / RANGER_DRAW_TICKS, 1.0F);
        cir.setReturnValue(cir.getReturnValueF() * MathHelper.lerp(pull, 1.0F, 1.0F / RANGER_ZOOM));
    }
}
