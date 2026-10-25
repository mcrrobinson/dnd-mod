package mattonfire.dnd.world.underdark.drow;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.StructureProximity;
import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.UnderdarkSite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A Drow Outpost (design 9, 8.1): hollow stalagmite towers joined by web bridges round a spider shrine, with a
 * captive pen and a barracks, on the floor of a wide cavern in the Fungal Forest or the Faerzress Hollows.
 * <p>
 * The site is an {@link UnderdarkSite} with {@link UnderdarkSite.Rules#SETTLEMENT} rules (a cavern floor between
 * y -220 and -110, flat to 4 blocks over 16 of 25 points round it, with 12 blocks of air). It keeps
 * {@value #CLEARANCE} chunks from the rift landings.
 * <p>
 * Keeping apart is one-sided: {@link StructureProximity#nearGenerating} runs the other structure's own site check, so
 * if two structures each checked the other they would recurse forever. Later settlements (the Duergar Holds, the Drow
 * Spire) check {@link DrowOutposts#SET} themselves; the outposts don't check them back.
 */
public class DrowOutpostStructure extends Structure {
    public static final Codec<DrowOutpostStructure> CODEC = createCodec(DrowOutpostStructure::new);

    /** Chunks kept clear of the sets in {@link #KEEP_CLEAR_OF}. */
    public static final int CLEARANCE = 10;
    /** Structure sets an outpost keeps clear of. Never add one that checks the outposts back (see above). */
    static final List<Identifier> KEEP_CLEAR_OF = List.of(id("rift_landings"));

    public DrowOutpostStructure(Config config) {
        super(config);
    }

    private static Identifier id(String path) {
        return new Identifier(DnDClasses.MOD_ID, path);
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
                DrowOutpostPlanner.plan(collector, Random.create(seed), site)));
    }

    @Override
    public StructureType<?> getType() {
        return DrowOutposts.DROW_OUTPOST;
    }
}
