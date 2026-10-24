package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.BasiliskEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * geo/entity/basilisk.geo.json, animations/entity/basilisk.animation.json, textures/entity/basilisk.png (all from
 * tools/basilisk_model.py).
 */
public class BasiliskModel extends DefaultedEntityGeoModel<BasiliskEntity> {
    public BasiliskModel() {
        // Turns the "head" bone towards where the basilisk is looking.
        super(new Identifier("dndclasses", "basilisk"), true);
    }
}
