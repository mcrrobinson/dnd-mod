package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.MagmamuncherEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class MagmamuncherModel extends GeoModel<MagmamuncherEntity> {
    private static final Identifier MODEL = new Identifier("dndclasses", "geo/magmamuncher.geo.json");
    private static final Identifier TEXTURE = new Identifier("dndclasses", "textures/entity/magmamuncher/magma.png");
    private static final Identifier ANIMATION = new Identifier("dndclasses", "animations/magmamuncher.animation.json");

    @Override
    public Identifier getModelResource(MagmamuncherEntity object) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(MagmamuncherEntity object) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(MagmamuncherEntity object) {
        return ANIMATION;
    }
}
