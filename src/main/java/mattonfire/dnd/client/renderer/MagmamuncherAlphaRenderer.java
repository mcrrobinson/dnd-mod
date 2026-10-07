package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.MagmamuncherAlphaModel;
import mattonfire.dnd.entity.MagmamuncherAlphaEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MagmamuncherAlphaRenderer extends GeoEntityRenderer<MagmamuncherAlphaEntity> {
    // Darker, redder than a regular Magmamuncher.
    private static final Color TINT = Color.ofRGB(255, 150, 120);
    private static final Color ENRAGED_TINT = Color.ofRGB(255, 100, 80);

    public MagmamuncherAlphaRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new MagmamuncherAlphaModel());
        // Same model as the Magmamuncher, scaled up 2.5x to match its hitbox.
        this.withScale(2.5f);
        this.shadowRadius = 1.5f;
    }

    @Override
    public Color getRenderColor(MagmamuncherAlphaEntity animatable, float partialTick, int packedLight) {
        return animatable.getHealth() < animatable.getMaxHealth() * 0.5F ? ENRAGED_TINT : TINT;
    }
}
