package mattonfire.dnd.world.underdark.lair;

import com.mojang.serialization.Codec;
import java.util.Optional;
import mattonfire.dnd.world.underdark.UnderdarkSite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A Basilisk Den (design 9, section 10): a pocket of radius {@value BasiliskDenPiece#RADIUS} in a cave floor of the
 * Grey Reaches or the Faerzress Hollows. The site is an {@link UnderdarkSite} with {@link #RULES}: a floor between
 * y -220 and -110, flat to 3 blocks over 15 of 25 points 4 apart, with 7 blocks of air.
 */
public class BasiliskDenStructure extends UnderdarkLairStructure {
    public static final Codec<BasiliskDenStructure> CODEC = createCodec(BasiliskDenStructure::new);
    public static final UnderdarkSite.Rules RULES = new UnderdarkSite.Rules(-220, -110, 5, 4, 15, 3, 7);

    public BasiliskDenStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> site(Context context) {
        Optional<UnderdarkSite.Site> found = UnderdarkSite.find(context, RULES);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        UnderdarkSite.Site site = found.get();
        long seed = context.random().nextLong();
        return Optional.of(new StructurePosition(site.center(), collector -> {
            Random random = Random.create(seed);
            collector.addPiece(new BasiliskDenPiece(site.center(), Direction.Type.HORIZONTAL.random(random),
                    random.nextLong()));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return UnderdarkLairs.BASILISK_DEN;
    }
}
