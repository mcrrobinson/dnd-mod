package mattonfire.dnd.client.renderer;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.client.model.BasiliskModel;
import mattonfire.dnd.entity.BasiliskEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * The basilisk with glowing eyes (basilisk_glowmask.png). A petrified basilisk is drawn stone grey with the same
 * tint {@code LivingEntityRendererMixin} gives vanilla-rendered mobs (GeckoLib renderers don't go through it), and
 * its eyes go dark.
 */
public class BasiliskRenderer extends GeoEntityRenderer<BasiliskEntity> {
    private static final Color STONE = Color.ofRGBA(0.6F, 0.6F, 0.62F, 1.0F);

    public BasiliskRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new BasiliskModel());
        this.shadowRadius = 0.8f;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(MatrixStack poseStack, BasiliskEntity animatable, BakedGeoModel bakedModel,
                    net.minecraft.client.render.RenderLayer renderType, VertexConsumerProvider bufferSource,
                    VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (!Conditions.has(animatable, Condition.PETRIFIED)) {
                    super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick,
                            packedLight, packedOverlay);
                }
            }
        });
    }

    @Override
    public Color getRenderColor(BasiliskEntity animatable, float partialTick, int packedLight) {
        return Conditions.has(animatable, Condition.PETRIFIED) ? STONE : super.getRenderColor(animatable, partialTick,
                packedLight);
    }

    @Override
    public int getPackedOverlay(BasiliskEntity animatable, float u) {
        if (Conditions.has(animatable, Condition.PETRIFIED)) {
            // The white flash overlay washes the colours out to stone
            return OverlayTexture.packUv(OverlayTexture.getU(0.65F), OverlayTexture.getV(animatable.hurtTime > 0));
        }
        return super.getPackedOverlay(animatable, u);
    }
}
