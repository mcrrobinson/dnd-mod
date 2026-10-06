package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.LightningChaserModel;
import mattonfire.dnd.entity.LightningChaserEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class LightningChaserRenderer extends DragonRenderer<LightningChaserEntity> {
    public LightningChaserRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new LightningChaserModel());
    }
}
