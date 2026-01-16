package mattonfire.dnd.classes.Client.Player;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public abstract class GeckoPlayer implements GeoEntity {
    protected GeoRenderer<GeckoPlayer> renderer;
    protected GeoModel<GeckoPlayer> model;

}