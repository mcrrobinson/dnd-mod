package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.OwlbearModel;
import mattonfire.dnd.entity.OwlbearEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OwlbearRenderer extends GeoEntityRenderer<OwlbearEntity> {
    public OwlbearRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new OwlbearModel());
        // The model is about 1.7 blocks tall on all fours; scaled up to fill its 1.9 block hitbox.
        this.withScale(1.12f);
        this.shadowRadius = 0.9f;
    }
}
