package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.RiverPikehornModel;
import mattonfire.dnd.entity.RiverPikehornEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RiverPikehornRenderer extends GeoEntityRenderer<RiverPikehornEntity> {
    public RiverPikehornRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new RiverPikehornModel());
    }
}
