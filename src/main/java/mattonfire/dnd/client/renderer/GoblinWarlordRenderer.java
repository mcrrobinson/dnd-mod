package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.GoblinWarlordModel;
import mattonfire.dnd.entity.GoblinWarlordEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GoblinWarlordRenderer extends GeoEntityRenderer<GoblinWarlordEntity> {
    public GoblinWarlordRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new GoblinWarlordModel());
        // Same model as the Goblin Warrior, scaled to match the larger hitbox.
        this.withScale(1.4f);
        this.shadowRadius = 0.8f;
    }
}
