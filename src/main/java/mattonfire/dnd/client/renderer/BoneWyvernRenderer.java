package mattonfire.dnd.client.renderer;

import mattonfire.dnd.client.model.WyvernModel;
import mattonfire.dnd.entity.BoneWyvernEntity;
import mattonfire.dnd.entity.WyvernEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;

/** The Wyvern model at the Bone Wyvern's scale; DragonRenderer fits the parts to the scaled bones. */
public class BoneWyvernRenderer extends DragonRenderer<WyvernEntity> {
    public BoneWyvernRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new WyvernModel());
        this.withScale(BoneWyvernEntity.SCALE);
    }
}
