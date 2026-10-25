package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.MindFlayerEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** geo/entity/mind_flayer.geo.json, animations/entity/mind_flayer.animation.json, textures/entity/mind_flayer.png. */
public class MindFlayerModel extends DefaultedEntityGeoModel<MindFlayerEntity> {
    public MindFlayerModel() {
        // Turns the "head" bone towards where it's looking.
        super(new Identifier("dndclasses", "mind_flayer"), true);
    }
}
