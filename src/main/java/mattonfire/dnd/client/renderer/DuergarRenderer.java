package mattonfire.dnd.client.renderer;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.entity.DuergarEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Duergar use the mountain dwarf's reshaped player model ({@link MountainDwarfRenderer.DwarfModel}) with grey skins
 * from {@code tools/duergar_skins.py}: six for soldiers and arbalesters (the variant), and one each for the factor
 * and the taskmaster. An Enlarged duergar is drawn up to 1.5x, easing over half a second
 * ({@link DuergarEntity#renderScale}). Arms take a crossbow's load and aim poses.
 */
public class DuergarRenderer extends BipedEntityRenderer<DuergarEntity, DuergarRenderer.DuergarModel> {
    private static final Identifier[] SKINS = new Identifier[DuergarEntity.VARIANTS];
    private static final Identifier FACTOR_SKIN = new Identifier(DnDClasses.MOD_ID, "textures/entity/duergar/factor.png");
    private static final Identifier TASKMASTER_SKIN = new Identifier(DnDClasses.MOD_ID, "textures/entity/duergar/taskmaster.png");

    static {
        for (int i = 0; i < SKINS.length; i++) {
            SKINS[i] = new Identifier(DnDClasses.MOD_ID, "textures/entity/duergar/duergar_" + i + ".png");
        }
    }

    public DuergarRenderer(EntityRendererFactory.Context context) {
        super(context, new DuergarModel(context.getPart(EntityModelLayers.PLAYER)), 0.45F);
        this.addFeature(new ArmorFeatureRenderer<>(this,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    /** Registers the renderer for every duergar type. */
    public static void registerAll() {
        DuergarEntities.all().forEach(type -> EntityRendererRegistry.register(type, DuergarRenderer::new));
    }

    @Override
    public Identifier getTexture(DuergarEntity entity) {
        if (entity.getType() == DuergarEntities.DUERGAR_FACTOR) {
            return FACTOR_SKIN;
        }
        if (entity.getType() == DuergarEntities.DUERGAR_TASKMASTER) {
            return TASKMASTER_SKIN;
        }
        return SKINS[Math.floorMod(entity.getVariant(), SKINS.length)];
    }

    @Override
    protected void scale(DuergarEntity entity, MatrixStack matrices, float tickDelta) {
        float scale = entity.renderScale(tickDelta);
        if (scale != 1.0F) {
            matrices.scale(scale, scale, scale);
        }
        this.shadowRadius = 0.45F * scale;
    }

    /** The dwarf shape, with arm poses for a crossbow (loading, then held aimed) and other held items. */
    public static class DuergarModel extends MountainDwarfRenderer.DwarfModel<DuergarEntity> {
        DuergarModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setAngles(DuergarEntity entity, float limbAngle, float limbDistance, float animationProgress,
                              float headYaw, float headPitch) {
            ItemStack held = entity.getMainHandStack();
            if (held.getItem() instanceof CrossbowItem) {
                this.rightArmPose = entity.isUsingItem() ? ArmPose.CROSSBOW_CHARGE
                        : CrossbowItem.isCharged(held) ? ArmPose.CROSSBOW_HOLD : ArmPose.ITEM;
            } else {
                this.rightArmPose = held.isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
            }
            this.leftArmPose = entity.getOffHandStack().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
            super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        }
    }
}
