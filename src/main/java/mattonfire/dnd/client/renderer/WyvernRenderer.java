package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.WyvernModel;
import mattonfire.dnd.entity.WyvernEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class WyvernRenderer extends DragonRenderer<WyvernEntity> {
    public WyvernRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new WyvernModel());
    }
}
