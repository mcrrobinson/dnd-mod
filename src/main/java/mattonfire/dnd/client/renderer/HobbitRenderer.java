package mattonfire.dnd.client.renderer;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.HobbitEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Hobbits use the player model drawn with child proportions (big head, small body), which makes
 * them about a block tall, with one of several skins.
 */
public class HobbitRenderer extends BipedEntityRenderer<HobbitEntity, PlayerEntityModel<HobbitEntity>> {
    private static final Identifier[] TEXTURES = new Identifier[HobbitEntity.VARIANTS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = new Identifier(DnDClasses.MOD_ID, "textures/entity/hobbit/hobbit_" + i + ".png");
        }
    }

    public HobbitRenderer(EntityRendererFactory.Context context) {
        super(context, new HobbitModel(context.getPart(EntityModelLayers.PLAYER)), 0.35F);
    }

    @Override
    public Identifier getTexture(HobbitEntity entity) {
        return TEXTURES[Math.floorMod(entity.getVariant(), TEXTURES.length)];
    }

    @Override
    protected void scale(HobbitEntity entity, MatrixStack matrices, float amount) {
        // Child proportions come out at ~0.95 blocks; hobbits are a touch taller.
        matrices.scale(1.1F, 1.1F, 1.1F);
    }

    private static class HobbitModel extends PlayerEntityModel<HobbitEntity> {
        HobbitModel(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setAngles(HobbitEntity entity, float limbAngle, float limbDistance, float animationProgress,
                              float headYaw, float headPitch) {
            super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
            // The renderer resets this from isBaby() each frame, before posing and drawing.
            this.child = true;
        }
    }
}
