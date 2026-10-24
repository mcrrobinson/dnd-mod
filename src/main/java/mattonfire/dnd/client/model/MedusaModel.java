package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.MedusaEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** The Medusa (geo, animations and textures from tools/medusa_model.py). */
public class MedusaModel extends DefaultedEntityGeoModel<MedusaEntity> {
    public MedusaModel() {
        // "head" turns towards where she's looking: her gaze follows you.
        super(new Identifier("dndclasses", "medusa"), true);
    }
}
