package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.PhylacteryEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class PhylacteryModel extends DefaultedEntityGeoModel<PhylacteryEntity> {
    public PhylacteryModel() {
        super(new Identifier("dndclasses", "phylactery"));
    }
}
