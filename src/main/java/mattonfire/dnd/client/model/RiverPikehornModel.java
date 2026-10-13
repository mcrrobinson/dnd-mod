package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.RiverPikehornEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class RiverPikehornModel extends GeoModel<RiverPikehornEntity> {
    private static final Identifier MODEL = new Identifier("dndclasses", "geo/river_pikehorn.geo.json");
    private static final Identifier TEXTURE = new Identifier("dndclasses", "textures/entity/river_pikehorn/green.png");
    private static final Identifier ANIMATION = new Identifier("dndclasses", "animations/river_pikehorn.animation.json");

    @Override
    public Identifier getModelResource(RiverPikehornEntity object) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(RiverPikehornEntity object) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(RiverPikehornEntity object) {
        return ANIMATION;
    }
}
