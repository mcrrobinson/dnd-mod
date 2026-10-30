package mattonfire.dnd.client.kobold;

import mattonfire.dnd.entity.kobold.KoboldEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * geo/entity/kobold.geo.json and animations/entity/kobold.animation.json ({@code tools/kobold_model.py}), shared by
 * all four kobolds, with the skin {@code textures/entity/kobold/kobold_<variant>.png} ({@code tools/kobold_skins.py}).
 */
public class KoboldModel extends DefaultedEntityGeoModel<KoboldEntity> {
    private static final Identifier[] TEXTURES = new Identifier[KoboldEntity.VARIANTS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = new Identifier("dndclasses", "textures/entity/kobold/kobold_" + i + ".png");
        }
    }

    public KoboldModel() {
        // Turns the "head" bone towards where the kobold is looking.
        super(new Identifier("dndclasses", "kobold"), true);
    }

    @Override
    public Identifier getTextureResource(KoboldEntity kobold) {
        return TEXTURES[Math.floorMod(kobold.getVariant(), TEXTURES.length)];
    }
}
