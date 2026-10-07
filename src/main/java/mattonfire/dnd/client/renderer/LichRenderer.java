package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.LichModel;
import mattonfire.dnd.entity.LichEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** The Lich, with lich_glowmask.png drawn fullbright (eyes, crown, amulet and staff gems). */
public class LichRenderer extends GeoEntityRenderer<LichEntity> {
    public LichRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new LichModel());
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.6f;
    }
}
