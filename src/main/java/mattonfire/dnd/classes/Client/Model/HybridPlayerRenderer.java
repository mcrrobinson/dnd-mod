package mattonfire.dnd.classes.Client.Model;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;

public class HybridPlayerRenderer extends PlayerEntityRenderer {
    private final WizardPlayerRenderer wizardRenderer;

    public HybridPlayerRenderer(EntityRendererFactory.Context ctx, boolean slim) {
        super(ctx, slim);
        this.wizardRenderer = new WizardPlayerRenderer(ctx);
    }

    // @Override
    // public void render(PlayerEntity entity, float yaw, float tickDelta, MatrixStack matrices,
    //         VertexConsumerProvider vertexConsumers, int light) {
    //     if (entity instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.WIZARD) {
    //         // Use Geckolib renderer
    //         wizardRenderer.render((AnimatablePlayerEntity) entity, yaw, tickDelta, matrices, vertexConsumers, light);
    //     } else {
    //         // Use vanilla renderer
    //         super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    //     }
    // }
}