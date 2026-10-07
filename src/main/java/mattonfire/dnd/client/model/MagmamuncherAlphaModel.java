package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.MagmamuncherAlphaEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/** The regular Magmamuncher's model, texture and animations; the renderer scales it up. */
public class MagmamuncherAlphaModel extends GeoModel<MagmamuncherAlphaEntity> {
    private static final Identifier MODEL = new Identifier("dndclasses", "geo/magmamuncher.geo.json");
    private static final Identifier TEXTURE = new Identifier("dndclasses", "textures/entity/magmamuncher/magma.png");
    private static final Identifier ANIMATION = new Identifier("dndclasses", "animations/magmamuncher.animation.json");

    @Override
    public Identifier getModelResource(MagmamuncherAlphaEntity object) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(MagmamuncherAlphaEntity object) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(MagmamuncherAlphaEntity object) {
        return ANIMATION;
    }
}
