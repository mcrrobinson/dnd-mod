package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.WyvernModel;
import mattonfire.dnd.entity.WyvernEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WyvernRenderer extends GeoEntityRenderer<WyvernEntity> {
    public WyvernRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new WyvernModel());
    }
}
