package mattonfire.dnd.world.underdark.duergar;

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
 * A Duergar Hold (design 9, 8.2): a walled forge-town of the Grey Clans against a lava lake, on the floor of a wide
 * cavern in the Grey Reaches.
 * <p>
 * The site is an {@link UnderdarkSite} with {@link UnderdarkSite.Rules#SETTLEMENT} rules (a cavern floor between
 * y -220 and -110, flat to 4 blocks over 16 of 25 points round it, with 12 blocks of air). It keeps
 * {@value #CLEARANCE} chunks from the Drow Outposts and the rift landings.
 * <p>
 * Keeping apart is one-sided: {@link StructureProximity#nearGenerating} runs the other structure's own site check, so
 * two structures that checked each other would recurse forever. The holds check the outposts and the landings; neither
 * checks the holds back, and nothing may.
 */
public class DuergarHoldStructure extends Structure {
    public static final Codec<DuergarHoldStructure> CODEC = createCodec(DuergarHoldStructure::new);

    /** Chunks kept clear of the sets in {@link #KEEP_CLEAR_OF}. */
    public static final int CLEARANCE = 10;
    /** Structure sets a hold keeps clear of. Never add one that checks the holds back (see above). */
    static final List<Identifier> KEEP_CLEAR_OF = List.of(DuergarHolds.id("drow_outposts"), DuergarHolds.id("rift_landings"));

    public DuergarHoldStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        if (context.world().getBottomY() > Underdark.BOTTOM_Y + 16) {
            return Optional.empty(); // not the Underdark's generator
        }
        Optional<UnderdarkSite.Site> found = UnderdarkSite.find(context, UnderdarkSite.Rules.SETTLEMENT);
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
                DuergarHoldPlanner.plan(collector, Random.create(seed), site)));
    }

    @Override
    public StructureType<?> getType() {
        return DuergarHolds.DUERGAR_HOLD;
    }
}
