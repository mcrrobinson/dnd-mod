package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.TrollEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** geo/entity/troll.geo.json, animations/entity/troll.animation.json, textures/entity/troll.png. */
public class TrollModel extends DefaultedEntityGeoModel<TrollEntity> {
    public TrollModel() {
        // Turns the "head" bone towards where the troll is looking.
        super(new Identifier("dndclasses", "troll"), true);
    }
}
