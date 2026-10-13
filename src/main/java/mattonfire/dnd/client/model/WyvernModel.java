package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.WyvernEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class WyvernModel extends GeoModel<WyvernEntity> {
    private static final Identifier MODEL = new Identifier("dndclasses", "geo/wyvern.geo.json");
    private static final Identifier ANIMATION = new Identifier("dndclasses", "animations/wyvern.animation.json");

    @Override
    public Identifier getModelResource(WyvernEntity object) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(WyvernEntity object) {
        return object.getTexture();
    }

    @Override
    public Identifier getAnimationResource(WyvernEntity object) {
        return ANIMATION;
    }
}
