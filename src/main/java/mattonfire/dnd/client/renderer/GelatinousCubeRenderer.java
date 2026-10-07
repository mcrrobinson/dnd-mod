package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.GelatinousCubeModel;
import mattonfire.dnd.entity.GelatinousCubeEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renders the jelly translucent, with the items it has absorbed slowly turning inside it. */
public class GelatinousCubeRenderer extends GeoEntityRenderer<GelatinousCubeEntity> {
    /** Where each absorbed stack floats, in blocks from the cube's bottom centre (x, y, z). */
    private static final float[][] SLOTS = {
            {-0.45f, 0.45f, -0.35f},
            {0.45f, 0.75f, 0.3f},
            {0.1f, 1.25f, -0.45f},
            {-0.35f, 1.1f, 0.45f},
            {0.5f, 0.35f, -0.5f},
            {-0.05f, 0.6f, 0.05f},
    };

    private final ItemRenderer itemRenderer;

    public GelatinousCubeRenderer(EntityRendererFactory.Context context) {
        super(context, new GelatinousCubeModel());
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.9f;
    }

    @Override
    public void render(GelatinousCubeEntity cube, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        // Items first: they're opaque, and the translucent jelly is drawn over them afterwards.
        this.renderAbsorbedItems(cube, partialTick, poseStack, bufferSource, packedLight);
        if (bufferSource instanceof VertexConsumerProvider.Immediate immediate) {
            // Item quads go into shared buffers that are normally drawn after every entity, which would be
            // after the jelly has already written its depth, hiding them. Draw them now instead.
            immediate.draw(TexturedRenderLayers.getEntitySolid());
            immediate.draw(TexturedRenderLayers.getEntityCutout());
            immediate.draw(TexturedRenderLayers.getItemEntityTranslucentCull());
            immediate.draw(TexturedRenderLayers.getEntityTranslucentCull());
        }
        super.render(cube, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderAbsorbedItems(GelatinousCubeEntity cube, float partialTick, MatrixStack poseStack,
                                     VertexConsumerProvider bufferSource, int packedLight) {
        float bodyYaw = MathHelper.lerpAngleDegrees(partialTick, cube.prevBodyYaw, cube.bodyYaw);
        float time = cube.age + partialTick;
        for (int i = 0; i < GelatinousCubeEntity.MAX_ABSORBED; i++) {
            ItemStack stack = cube.getAbsorbed(i);
            if (stack.isEmpty()) {
                continue;
            }
            float[] slot = SLOTS[i];
            poseStack.push();
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
            float drift = MathHelper.sin(time * 0.05f + i * 1.7f) * 0.06f;
            poseStack.translate(slot[0], slot[1] + drift, slot[2]);
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 0.8f + i * 60.0f));
            poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(15.0f * MathHelper.sin(time * 0.03f + i)));
            poseStack.scale(1.3f, 1.3f, 1.3f);
            this.itemRenderer.renderItem(stack, ModelTransformationMode.GROUND, packedLight, OverlayTexture.DEFAULT_UV,
                    poseStack, bufferSource, cube.world, cube.getId() * 31 + i);
            poseStack.pop();
        }
    }

    @Override
    protected float getDeathMaxRotation(GelatinousCubeEntity cube) {
        // It slumps where it is rather than toppling over.
        return 0.0f;
    }
}
