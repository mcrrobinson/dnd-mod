package mattonfire.dnd.client.model;

import mattonfire.dnd.client.MimicTexture;
import mattonfire.dnd.entity.MimicEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class MimicModel extends GeoModel<MimicEntity> {
    @Override
    public Identifier getModelResource(MimicEntity object) {
        return new Identifier("dndclasses", "geo/mimic.geo.json");
    }

    @Override
    public Identifier getTextureResource(MimicEntity object) {
        return MimicTexture.get();
    }

    @Override
    public Identifier getAnimationResource(MimicEntity object) {
        return new Identifier("dndclasses", "animations/mimic.animation.json");
    }
}
