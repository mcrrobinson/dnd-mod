package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.dragon.AncientDragonEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * The Ancient dragons' model: the Lightning Chaser's skeleton with thickened cubes, horns, frills, spines and a
 * tail blade (tools/ancient_dragon_model.py), and the Lightning Chaser's animations. The texture comes from the
 * dragon's colour (tools/ancient_dragon_texture.py).
 */
public class AncientDragonModel extends GeoModel<AncientDragonEntity> {
    private static final Identifier MODEL = new Identifier("dndclasses", "geo/ancient_dragon.geo.json");
    private static final Identifier ANIMATION = new Identifier("dndclasses", "animations/ancient_dragon.animation.json");

    @Override
    public Identifier getModelResource(AncientDragonEntity dragon) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(AncientDragonEntity dragon) {
        return dragon.getColor().texture();
    }

    @Override
    public Identifier getAnimationResource(AncientDragonEntity dragon) {
        return ANIMATION;
    }
}
