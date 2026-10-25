package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.MindFlayerModel;
import mattonfire.dnd.entity.MindFlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class MindFlayerRenderer extends GeoEntityRenderer<MindFlayerEntity> {
    public MindFlayerRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new MindFlayerModel());
        // The blank white eyes glow in the dark (mind_flayer_glowmask.png).
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.4f;
    }
}
