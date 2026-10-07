package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.MimicModel;
import mattonfire.dnd.entity.MimicEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MimicRenderer extends GeoEntityRenderer<MimicEntity> {
    public MimicRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new MimicModel());
    }

    @Override
    public void render(MimicEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        // Chests don't cast a shadow, so a disguised mimic doesn't either.
        this.shadowRadius = entity.isDormant() ? 0.0F : 0.45F;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
