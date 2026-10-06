package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.RiverPikehornModel;
import mattonfire.dnd.entity.RiverPikehornEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class RiverPikehornRenderer extends DragonRenderer<RiverPikehornEntity> {
    public RiverPikehornRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new RiverPikehornModel());
    }
}
