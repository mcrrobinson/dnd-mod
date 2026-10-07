package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.WyvernModel;
import mattonfire.dnd.entity.WyvernEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** The Wyvern renderer plus a fullbright layer from ember_glowmask.png (glowing eyes and wing membranes). */
public class EmberWyvernRenderer extends DragonRenderer<WyvernEntity> {
    public EmberWyvernRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new WyvernModel());
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
