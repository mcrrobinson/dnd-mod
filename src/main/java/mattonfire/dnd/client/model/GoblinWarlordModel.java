package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.GoblinWarlordEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GoblinWarlordModel extends DefaultedEntityGeoModel<GoblinWarlordEntity> {
    public GoblinWarlordModel() {
        // The Goblin Warrior's model with a crown; "head" turns towards where the warlord is looking.
        super(new Identifier("dndclasses", "goblin_warlord"), true);
    }
}
