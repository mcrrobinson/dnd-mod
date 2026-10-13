package mattonfire.dnd.client.model;

import mattonfire.dnd.client.MimicTexture;
import mattonfire.dnd.entity.MimicEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class MimicModel extends GeoModel<MimicEntity> {
    private static final Identifier MODEL = new Identifier("dndclasses", "geo/mimic.geo.json");
    private static final Identifier ANIMATION = new Identifier("dndclasses", "animations/mimic.animation.json");

    @Override
    public Identifier getModelResource(MimicEntity object) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(MimicEntity object) {
        return MimicTexture.get();
    }

    @Override
    public Identifier getAnimationResource(MimicEntity object) {
        return ANIMATION;
    }
}
