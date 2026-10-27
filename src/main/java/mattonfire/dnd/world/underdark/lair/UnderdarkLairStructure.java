package mattonfire.dnd.world.underdark.lair;

import java.util.List;
import java.util.Optional;
import mattonfire.dnd.world.gen.StructureProximity;
import mattonfire.dnd.world.underdark.Underdark;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

/**
 * What the Underdark's monster lairs share: they only generate in the Underdark's generator, and they keep
 * {@value #CLEARANCE} chunks from the settlements and the rift landings. Keeping apart is one-sided (see
 * {@code DrowOutpostStructure}): the lairs check the settlements, the settlements never check the lairs, so the checks
 * can't recurse.
 */
public abstract class UnderdarkLairStructure extends Structure {
    /** Chunks kept clear of the sets in {@link #KEEP_CLEAR_OF}. */
    public static final int CLEARANCE = 5;
    /** Structure sets a lair keeps clear of. Never add one that checks the lairs back. */
    static final List<Identifier> KEEP_CLEAR_OF = List.of(UnderdarkLairs.id("rift_landings"),
            UnderdarkLairs.id("drow_outposts"), UnderdarkLairs.id("duergar_holds"), UnderdarkLairs.id("drow_spires"));

    protected UnderdarkLairStructure(Config config) {
        super(config);
    }

    @Override
    protected final Optional<StructurePosition> getStructurePosition(Context context) {
        if (context.world().getBottomY() > Underdark.BOTTOM_Y + 16) {
            return Optional.empty(); // not the Underdark's generator
        }
        Optional<StructurePosition> position = this.site(context);
        if (position.isEmpty()) {
            return position;
        }
        for (Identifier set : KEEP_CLEAR_OF) {
            if (StructureProximity.nearGenerating(context, set, CLEARANCE)) {
                return Optional.empty();
            }
        }
        return position;
    }

    /** Where this lair would go round the context's chunk, and its pieces, if anywhere. */
    protected abstract Optional<StructurePosition> site(Context context);
}
