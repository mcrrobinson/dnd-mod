package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.RustMonsterModel;
import mattonfire.dnd.entity.RustMonsterEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RustMonsterRenderer extends GeoEntityRenderer<RustMonsterEntity> {
    public RustMonsterRenderer(EntityRendererFactory.Context context) {
        super(context, new RustMonsterModel());
        // The model's shell is 14 px wide; scaled up a little towards its 1.2 block hitbox.
        this.withScale(1.2f);
        this.shadowRadius = 0.7f;
    }
}
