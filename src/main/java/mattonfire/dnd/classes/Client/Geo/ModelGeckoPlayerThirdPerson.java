package mattonfire.dnd.classes.Client.Geo;


import net.minecraft.util.Identifier;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.DndCharacter;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class ModelGeckoPlayerThirdPerson extends GeoModel<GeoAnimatable> {
    @Override
    public Identifier getAnimationResource(GeoAnimatable entity) {
        throw new UnsupportedOperationException("Animation resource not supported");
    }

    @Override
    public Identifier getModelResource(GeoAnimatable entity) {
        if (entity instanceof PlayerEntityExt player) {
            return GeoModelHelper.getModelLocation(player.getDndClass());
        }
        return new Identifier("dndclasses", "geo/wizard_armor.geo.json");
    }

    @Override
    public Identifier getTextureResource(GeoAnimatable entity) {
        if (entity instanceof PlayerEntityExt player) {
            return GeoModelHelper.getTextureLocation(player.getDndClass());
        }
        return new Identifier("dndclasses", "textures/models/armor/wizard_armor.png");
    }
}