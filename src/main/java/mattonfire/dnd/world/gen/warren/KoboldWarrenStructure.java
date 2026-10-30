package mattonfire.dnd.world.gen.warren;

import com.mojang.serialization.Codec;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.StructureProximity;
import mattonfire.dnd.world.gen.TerrainColumns;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A kobold warren (design 10, 4.3): a cave mouth in a cliff or steep slope of the hills, badlands and stony peaks,
 * with a tunnel down to a den and the rooms round it ({@link KoboldWarrenPlanner}).
 * <p>
 * The mountainside test: round the chunk's middle (&plusmn;{@value #SEARCH} blocks, every {@value #STEP}), a column
 * {@code c} and a direction {@code d} where the ground {@value #RISE_DISTANCE} blocks along {@code d} stands at least
 * {@value #RISE} blocks higher than at {@code c}: a cliff or slope face. {@code c} must be dry, at least sea level +
 * {@value #ABOVE_SEA}, in an allowed biome, with open ground in front, and the hill must cover the rooms further in.
 * The mouth faces {@code -d} and the warren digs into the hill along {@code d}. Directions are the four compass
 * points the pieces can be turned to.
 * <p>
 * A <b>great warren</b> (one in {@value #GREAT_CHANCE_PERCENT}, and any within {@value #ANCIENT_LAIR_CHUNKS} chunks of
 * an Ancient's lair, once the {@code dndclasses:ancient_lairs} set exists) has a guard hall, more kobolds and tier 2
 * traps.
 */
public class KoboldWarrenStructure extends Structure {
    public static final Codec<KoboldWarrenStructure> CODEC = createCodec(KoboldWarrenStructure::new);

    public static final int SEARCH = 16;
    public static final int STEP = 4;
    public static final int RISE = 10;
    public static final int RISE_DISTANCE = 10;
    public static final int ABOVE_SEA = 4;
    /** How far in the hill must still stand over the den's ceiling (depth into the hill, blocks). */
    private static final int[] COVER_DEPTHS = {20, 30, 40};
    /** The hill over those spots must stand at least this far above the mouth floor. */
    private static final int COVER = 2;
    public static final int GREAT_CHANCE_PERCENT = 20;
    public static final int ANCIENT_LAIR_CHUNKS = 12;
    /** Chunks kept clear between a warren and other settlements and lairs. */
    public static final int CLEARANCE = 4;
    private static final Identifier[] KEEP_CLEAR = {id("goblin_camps"), id("dwarven_fortresses"), id("cogsdeeps"),
            id("hobbit_villages"), id("orc_strongholds"), id("dragon_lairs")};
    /** D2's lairs (a missing set counts as none near). */
    public static final Identifier ANCIENT_LAIRS = id("ancient_lairs");

    public KoboldWarrenStructure(Config config) {
        super(config);
    }

    private static Identifier id(String path) {
        return new Identifier(DnDClasses.MOD_ID, path);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        TerrainColumns columns = new TerrainColumns(context);
        int cx = chunk.getCenterX();
        int cz = chunk.getCenterZ();
        if (!anyBiomeAllowed(columns, cx, cz)) {
            return Optional.empty();
        }
        for (Identifier set : KEEP_CLEAR) {
            if (StructureProximity.near(context, set, CLEARANCE)) {
                return Optional.empty();
            }
        }
        KoboldWarrenPlanner.Site site = findSite(columns, cx, cz, context.chunkGenerator().getSeaLevel() + ABOVE_SEA);
        if (site == null) {
            return Optional.empty();
        }
        boolean great = context.random().nextInt(100) < GREAT_CHANCE_PERCENT
                || StructureProximity.nearGenerating(context, ANCIENT_LAIRS, ANCIENT_LAIR_CHUNKS);
        KoboldWarrenPlanner.Site chosen = new KoboldWarrenPlanner.Site(site.mouth(), site.in(), great, site.rise());
        long seed = context.random().nextLong();
        return Optional.of(new StructurePosition(chosen.mouth(),
                collector -> KoboldWarrenPlanner.plan(collector, seed, chosen, columns)));
    }

    /**
     * Whether the middle or a corner of the search square is in an allowed biome. Cliffs often stand where the hills
     * meet the lowland, so the middle alone would miss the edge of the hills; {@link #findSite} checks each mouth.
     */
    private static boolean anyBiomeAllowed(TerrainColumns columns, int cx, int cz) {
        return columns.biomeAllowed(cx, cz) || columns.biomeAllowed(cx - SEARCH, cz - SEARCH)
                || columns.biomeAllowed(cx + SEARCH, cz - SEARCH) || columns.biomeAllowed(cx - SEARCH, cz + SEARCH)
                || columns.biomeAllowed(cx + SEARCH, cz + SEARCH);
    }

    /** The best mouth site round x/z, or null. Public for the survey. */
    public static KoboldWarrenPlanner.Site findSite(TerrainColumns columns, int cx, int cz, int minY) {
        KoboldWarrenPlanner.Site best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int dx = -SEARCH; dx <= SEARCH; dx += STEP) {
            for (int dz = -SEARCH; dz <= SEARCH; dz += STEP) {
                int x = cx + dx;
                int z = cz + dz;
                if (columns.water(x, z)) {
                    continue;
                }
                int y = columns.ground(x, z);
                if (y < minY) {
                    continue;
                }
                for (Direction in : Direction.Type.HORIZONTAL) {
                    int ox = in.getOffsetX();
                    int oz = in.getOffsetZ();
                    int rise = columns.ground(x + ox * RISE_DISTANCE, z + oz * RISE_DISTANCE) - y;
                    if (rise < RISE) {
                        continue;
                    }
                    // Open ground in front of the mouth, not a wall or water
                    int front = columns.ground(x - ox * 3, z - oz * 3);
                    if (front > y + 2 || columns.water(x - ox * 3, z - oz * 3)) {
                        continue;
                    }
                    int cover = Integer.MAX_VALUE;
                    for (int depth : COVER_DEPTHS) {
                        cover = Math.min(cover, columns.ground(x + ox * depth, z + oz * depth) - y);
                    }
                    if (cover < COVER) {
                        continue;
                    }
                    int score = Math.min(rise, 30) + Math.min(cover, 20) - (Math.abs(dx) + Math.abs(dz)) / 4;
                    if (score > bestScore && columns.biomeAllowed(x, z)) {
                        bestScore = score;
                        best = new KoboldWarrenPlanner.Site(new BlockPos(x, y, z), in, false, rise);
                    }
                }
            }
        }
        return best;
    }

    @Override
    public StructureType<?> getType() {
        return KoboldWarrens.KOBOLD_WARREN;
    }
}
