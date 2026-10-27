package mattonfire.dnd.world.underdark.lair;

import com.mojang.serialization.Codec;
import java.util.Optional;
import mattonfire.dnd.world.underdark.UnderdarkSite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.StructureType;

/**
 * An Illithid Colony (design 9, section 10): a cluster of organic chambers on a wide cavern floor in the Faerzress
 * Hollows. The site is an {@link UnderdarkSite} with {@link UnderdarkSite.Rules#SETTLEMENT} rules (a floor between
 * y -220 and -110, flat to 4 blocks over 16 of 25 points 8 apart, 12 blocks of air).
 */
public class IllithidColonyStructure extends UnderdarkLairStructure {
    public static final Codec<IllithidColonyStructure> CODEC = createCodec(IllithidColonyStructure::new);

    public IllithidColonyStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> site(Context context) {
        Optional<UnderdarkSite.Site> found = UnderdarkSite.find(context, UnderdarkSite.Rules.SETTLEMENT);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        UnderdarkSite.Site site = found.get();
        long seed = context.random().nextLong();
        return Optional.of(new StructurePosition(site.center(), collector -> {
            Random random = Random.create(seed);
            collector.addPiece(new IllithidColonyPiece(site.center(), Direction.Type.HORIZONTAL.random(random),
                    random.nextLong()));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return UnderdarkLairs.ILLITHID_COLONY;
    }
}
