
package mattonfire.dnd.classes.Client.Model;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WizardPlayerRenderer extends GeoEntityRenderer<AnimatablePlayerEntity> {
    public WizardPlayerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new WizardPlayerModel());
    }

    @Override
    public void render(AnimatablePlayerEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
            VertexConsumerProvider bufferSource, int packedLight) {
        if (entity instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.WIZARD) {
            super.render(entity, entityYaw, partialTick, poseStack,
                    bufferSource, packedLight);
        } else {
            // pass
        }

    }
}