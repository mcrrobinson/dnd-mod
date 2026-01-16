package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.MagmamuncherEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class MagmamuncherModel extends GeoModel<MagmamuncherEntity> {
    @Override
    public Identifier getModelResource(MagmamuncherEntity object) {
        return new Identifier("dndclasses", "geo/magmamuncher.geo.json");
    }

    @Override
    public Identifier getTextureResource(MagmamuncherEntity object) {
        return new Identifier("dndclasses", "textures/entity/magmamuncher/magmamuncher.png");
    }

    @Override
    public Identifier getAnimationResource(MagmamuncherEntity object) {
        return new Identifier("dndclasses", "animations/magmamuncher.animation.json");
    }
}
