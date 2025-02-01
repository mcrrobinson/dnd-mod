package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import mattonfire.dnd.classes.ModEffects;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

// In your Mixin class
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    // This @Redirect will intercept the call to model.render in the render method
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V"))
    private void redirectRenderColor(
            EntityModel<T> model,
            MatrixStack matrices,
            VertexConsumer vertexConsumer,
            int light,
            int overlay,
            float red,
            float green,
            float blue,
            float alpha,
            T entity,
            float f,
            float g,
            MatrixStack matrixStack,
            VertexConsumerProvider vertexConsumers,
            int i) {

        float redModifier = 1;
        float greenModifier = 1;
        float blueModifier = 1;

        if (entity.hasStatusEffect(ModEffects.FREEZE)) {
            redModifier = 0.5f;
            greenModifier = 0.5f;
            blueModifier = 1.5f;
        }

        float tintedRed = red * redModifier;
        float tintedGreen = green * greenModifier;
        float tintedBlue = Math.min(blue * blueModifier, 1.0f); // Increase blue slightly, but clamp at 1.0f
        float tintedAlpha = alpha; // Keep the same alpha

        // Now call the model's render with our adjusted colors
        model.render(matrices, vertexConsumer, light, overlay, tintedRed, tintedGreen, tintedBlue, tintedAlpha);
    }
}
