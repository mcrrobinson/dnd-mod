package mattonfire.dnd.world.underdark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.Structure;

/**
 * Finds an open cavern floor for an Underdark settlement or lair (design 9, section 8; shared by the Drow Outposts,
 * the Duergar Holds, the Drow Spire and the monster lairs).
 * <p>
 * It reads the noise columns ({@link net.minecraft.world.gen.chunk.ChunkGenerator#getColumnSample}, so before carvers
 * and features), cached per column, on a {@link Rules#grid} x {@link Rules#grid} grid {@link Rules#spacing} blocks
 * apart round a centre. A column's <i>floors</i> are its solid blocks with air (not water) above them. The centre's
 * floors between {@link Rules#minY} and {@link Rules#maxY} with {@link Rules#headroom} blocks of air are the candidate
 * levels; a grid point matches a level when it has a floor within {@link Rules#tolerance} of it with that much air too.
 * The level with the most matches wins if it has at least {@link Rules#minMatches}: the site's floor is the median of
 * the matched floors. Pieces then level the ground themselves (fill a few blocks under, clear above, seal liquids).
 * <p>
 * {@link #inSettlement} keeps the giant fungi out of the chunks a settlement covers, so they don't grow through it
 * after it's built.
 */
public final class UnderdarkSite {
    /**
     * Structures the Underdark's giant-fungus features keep out of ({@code #dndclasses:underdark/settlements}): the
     * Drow Outposts, and the Duergar Holds and the Drow Spire when they exist (optional entries).
     */
    public static final TagKey<Structure> SETTLEMENTS = TagKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "underdark/settlements"));

    /**
     * What a site needs.
     *
     * @param minY       lowest floor (the floor block's y)
     * @param maxY       highest floor
     * @param grid       grid points per side (odd)
     * @param spacing    blocks between grid points
     * @param minMatches grid points that must match the level (out of grid x grid)
     * @param tolerance  how far a point's floor may lie above or below the level
     * @param headroom   air above a floor (not counting the floor block)
     */
    public record Rules(int minY, int maxY, int grid, int spacing, int minMatches, int tolerance, int headroom) {
        /** A settlement's cavern (design 9, 8): y -220 to -110, 16 of 25 points within 4 blocks, 12 blocks of air. */
        public static final Rules SETTLEMENT = new Rules(-220, -110, 5, 8, 16, 4, 12);

        public Rules withHeadroom(int headroom) {
            return new Rules(this.minY, this.maxY, this.grid, this.spacing, this.minMatches, this.tolerance, headroom);
        }
    }

    /**
     * A site found.
     *
     * @param center   the centre column at standing height (one above the floor block)
     * @param floorY   the floor block's y
     * @param matches  grid points that matched
     * @param headroom the least air over a matched floor (capped at {@value #AIR_CAP})
     */
    public record Site(BlockPos center, int floorY, int matches, int headroom) {
    }

    /** Air runs are counted up to this many blocks. */
    public static final int AIR_CAP = 48;

    private final Structure.Context context;
    /** Per column: its floors as {y, air above}, top down. */
    private final Map<Long, List<int[]>> floors = new HashMap<>();

    public UnderdarkSite(Structure.Context context) {
        this.context = context;
    }

    /**
     * The site nearest the centre of the context's chunk: the centre itself, else the centre moved one grid spacing
     * east, west, south or north (the columns are shared, so each try costs one more row of samples).
     */
    public static Optional<Site> find(Structure.Context context, Rules rules) {
        UnderdarkSite site = new UnderdarkSite(context);
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        int s = rules.spacing();
        for (int[] shift : new int[][] {{0, 0}, {s, 0}, {-s, 0}, {0, s}, {0, -s}}) {
            Optional<Site> found = site.at(x + shift[0], z + shift[1], rules);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** The site centred on column x, z, if it is one. */
    public Optional<Site> at(int x, int z, Rules rules) {
        int half = rules.grid() / 2;
        Site best = null;
        for (int[] floor : this.floors(x, z)) {
            int level = floor[0];
            if (level < rules.minY() || level > rules.maxY() || floor[1] < rules.headroom()) {
                continue;
            }
            List<Integer> ys = new ArrayList<>();
            int air = AIR_CAP;
            for (int gx = -half; gx <= half; gx++) {
                for (int gz = -half; gz <= half; gz++) {
                    int[] match = this.match(x + gx * rules.spacing(), z + gz * rules.spacing(), level, rules);
                    if (match != null) {
                        ys.add(match[0]);
                        air = Math.min(air, match[1]);
                    }
                }
            }
            if (ys.size() >= rules.minMatches() && (best == null || ys.size() > best.matches())) {
                Collections.sort(ys);
                int median = ys.get(ys.size() / 2);
                best = new Site(new BlockPos(x, median + 1, z), median, ys.size(), air);
            }
        }
        return Optional.ofNullable(best);
    }

    /** The floor in column x, z nearest {@code level} (within the tolerance, with the headroom), or null. */
    private int[] match(int x, int z, int level, Rules rules) {
        int[] best = null;
        for (int[] floor : this.floors(x, z)) {
            int d = Math.abs(floor[0] - level);
            if (d <= rules.tolerance() && floor[1] >= rules.headroom()
                    && (best == null || d < Math.abs(best[0] - level))) {
                best = floor;
            }
        }
        return best;
    }

    /** Column x, z's floors as {y, air above (capped)}, from the top down. */
    public List<int[]> floors(int x, int z) {
        return this.floors.computeIfAbsent(ChunkPos.toLong(x, z), key -> {
            VerticalBlockSample column = this.context.chunkGenerator().getColumnSample(x, z, this.context.world(),
                    this.context.noiseConfig());
            List<int[]> found = new ArrayList<>();
            int bottom = this.context.world().getBottomY();
            int air = 0;
            for (int y = this.context.world().getTopY() - 1; y >= bottom; y--) {
                BlockState state = column.getState(y);
                if (state.isAir()) {
                    air++;
                } else if (!state.getFluidState().isEmpty()) {
                    air = 0;
                } else {
                    if (air > 0) {
                        found.add(new int[] {y, Math.min(air, AIR_CAP)});
                    }
                    air = 0;
                }
            }
            return found;
        });
    }

    /**
     * Whether {@code pos} is in or next to a chunk an Underdark settlement ({@link #SETTLEMENTS}) covers, during world
     * generation.
     * The giant fungi ask before they grow, so they don't sprout through a settlement built before them. Outside
     * world generation (bone meal) it's always false.
     */
    public static boolean inSettlement(StructureWorldAccess world, BlockPos pos) {
        if (!(world instanceof ChunkRegion)) {
            return false;
        }
        Registry<Structure> structures = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        // This chunk and the 8 round it: a giant fungus leans and spreads its cap into its neighbours.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk chunk = world.getChunk((pos.getX() >> 4) + dx, (pos.getZ() >> 4) + dz);
                for (Structure structure : chunk.getStructureReferences().keySet()) {
                    if (tagged(structures, structure)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean tagged(Registry<Structure> structures, Structure structure) {
        Optional<RegistryKey<Structure>> key = structures.getKey(structure);
        return key.isPresent() && structures.getEntry(key.get()).map(entry -> entry.isIn(SETTLEMENTS)).orElse(false);
    }
}
