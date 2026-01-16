package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.RiverPikehornEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class RiverPikehornModel extends GeoModel<RiverPikehornEntity> {
    @Override
    public Identifier getModelResource(RiverPikehornEntity object) {
        return new Identifier("dndclasses", "geo/river_pikehorn.geo.json");
    }

    @Override
    public Identifier getTextureResource(RiverPikehornEntity object) {
        return new Identifier("dndclasses", "textures/entity/river_pikehorn/green.png");
    }

    @Override
    public Identifier getAnimationResource(RiverPikehornEntity object) {
        return new Identifier("dndclasses", "animations/river_pikehorn.animation.json");
    }
}
