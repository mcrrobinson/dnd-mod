package mattonfire.dnd.world.underdark.feature;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

/**
 * The Underdark's own feature types (design 9 T3). Their configured and placed features are data under
 * {@code worldgen/{configured,placed}_feature/underdark/}, written by {@code tools/underdark_worldgen.py}.
 */
public final class UnderdarkFeatures {
    public static final Feature<DefaultFeatureConfig> GIANT_ZURKHWOOD = new GiantZurkhwoodFeature(
            DefaultFeatureConfig.CODEC);
    public static final Feature<NightlightTowerConfig> NIGHTLIGHT_TOWER = new NightlightTowerFeature(
            NightlightTowerConfig.CODEC);
    public static final Feature<DefaultFeatureConfig> SHELF_FUNGUS = new ShelfFungusFeature(DefaultFeatureConfig.CODEC);

    private UnderdarkFeatures() {
    }

    public static void register() {
        Registry.register(Registries.FEATURE, new Identifier(DnDClasses.MOD_ID, "giant_zurkhwood"), GIANT_ZURKHWOOD);
        Registry.register(Registries.FEATURE, new Identifier(DnDClasses.MOD_ID, "nightlight_tower"), NIGHTLIGHT_TOWER);
        Registry.register(Registries.FEATURE, new Identifier(DnDClasses.MOD_ID, "shelf_fungus"), SHELF_FUNGUS);
    }
}
