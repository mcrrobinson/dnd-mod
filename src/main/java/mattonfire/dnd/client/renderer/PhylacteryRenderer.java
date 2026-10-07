package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.PhylacteryModel;
import mattonfire.dnd.entity.PhylacteryEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** The Lich's phylactery, with its soul light (phylactery_glowmask.png) drawn fullbright. */
public class PhylacteryRenderer extends GeoEntityRenderer<PhylacteryEntity> {
    public PhylacteryRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new PhylacteryModel());
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.5f;
    }
}
