package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.AncientDragonModel;
import mattonfire.dnd.entity.dragon.AncientDragonEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * An Ancient dragon drawn at {@link AncientDragonEntity#SCALE}x (a ~46-block wingspan), with a fullbright layer
 * from its colour's glowmask (eyes, throat and belly seams). DragonRenderer fits the hit shapes to the scaled
 * bones.
 */
public class AncientDragonRenderer extends DragonRenderer<AncientDragonEntity> {
    public AncientDragonRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new AncientDragonModel());
        this.withScale(AncientDragonEntity.SCALE);
        this.shadowRadius = 6.0F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
