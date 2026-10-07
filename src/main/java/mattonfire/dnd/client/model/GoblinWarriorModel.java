package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.GoblinWarriorEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GoblinWarriorModel extends DefaultedEntityGeoModel<GoblinWarriorEntity> {
    public GoblinWarriorModel() {
        // Turns the "head" bone towards where the goblin is looking.
        super(new Identifier("dndclasses", "goblin_warrior"), true);
    }
}
