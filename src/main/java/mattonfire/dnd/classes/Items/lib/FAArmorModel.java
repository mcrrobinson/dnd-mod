package mattonfire.dnd.classes.Items.lib;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class FAArmorModel <T extends FAArmorItem> extends GeoModel<T> {
    private final String modelPath;
    private final String texturePath;
    private final String animationPath;

    public FAArmorModel(String modelPath, String texturePath) {
        this(modelPath, texturePath, null);
    }

    public FAArmorModel(String modelPath, String texturePath, String animationPath) {
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.animationPath = animationPath;
    }

    @Override
    public Identifier getModelResource(T animatable) {
        return new Identifier(DnDClasses.MOD_ID, modelPath);
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return new Identifier(DnDClasses.MOD_ID, texturePath);
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        if (animationPath != null) {
            return new Identifier(DnDClasses.MOD_ID, animationPath);
        }
        return null;
    }
}