package mattonfire.dnd.classes.Client.Render;

import mattonfire.dnd.classes.Client.Geo.ModelGeckoPlayerFirstPerson;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GeoPlayerFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private final GeoEntityRenderer geoRenderer;
    private final ModelGeckoPlayerFirstPerson geoModel;

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

        // Get vanilla model and ensure it has current animations
        PlayerEntityModel<AbstractClientPlayerEntity> vanilla = this.getContextModel();
        vanilla.animateModel(player, limbSwing, limbSwingAmount, partialTicks);
        vanilla.setAngles(player, limbSwing, limbSwingAmount, age, headYaw, headPitch);
        
        // Pass vanilla model to GeckoLib model for bone copying
        geoModel.setVanillaModel(vanilla);
        
        matrices.push();

        // flip model upright for Geckolib coordinate system FIRST
        matrices.scale(1F, -1F, -1F);

        // then move origin down to player's feet (adjust this value if needed)
        matrices.translate(0.0d, -1.5d, 0.0d);

        // rotate model to match player body yaw
        float yawDegrees = player.getYaw(partialTicks);
        float yawRad = (float) Math.toRadians(yawDegrees);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(yawRad));
        
        // hide vanilla player parts
        vanilla.head.visible = false;
        vanilla.body.visible = false;
        vanilla.leftArm.visible = false;
        vanilla.rightArm.visible = false;
        vanilla.leftLeg.visible = false;
        vanilla.rightLeg.visible = false;

        // delegate everything to Geckolib
        geoRenderer.render(
            player,
            limbSwing,
            limbSwingAmount,
            matrices,
            vertexConsumers,
            light
        );

        matrices.pop();
    }
}
