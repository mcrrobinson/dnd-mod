package mattonfire.dnd.classes.Client.Render;

import mattonfire.dnd.classes.Client.Geo.ModelGeckoPlayerFirstPerson;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GeoPlayerFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private final GeoEntityRenderer geoRenderer;
    private final ModelGeckoPlayerFirstPerson geoModel;

    @SuppressWarnings("unchecked")
    public GeoPlayerFeature(PlayerEntityRenderer renderer, EntityRendererFactory.Context context) {
        
       super(renderer);
       this.geoModel = new ModelGeckoPlayerFirstPerson();
       this.geoRenderer = new GeoEntityRenderer(context, geoModel);
    }

    @Override
    public void render(MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light,
                       AbstractClientPlayerEntity player,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTicks,
                       float age,
                       float headYaw,
                       float headPitch) {

        // Other players' classes reach this client a moment after they appear; draw them vanilla until then
        if (((PlayerEntityExt) player).getDndClass() == null) {
            return;
        }

        // Get vanilla model - it already has the correct animations/pose from the parent renderer
        PlayerEntityModel<AbstractClientPlayerEntity> vanilla = this.getContextModel();
        
        // Pass vanilla model to GeckoLib model for bone copying
        geoModel.setVanillaModel(vanilla);
        
        matrices.push();

        // flip model upright for Geckolib coordinate system FIRST
        matrices.scale(1F, -1F, -1F);

        // then move origin down to player's feet (adjust this value if needed)
        matrices.translate(0.0d, -1.5d, 0.0d);
        
        // hide vanilla player parts
        vanilla.head.visible = false;
        vanilla.body.visible = false;
        vanilla.leftArm.visible = false;
        vanilla.rightArm.visible = false;
        vanilla.leftLeg.visible = false;
        vanilla.rightLeg.visible = false;
        vanilla.hat.visible = false;
        vanilla.jacket.visible = false;
        vanilla.leftSleeve.visible = false;
        vanilla.rightSleeve.visible = false;
        vanilla.leftPants.visible = false;
        vanilla.rightPants.visible = false;
        
        // Counter-act the extra rotation that GeoEntityRenderer will apply.
        // We are already in the player's body-rotated matrix stack.
        // GeoEntityRenderer applies (180 - bodyYaw). We apply simply bodyYaw to flip it 180 degrees relative to that.
        float bodyYaw = MathHelper.lerp(partialTicks, player.prevBodyYaw, player.bodyYaw);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bodyYaw));

        // delegate everything to Geckolib
        // We cast to GeoAnimatable to handle the raw type call safely, relying on Mixin at runtime
        geoRenderer.render(
            player,
            0f,
            partialTicks,
            matrices,
            vertexConsumers,
            light
        );

        // Restore visibility so we don't break other renderers or the next frame
        vanilla.head.visible = true;
        vanilla.body.visible = true;
        vanilla.leftArm.visible = true;
        vanilla.rightArm.visible = true;
        vanilla.leftLeg.visible = true;
        vanilla.rightLeg.visible = true;
        vanilla.hat.visible = true;
        vanilla.jacket.visible = true;
        vanilla.leftSleeve.visible = true;
        vanilla.rightSleeve.visible = true;
        vanilla.leftPants.visible = true;
        vanilla.rightPants.visible = true;

        matrices.pop();
    }
}
