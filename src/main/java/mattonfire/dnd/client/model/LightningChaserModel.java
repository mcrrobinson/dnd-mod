package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.LightningChaserEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class LightningChaserModel extends GeoModel<LightningChaserEntity> {
    @Override
    public Identifier getModelResource(LightningChaserEntity object) {
        return new Identifier("dndclasses", "geo/lightning_chaser.geo.json");
    }

    @Override
    public Identifier getTextureResource(LightningChaserEntity object) {
        return new Identifier("dndclasses", "textures/entity/lightning_chaser/blue.png");
    }

    @Override
    public Identifier getAnimationResource(LightningChaserEntity object) {
        return new Identifier("dndclasses", "animations/lightning_chaser.animation.json");
    }
}
