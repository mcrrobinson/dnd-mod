package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.TrollModel;
import mattonfire.dnd.entity.TrollEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TrollRenderer extends GeoEntityRenderer<TrollEntity> {
    public TrollRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new TrollModel());
        // The model is about 2.6 blocks tall; scaled up to fill its 2.9 block hitbox.
        this.withScale(1.1f);
        this.shadowRadius = 0.85f;
    }
}
