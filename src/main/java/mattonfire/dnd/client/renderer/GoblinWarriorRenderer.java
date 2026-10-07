package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.GoblinWarriorModel;
import mattonfire.dnd.entity.GoblinWarriorEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GoblinWarriorRenderer extends GeoEntityRenderer<GoblinWarriorEntity> {
    public GoblinWarriorRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new GoblinWarriorModel());
        this.shadowRadius = 0.5f;
    }
}
