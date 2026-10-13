package mattonfire.dnd.client.renderer;

import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.ClockworkSentinelEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.IronGolemEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * The Clockwork Sentinel: the iron golem's model at {@value #SCALE} scale with a copper texture
 * (tools/cogsdeep_textures.py), the golem's sway as it walks, its crack textures as it's hurt, and glowing eyes
 * and clock rim.
 */
public class ClockworkSentinelRenderer extends MobEntityRenderer<ClockworkSentinelEntity, IronGolemEntityModel<ClockworkSentinelEntity>> {
    public static final float SCALE = 0.75F;
    private static final Identifier TEXTURE = new Identifier(DnDClasses.MOD_ID, "textures/entity/clockwork_sentinel/sentinel.png");
    private static final Identifier EYES = new Identifier(DnDClasses.MOD_ID, "textures/entity/clockwork_sentinel/sentinel_eyes.png");

    public ClockworkSentinelRenderer(EntityRendererFactory.Context context) {
        super(context, new IronGolemEntityModel<>(context.getPart(EntityModelLayers.IRON_GOLEM)), 0.7F * SCALE);
        this.addFeature(new CrackFeature(this));
        this.addFeature(new EyesFeatureRenderer<>(this) {
            @Override
            public RenderLayer getEyesTexture() {
                return RenderLayer.getEyes(EYES);
            }
        });
    }

    @Override
    public Identifier getTexture(ClockworkSentinelEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(ClockworkSentinelEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(SCALE, SCALE, SCALE);
    }

    /** The iron golem's side-to-side sway while walking. */
    @Override
    protected void setupTransforms(ClockworkSentinelEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw,
                                   float tickDelta) {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta);
        if (entity.limbAnimator.getSpeed() >= 0.01D) {
            float pos = entity.limbAnimator.getPos(tickDelta) + 6.0F;
            float sway = (Math.abs(pos % 13.0F - 6.5F) - 3.25F) / 3.25F;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(6.5F * sway));
        }
    }

    /** The iron golem's crack overlays as health drops (vanilla textures). */
    private static final class CrackFeature extends FeatureRenderer<ClockworkSentinelEntity, IronGolemEntityModel<ClockworkSentinelEntity>> {
        private static final Map<IronGolemEntity.Crack, Identifier> TEXTURES = Map.of(
                IronGolemEntity.Crack.LOW, new Identifier("textures/entity/iron_golem/iron_golem_crackiness_low.png"),
                IronGolemEntity.Crack.MEDIUM, new Identifier("textures/entity/iron_golem/iron_golem_crackiness_medium.png"),
                IronGolemEntity.Crack.HIGH, new Identifier("textures/entity/iron_golem/iron_golem_crackiness_high.png"));

        CrackFeature(FeatureRendererContext<ClockworkSentinelEntity, IronGolemEntityModel<ClockworkSentinelEntity>> context) {
            super(context);
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ClockworkSentinelEntity entity,
                           float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            Identifier crack = TEXTURES.get(entity.getCrack());
            if (crack != null) {
                renderModel(this.getContextModel(), crack, matrices, vertexConsumers, light, entity, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
