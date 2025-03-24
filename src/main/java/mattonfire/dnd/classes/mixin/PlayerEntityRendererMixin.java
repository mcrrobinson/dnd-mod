package mattonfire.dnd.classes.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.Items.lib.FAArmorItem;
import mattonfire.dnd.classes.Items.lib.FARenderUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;


@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "render",
    at = @At(
        value = "INVOKE", 
        target = "Lnet/minecraft/client/renderer/entity/PlayerEntityRenderer;render(Lnet/minecraft/world/entity/PlayerEntityRenderer;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/renderer/VertexConsumerProvider;I)V"
    )
)    private void onRender(AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        Item item = abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST).getItem();
        PlayerEntityModel<AbstractClientPlayerEntity> playerModel = ((PlayerEntityRenderer)(Object)this).getModel();

        if (item instanceof FAArmorItem) {
            FARenderUtils.setArmsVisibility(playerModel, false);
        }
    }
}