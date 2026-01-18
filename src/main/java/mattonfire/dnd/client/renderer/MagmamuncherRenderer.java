package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.MagmamuncherModel;
import mattonfire.dnd.entity.MagmamuncherEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MagmamuncherRenderer extends GeoEntityRenderer<MagmamuncherEntity> {
    public MagmamuncherRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new MagmamuncherModel());
    }
}
