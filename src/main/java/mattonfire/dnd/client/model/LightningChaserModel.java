package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.LairDragonEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/** The Lightning Chaser's model and animations, shared by the Frost Drake with its own texture. */
public class LightningChaserModel extends GeoModel<LairDragonEntity> {
    private final Identifier texture;

    public LightningChaserModel() {
        this("textures/entity/lightning_chaser/blue.png");
    }

    public LightningChaserModel(String texture) {
        this.texture = new Identifier("dndclasses", texture);
    }

    @Override
    public Identifier getModelResource(LairDragonEntity object) {
        return new Identifier("dndclasses", "geo/lightning_chaser.geo.json");
    }

    @Override
    public Identifier getTextureResource(LairDragonEntity object) {
        return this.texture;
    }

    @Override
    public Identifier getAnimationResource(LairDragonEntity object) {
        return new Identifier("dndclasses", "animations/lightning_chaser.animation.json");
    }
}
