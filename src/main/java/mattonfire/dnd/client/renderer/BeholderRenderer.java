package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.BeholderModel;
import mattonfire.dnd.entity.BeholderEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;

/** The Beholder, with each eyestalk as its own hit shape. */
public class BeholderRenderer extends DragonRenderer<BeholderEntity> {
    public BeholderRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new BeholderModel());
        this.shadowRadius = 1.2F;
    }
}
