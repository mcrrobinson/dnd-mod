package mattonfire.dnd.world.underdark.drow;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import mattonfire.dnd.world.gen.StructureProximity;
import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.UnderdarkSite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * The Drow Spire (design 9, 8.3): a hanging spire of drowstone and dripstone in a great dome of the Faerzress Hollows,
 * three minor towers bridged to its middle, a spider pit at its foot, and the Matron Mother in the Sanctum at its top.
 * <p>
 * The site is an {@link UnderdarkSite} like a settlement's ({@link #RULES}: a cavern floor between y -220 and
 * {@value #MAX_FLOOR}, flat to 4 blocks over 16 of 25 points, 12 blocks of air); the cavern is rarely 44 blocks
 * tall, so {@link DrowSpireCavernPiece} carves the dome it needs. It keeps {@value #CLEARANCE} chunks from the Drow
 * Outposts and the rift landings. Keeping apart is one-sided ({@link DrowOutpostStructure}): the spire checks them, they
 * never check the spire.
 */
public class DrowSpireStructure extends Structure {
    public static final Codec<DrowSpireStructure> CODEC = createCodec(DrowSpireStructure::new);

    /** The highest floor: the dome reaches 46 blocks above it, under the Underdark's roof. */
    public static final int MAX_FLOOR = -125;
    public static final UnderdarkSite.Rules RULES = new UnderdarkSite.Rules(-220, MAX_FLOOR, 5, 8, 16, 4, 12);
    /** Chunks kept clear of the sets in {@link #KEEP_CLEAR_OF}. */
    public static final int CLEARANCE = 8;
    /** Structure sets the spire keeps clear of. Never add one that checks the spire back. */
    static final List<Identifier> KEEP_CLEAR_OF = List.of(DrowOutposts.SET, DrowOutposts.id("rift_landings"));

    public DrowSpireStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        if (context.world().getBottomY() > Underdark.BOTTOM_Y + 16) {
            return Optional.empty(); // not the Underdark's generator
        }
        Optional<UnderdarkSite.Site> found = UnderdarkSite.find(context, RULES);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        for (Identifier set : KEEP_CLEAR_OF) {
            if (StructureProximity.nearGenerating(context, set, CLEARANCE)) {
                return Optional.empty();
            }
        }
        UnderdarkSite.Site site = found.get();
        long seed = context.random().nextLong();
        return Optional.of(new StructurePosition(site.center(), collector ->
                DrowSpirePlanner.plan(collector, Random.create(seed), site)));
    }

    @Override
    public StructureType<?> getType() {
        return DrowSpires.DROW_SPIRE;
    }
}
