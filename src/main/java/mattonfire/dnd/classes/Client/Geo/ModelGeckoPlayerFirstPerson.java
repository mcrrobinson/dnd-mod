package mattonfire.dnd.classes.Client.Geo;


import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
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
            head.setPosX(vanillaModel.head.pivotX);
            head.setPosY(-vanillaModel.head.pivotY);
            head.setPosZ(-vanillaModel.head.pivotZ);
        }
        
        if (body != null) {
            body.setRotX(-vanillaModel.body.pitch);
            body.setRotY(-vanillaModel.body.yaw);
            body.setRotZ(-vanillaModel.body.roll);
            body.setPosX(vanillaModel.body.pivotX);
            body.setPosY(-vanillaModel.body.pivotY);
            body.setPosZ(-vanillaModel.body.pivotZ);
        }
        
        if (rightArm != null) {
            rightArm.setRotX(-vanillaModel.rightArm.pitch);
            rightArm.setRotY(-vanillaModel.rightArm.yaw);
            rightArm.setRotZ(vanillaModel.rightArm.roll);
            
            float defaultX = -5.0f;
            float defaultY = vanillaModel.thinArms ? 2.5f : 2.0f;
            float defaultZ = 0.0f;

            rightArm.setPosX(vanillaModel.rightArm.pivotX - defaultX);
            rightArm.setPosY(-(vanillaModel.rightArm.pivotY - defaultY));
            rightArm.setPosZ(-(vanillaModel.rightArm.pivotZ - defaultZ));
        }
        
        if (leftArm != null) {
            leftArm.setRotX(-vanillaModel.leftArm.pitch);
            leftArm.setRotY(-vanillaModel.leftArm.yaw);
            leftArm.setRotZ(vanillaModel.leftArm.roll);

            float defaultX = 5.0f;
            float defaultY = vanillaModel.thinArms ? 2.5f : 2.0f;
            float defaultZ = 0.0f;
            
            leftArm.setPosX(vanillaModel.leftArm.pivotX - defaultX);
            leftArm.setPosY(-(vanillaModel.leftArm.pivotY - defaultY));
            leftArm.setPosZ(-(vanillaModel.leftArm.pivotZ - defaultZ));
        }
        
        if (rightLeg != null) {
            rightLeg.setRotX(-vanillaModel.rightLeg.pitch);
            rightLeg.setRotY(-vanillaModel.rightLeg.yaw);
            rightLeg.setRotZ(vanillaModel.rightLeg.roll);
            
            float defaultX = -1.9f;
            float defaultY = 12.0f;
            float defaultZ = 0.0f;

            rightLeg.setPosX(vanillaModel.rightLeg.pivotX - defaultX);
            rightLeg.setPosY(-(vanillaModel.rightLeg.pivotY - defaultY));
            rightLeg.setPosZ(vanillaModel.rightLeg.pivotZ - defaultZ);
        }
        
        if (leftLeg != null) {
            leftLeg.setRotX(-vanillaModel.leftLeg.pitch);
            leftLeg.setRotY(-vanillaModel.leftLeg.yaw);
            leftLeg.setRotZ(vanillaModel.leftLeg.roll);
            
            float defaultX = 1.9f;
            float defaultY = 12.0f;
            float defaultZ = 0.0f;

            leftLeg.setPosX(vanillaModel.leftLeg.pivotX - defaultX);
                 
        GeoBone cape = this.getBone("armorCapeEpicFight").orElse(null);
        if (cape != null && entity instanceof AbstractClientPlayerEntity player) {
            float partialTick = animationState.getPartialTick();
            double d = MathHelper.lerp((double)partialTick, player.prevCapeX, player.capeX) - MathHelper.lerp((double)partialTick, player.prevX, player.getX());
            double e = MathHelper.lerp((double)partialTick, player.prevCapeY, player.capeY) - MathHelper.lerp((double)partialTick, player.prevY, player.getY());
            double m = MathHelper.lerp((double)partialTick, player.prevCapeZ, player.capeZ) - MathHelper.lerp((double)partialTick, player.prevZ, player.getZ());
            float n = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw);
            double o = Math.sin(n * ((float)Math.PI / 180));
            double p = -Math.cos(n * ((float)Math.PI / 180));
            float q = (float)e * 10.0f;
            q = MathHelper.clamp(q, -6.0f, 32.0f);
            float r = (float)(d * o + m * p) * 100.0f;
            r = MathHelper.clamp(r, 0.0f, 150.0f);
            float s = (float)(d * p - m * o) * 100.0f;
            s = MathHelper.clamp(s, -20.0f, 20.0f);
            if (r < 0.0f) {
                r = 0.0f;
            }
            float t = MathHelper.lerp(partialTick, player.prevStrideDistance, player.strideDistance);
            q += MathHelper.sin(MathHelper.lerp(partialTick, player.prevHorizontalSpeed, player.horizontalSpeed) * 6.0f) * 32.0f * t;
            if (player.isInSneakingPose()) {
                q += 25.0f;
            }
            
            // Apply rotations (converted to radians for GeoBone)
            // Vanilla applies: RotX(6 + q/2 + r), RotZ(s/2), RotY(180 - s/2)
            // But since this is a bone, we likely just need RotX and RotZ/Y relative to body.
            // Also need to account for coordinate flip if necessary.
            
            float rotX = (6.0f + q / 2.0f + r); 
            float rotZ = s / 2.0f;
            float rotY = 180.0f - s / 2.0f; // This is a specific vanilla oddity, might not be needed for just the cape flaring
            
            // Negating X because of previous established inverted pitch for this model? 
            // Previous fix restored RotX = pitch, so maybe direct application works.
            // However, vanilla RotX is usually positive = down? 
            // Let's try direct mapping first, but convert to radians.
            
            cape.setRotX(-(float)Math.toRadians(rotX));
            cape.setRotZ((float)Math.toRadians(rotZ));
            
            // The Y rotation in vanilla cape is likely to face the texture correctly. 
            // In a bone structure, the cape is already parented to body, so we probably ignore global Yaw.
            // But `s` encodes sway.
            cape.setRotY((float)Math.toRadians(s/2.0f)); 
        }
        
        GeoBone duplicateCape = this.getBone("armorCape").orElse(null);
        if (duplicateCape != null) {
            duplicateCape.setHidden(true);
        }            leftLeg.setPosZ(vanillaModel.leftLeg.pivotZ - defaultZ);
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