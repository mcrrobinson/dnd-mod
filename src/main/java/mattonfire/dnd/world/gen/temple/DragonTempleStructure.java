package mattonfire.dnd.world.gen.temple;

import com.mojang.serialization.Codec;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.StructureProximity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A Dragon Temple ("Shrine of the Three Breaths"): a terrace cut into a mountain slope, reached by a grand
 * stair, with a courtyard of three dragon statues, the sanctum of the three breath altars, the monks' cells
 * and library, and a training yard. The Dragonborn home.
 *
 * <p>A temple only generates where a dragon lair or frost lair really generates within
 * {@link #LAIR_RADIUS} chunks (the lair's own site check is run, not just its grid slot), and never within
 * {@link #FORTRESS_CLEARANCE} chunks of a dwarven fortress. Laid out by {@link DragonTemplePlanner}.
 */
public class DragonTempleStructure extends Structure {
    public static final Codec<DragonTempleStructure> CODEC = createCodec(DragonTempleStructure::new);

    public static final Identifier DRAGON_LAIRS = new Identifier(DnDClasses.MOD_ID, "dragon_lairs");
    public static final Identifier FROST_LAIRS = new Identifier(DnDClasses.MOD_ID, "frost_lairs");
    public static final Identifier DWARVEN_FORTRESSES = new Identifier(DnDClasses.MOD_ID, "dwarven_fortresses");
    /** A lair must start within this many chunks. */
    public static final int LAIR_RADIUS = 10;
    /** No dwarven fortress may start within this many chunks. */
    public static final int FORTRESS_CLEARANCE = 4;

    /** Set by {@code /dndhome templeplace} while it places a temple: skip the lair and fortress rules. */
    public static volatile boolean ignoreNeighbours;

    public DragonTempleStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        TempleTerrain terrain = new TempleTerrain(context);
        // Cheapest first: the biome, then whether any lair grid slot is near at all.
        boolean anywhere = ignoreNeighbours;
        if (!terrain.biomeAllowed(chunk.getCenterX(), chunk.getCenterZ())
                || !anywhere && !StructureProximity.near(context, DRAGON_LAIRS, LAIR_RADIUS) && !StructureProximity.near(context, FROST_LAIRS, LAIR_RADIUS)) {
            return Optional.empty();
        }
        Optional<DragonTemplePlanner.Site> site = terrain.findSite(chunk.getCenterX(), chunk.getCenterZ());
        if (site.isEmpty() || !anywhere && (!nearLair(context) || StructureProximity.nearGenerating(context, DWARVEN_FORTRESSES, FORTRESS_CLEARANCE))) {
            return Optional.empty();
        }
        DragonTemplePlanner.Site s = site.get();
        return Optional.of(new StructurePosition(s.foot(), collector -> DragonTemplePlanner.plan(collector, context.random(), s)));
    }

    /** Whether a dragon lair or frost lair really generates within {@link #LAIR_RADIUS} chunks. */
    public static boolean nearLair(Context context) {
        return StructureProximity.nearGenerating(context, DRAGON_LAIRS, LAIR_RADIUS)
                || StructureProximity.nearGenerating(context, FROST_LAIRS, LAIR_RADIUS);
    }

    @Override
    public StructureType<?> getType() {
        return DragonTempleStructures.DRAGON_TEMPLE;
    }
}
