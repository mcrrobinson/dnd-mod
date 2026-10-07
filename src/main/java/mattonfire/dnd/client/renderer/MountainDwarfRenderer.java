package mattonfire.dnd.client.renderer;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.MountainDwarfEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/**
 * Mountain dwarves use the player model reshaped into a dwarf: a full-size head on a broad, short
 * body with thick arms and stubby legs, about 1.4 blocks tall, with one of several skins. Armour
 * copies each part's scale, so helmets and chestplates fit the new shape.
 */
public class MountainDwarfRenderer extends BipedEntityRenderer<MountainDwarfEntity, PlayerEntityModel<MountainDwarfEntity>> {
    private static final Identifier[] TEXTURES = new Identifier[MountainDwarfEntity.VARIANTS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = new Identifier(DnDClasses.MOD_ID, "textures/entity/mountain_dwarf/mountain_dwarf_" + i + ".png");
        }
    }

    public MountainDwarfRenderer(EntityRendererFactory.Context context) {
        super(context, new DwarfModel(context.getPart(EntityModelLayers.PLAYER)), 0.45F);
        this.addFeature(new ArmorFeatureRenderer<>(this,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public Identifier getTexture(MountainDwarfEntity entity) {
        return TEXTURES[Math.floorMod(entity.getVariant(), TEXTURES.length)];
    }

    private static class DwarfModel extends PlayerEntityModel<MountainDwarfEntity> {
        /** Broadness of body, arms and legs. */
        private static final float WIDE = 1.2F;
        private static final float BODY_HEIGHT = 0.85F;
        private static final float ARM_HEIGHT = 0.8F;
        private static final float LEG_HEIGHT = 0.55F;
        private static final float LEG_LENGTH = 12.0F * LEG_HEIGHT;
        /** The neck drops by however much the body and legs were shortened; feet stay on the ground at y = 24. */
        private static final float NECK = 24.0F - LEG_LENGTH - 12.0F * BODY_HEIGHT;

        DwarfModel(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setAngles(MountainDwarfEntity entity, float limbAngle, float limbDistance, float animationProgress,
                              float headYaw, float headPitch) {
            super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
            // The pivots below were all just reset by super, so scaling them is safe every frame.
            this.head.pivotY = NECK;
            this.body.pivotY = NECK;
            this.body.xScale = WIDE;
            this.body.yScale = BODY_HEIGHT;
            this.body.zScale = WIDE;
            for (ModelPart arm : new ModelPart[]{this.rightArm, this.leftArm}) {
                arm.pivotX *= WIDE;
                arm.pivotZ *= WIDE;
                arm.pivotY = NECK + 2.0F * BODY_HEIGHT;
                arm.xScale = WIDE;
                arm.yScale = ARM_HEIGHT;
                arm.zScale = WIDE;
            }
            this.rightLeg.pivotX = -1.9F * WIDE;
            this.leftLeg.pivotX = 1.9F * WIDE;
            for (ModelPart leg : new ModelPart[]{this.rightLeg, this.leftLeg}) {
                leg.pivotY = 24.0F - LEG_LENGTH;
                leg.xScale = WIDE;
                leg.yScale = LEG_HEIGHT;
                leg.zScale = WIDE;
            }
            this.hat.copyTransform(this.head);
            this.jacket.copyTransform(this.body);
            this.rightSleeve.copyTransform(this.rightArm);
            this.leftSleeve.copyTransform(this.leftArm);
            this.rightPants.copyTransform(this.rightLeg);
            this.leftPants.copyTransform(this.leftLeg);
        }
    }
}
