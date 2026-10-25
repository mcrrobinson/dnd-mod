package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.MedusaModel;
import mattonfire.dnd.entity.MedusaEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** The Medusa, with medusa_glowmask.png (her eyes and the snakes' eyes) drawn fullbright. */
public class MedusaRenderer extends GeoEntityRenderer<MedusaEntity> {
    public MedusaRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new MedusaModel());
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.5f;
    }
}
