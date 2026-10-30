package mattonfire.dnd.client.kobold;

import mattonfire.dnd.entity.kobold.KoboldDragonshieldEntity;
import mattonfire.dnd.entity.kobold.KoboldEntity;
import mattonfire.dnd.entity.kobold.KoboldSlingerEntity;
import mattonfire.dnd.entity.kobold.KoboldTrapmakerEntity;
import mattonfire.dnd.entity.kobold.Kobolds;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Draws a kobold: the shared model with only the gear it carries (a Kobold's spear, a Slinger's sling, a Trapmaker's
 * hook-knife, a Dragonshield's spear, shield and crest). The model is 0.95 blocks to the top of the horns, the same
 * as the hitbox; the Dragonshield is drawn at {@value Kobolds#DRAGONSHIELD_SCALE}x.
 */
public class KoboldRenderer extends GeoEntityRenderer<KoboldEntity> {
    public KoboldRenderer(EntityRendererFactory.Context context, float scale, float shadow) {
        super(context, new KoboldModel());
        this.withScale(scale);
        this.shadowRadius = shadow;
    }

    public static KoboldRenderer small(EntityRendererFactory.Context context) {
        return new KoboldRenderer(context, 1.0F, 0.3F);
    }

    public static KoboldRenderer dragonshield(EntityRendererFactory.Context context) {
        return new KoboldRenderer(context, Kobolds.DRAGONSHIELD_SCALE, 0.4F);
    }

    @Override
    public void preRender(MatrixStack poseStack, KoboldEntity kobold, BakedGeoModel model,
            VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
            int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean leader = kobold instanceof KoboldDragonshieldEntity;
        boolean slinger = kobold instanceof KoboldSlingerEntity;
        boolean trapmaker = kobold instanceof KoboldTrapmakerEntity;
        model.getBone("spear").ifPresent(bone -> bone.setHidden(slinger || trapmaker));
        model.getBone("sling").ifPresent(bone -> bone.setHidden(!slinger));
        model.getBone("knife").ifPresent(bone -> bone.setHidden(!trapmaker));
        model.getBone("shield").ifPresent(bone -> bone.setHidden(!leader));
        model.getBone("crest").ifPresent(bone -> bone.setHidden(!leader));
        super.preRender(poseStack, kobold, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);
    }
}
