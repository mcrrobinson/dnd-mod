package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.LichEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class LichModel extends DefaultedEntityGeoModel<LichEntity> {
    public LichModel() {
        // "head" turns towards where the Lich is looking.
        super(new Identifier("dndclasses", "lich"), true);
    }
}
