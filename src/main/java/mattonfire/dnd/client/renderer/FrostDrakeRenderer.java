package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.LightningChaserModel;
import mattonfire.dnd.entity.LairDragonEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * The Lightning Chaser's model with the icy texture from tools/frost_drake_texture.py, plus a
 * fullbright layer from frost_drake_glowmask.png (glowing eyes).
 */
public class FrostDrakeRenderer extends DragonRenderer<LairDragonEntity> {
    public FrostDrakeRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new LightningChaserModel("textures/entity/frost_drake/frost_drake.png"));
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
