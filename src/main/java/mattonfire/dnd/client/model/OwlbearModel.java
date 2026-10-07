package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.OwlbearEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** geo/entity/owlbear.geo.json, animations/entity/owlbear.animation.json, textures/entity/owlbear.png. */
public class OwlbearModel extends DefaultedEntityGeoModel<OwlbearEntity> {
    public OwlbearModel() {
        // Turns the "head" bone towards where the owlbear is looking.
        super(new Identifier("dndclasses", "owlbear"), true);
    }
}
