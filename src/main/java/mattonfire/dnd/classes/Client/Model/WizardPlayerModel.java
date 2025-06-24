package mattonfire.dnd.classes.Client.Model;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class WizardPlayerModel extends GeoModel<AnimatablePlayerEntity> {
    @Override
    public Identifier getModelResource(AnimatablePlayerEntity animatable) {
        return new Identifier("dnd-mod", "geo/wizard.geo.json");
    }

    @Override
    public Identifier getTextureResource(AnimatablePlayerEntity animatable) {
        return new Identifier("dnd-mod", "textures/entity/wizard.png");
    }

    @Override
    public Identifier getAnimationResource(AnimatablePlayerEntity animatable) {
        return new Identifier("dnd-mod", "animations/wizard.animation.json");
    }
}