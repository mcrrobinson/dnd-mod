package mattonfire.dnd.world.underdark.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.feature.FeatureConfig;

/** A nightlight tower's stem height range: 10-22 in the forests, 6-9 for one a Bluecap grows into. */
public record NightlightTowerConfig(int minHeight, int maxHeight) implements FeatureConfig {
    public static final Codec<NightlightTowerConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(3, 40).fieldOf("min_height").forGetter(NightlightTowerConfig::minHeight),
            Codec.intRange(3, 40).fieldOf("max_height").forGetter(NightlightTowerConfig::maxHeight))
            .apply(instance, NightlightTowerConfig::new));
}
