package mattonfire.dnd.classes.Client.Geo;


import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.object.DataTicket;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class ModelGeckoPlayerFirstPerson extends GeoModel<GeoAnimatable> {
    private PlayerEntityModel<?> vanillaModel;

    public void setVanillaModel(PlayerEntityModel<?> model) {
        this.vanillaModel = model;
    }

    @Override
    public void setCustomAnimations(GeoAnimatable entity, long instanceId, AnimationState<GeoAnimatable> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        
        if (vanillaModel == null) return;
        
        GeoBone head = this.getBone("bipedHead").orElse(null);
        GeoBone body = this.getBone("bipedBody").orElse(null);
        GeoBone rightArm = this.getBone("bipedRightArm").orElse(null);
        GeoBone leftArm = this.getBone("bipedLeftArm").orElse(null);
        GeoBone rightLeg = this.getBone("bipedRightLeg").orElse(null);
        GeoBone leftLeg = this.getBone("bipedLeftLeg").orElse(null);
        
        // Negate rotations because of the coordinate system flip (scale -1 on Y and Z)
        if (head != null) {
            head.setRotX(-vanillaModel.head.pitch);
            head.setRotY(-vanillaModel.head.yaw);
            head.setRotZ(-vanillaModel.head.roll);
        }
        
        if (body != null) {
            body.setRotX(-vanillaModel.body.pitch);
            body.setRotY(-vanillaModel.body.yaw);
            body.setRotZ(-vanillaModel.body.roll);
        }
        
        if (rightArm != null) {
            rightArm.setRotX(-vanillaModel.rightArm.pitch);
            rightArm.setRotY(-vanillaModel.rightArm.yaw);
            rightArm.setRotZ(-vanillaModel.rightArm.roll);
        }
        
        if (leftArm != null) {
            leftArm.setRotX(-vanillaModel.leftArm.pitch);
            leftArm.setRotY(-vanillaModel.leftArm.yaw);
            leftArm.setRotZ(-vanillaModel.leftArm.roll);
        }
        
        if (rightLeg != null) {
            rightLeg.setRotX(-vanillaModel.rightLeg.pitch);
            rightLeg.setRotY(-vanillaModel.rightLeg.yaw);
            rightLeg.setRotZ(-vanillaModel.rightLeg.roll);
        }
        
        if (leftLeg != null) {
            leftLeg.setRotX(-vanillaModel.leftLeg.pitch);
            leftLeg.setRotY(-vanillaModel.leftLeg.yaw);
            leftLeg.setRotZ(-vanillaModel.leftLeg.roll);
        }
    }

    @Override
    public Identifier getAnimationResource(GeoAnimatable entity) {
        throw new UnsupportedOperationException("Animation resource not supported");
    }

    @Override
    public Identifier getModelResource(GeoAnimatable entity) {
        return new Identifier("dndclasses", "geo/wizard_armor.geo.json");
    }

    @Override
    public Identifier getTextureResource(GeoAnimatable entity) {
        return new Identifier("dndclasses", "textures/models/armor/wizard_armor.png");
    }
}