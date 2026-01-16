package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.WyvernEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class WyvernModel extends GeoModel<WyvernEntity> {
    @Override
    public Identifier getModelResource(WyvernEntity object) {
        return new Identifier("dndclasses", "geo/wyvern.geo.json");
    }

    @Override
    public Identifier getTextureResource(WyvernEntity object) {
        return new Identifier("dndclasses", "textures/entity/wyvern/green.png");
    }

    @Override
    public Identifier getAnimationResource(WyvernEntity object) {
        return new Identifier("dndclasses", "animations/wyvern.animation.json");
    }
}
