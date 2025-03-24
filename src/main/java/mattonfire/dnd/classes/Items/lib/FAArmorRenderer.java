package mattonfire.dnd.classes.Items.lib;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class FAArmorRenderer<T extends FAArmorItem> extends GeoArmorRenderer<T> {
    protected GeoBone cape = null;
    protected GeoBone frontCape = null;
    protected GeoBone leftLegCloth = null;
    protected GeoBone rightLegCloth = null;
    protected GeoBone braid = null;

    public FAArmorRenderer(GeoModel<T> model) {
        super(model);
    }

    @Nullable
    public GeoBone getCapeBone(GeoModel<T> model) {
        return model.getBone("armorCape").orElse(null);
    }

    @Nullable
    public GeoBone getFrontCapeBone(GeoModel<T> model) {
        return model.getBone("armorFrontCape").orElse(null);
    }

    @Nullable
    public GeoBone getLeftLegClothBone(GeoModel<T> model) {
        return model.getBone("armorLeftLegCloth").orElse(null);
    }

    @Nullable
    public GeoBone getRightLegClothBone(GeoModel<T> model) {
        return model.getBone("armorRightLegCloth").orElse(null);
    }

    @Nullable
    public GeoBone getBraidBone(GeoModel<T> model) {
        return model.getBone("armorBraid").orElse(null);
    }

    @Override
    protected void grabRelevantBones(BakedGeoModel bakedModel) {
        super.grabRelevantBones(bakedModel);

        GeoModel<T> model = getGeoModel();
        cape = getCapeBone(model);
        frontCape = getFrontCapeBone(model);
        leftLegCloth = getLeftLegClothBone(model);
        rightLegCloth = getRightLegClothBone(model);
        braid = getBraidBone(model);
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);

        if(currentSlot == EquipmentSlot.CHEST) {
            setBoneVisible(cape, true);
            setBoneVisible(frontCape, true);
            setBoneVisible(leftLegCloth, true);
            setBoneVisible(rightLegCloth, true);
        } else if (currentSlot == EquipmentSlot.HEAD) {
            setBoneVisible(braid, true);
        }
    }

    public void applyBoneVisibilityByPart(EquipmentSlot currentSlot, ModelPart currentPart, BipedEntityModel<?> model) {
        super.applyBoneVisibilityByPart(currentSlot, currentPart, model);

        if(currentPart == model.body) {
            if (cape != null) cape.setHidden(false);
            if (frontCape != null) frontCape.setHidden(false);
            if (leftLegCloth != null) leftLegCloth.setHidden(false);
            if (rightLegCloth != null) rightLegCloth.setHidden(false);
        } else if (currentSlot == EquipmentSlot.HEAD) {
            if (braid != null) braid.setHidden(false);
        }
    }

@Override
public void preRender(MatrixStack matrices, T animatable, BakedGeoModel model,
                      @Nullable VertexConsumerProvider vertexConsumers,
                      @Nullable VertexConsumer vertexConsumer,
                      boolean isReRender, float tickDelta, int light, int overlay,
                      float red, float green, float blue, float alpha) {

    super.preRender(matrices, animatable, model, vertexConsumers, vertexConsumer,
                    isReRender, tickDelta, light, overlay, red, green, blue, alpha);

        if(frontCape != null) {
            FARenderUtils.setFrontLegCapeAngle(this, frontCape);
        }

        if(cape != null) {
            if(currentEntity instanceof AbstractClientPlayerEntity player) {
                FARenderUtils.applyCapeRotation(player, cape, tickDelta);
            } else {
                cape.updateRotation((float) -Math.toRadians(5.0F), 0.0F, 0.0F);
            }
        }

        if (braid != null && currentEntity instanceof AbstractClientPlayerEntity player) {
            FARenderUtils.applyBraidRotation(player, braid, tickDelta);
        }
    }

    @Override
    protected void applyBaseTransformations(BipedEntityModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);

        if(cape != null) {
            ModelPart bodyPart = baseModel.body;

            cape.updatePosition(bodyPart.pivotX, 1 - bodyPart.pivotY, bodyPart.pivotZ);
        }

        if(frontCape != null) {
            ModelPart leftLegPart = baseModel.leftLeg;

            frontCape.updatePosition(leftLegPart.pivotX - 1.95f, 13 - leftLegPart.pivotY, leftLegPart.pivotZ - 0.1f);
        }

        if(leftLegCloth != null) {
            ModelPart leftLegPart = baseModel.leftLeg;

            RenderUtils.matchModelPartRot(leftLegPart, leftLegCloth);
            leftLegCloth.updatePosition(leftLegPart.pivotX - 2, 12 - leftLegPart.pivotY, leftLegPart.pivotZ);
        }

        if(rightLegCloth != null) {
            ModelPart rightLegPart = baseModel.rightLeg;

            RenderUtils.matchModelPartRot(rightLegPart, rightLegCloth);
            rightLegCloth.updatePosition(rightLegPart.pivotX + 2, 12 - rightLegPart.pivotY, rightLegPart.pivotZ);
        }
    }

    @Override
    public void setVisible(boolean pVisible) {
        super.setVisible(pVisible);

        setBoneVisible(cape, pVisible);
        setBoneVisible(frontCape, pVisible);
        setBoneVisible(leftLegCloth, pVisible);
        setBoneVisible(rightLegCloth, pVisible);
        setBoneVisible(braid, pVisible);
    }
}