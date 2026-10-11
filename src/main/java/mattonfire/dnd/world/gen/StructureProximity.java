package mattonfire.dnd.world.gen;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import java.util.stream.Stream;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps structures apart while they're being placed. A structure set can only exclude one other set
 * (vanilla villages use that slot), so a structure that must stay clear of another (goblin camps and
 * hobbit villages; later elven enclaves and camps, dragon temples and lairs) asks the other set's
 * spread placement which chunks it would start in.
 */
public final class StructureProximity {
    private StructureProximity() {
    }

    /**
     * Whether a structure of the {@code set} structure set (e.g. {@code dndclasses:hobbit_villages}) may
     * start within {@code radius} chunks of the chunk being placed. Only random-spread sets are checked;
     * any other placement counts as "not near". A grid slot counts only if vanilla would try a start there
     * (see {@link #startsAt}).
     */
    public static boolean near(Structure.Context context, Identifier set, int radius) {
        StructureSet structures = context.dynamicRegistryManager().get(RegistryKeys.STRUCTURE_SET).get(set);
        if (structures == null || !(structures.placement() instanceof RandomSpreadStructurePlacement spread)) {
            return false;
        }
        ChunkPos chunk = context.chunkPos();
        StructurePlacementCalculator placements = calculator(context);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (startsAt(placements, spread, chunk.x + dx, chunk.z + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Like {@link #near}, but only counts a start of the {@code set} structure set that really generates
     * there: its structure is asked for a valid position, so a lair grid slot with no peak doesn't count.
     * Costs a site check per start found, so call it after the cheaper checks. Returns the start chunk of
     * the nearest one, or null.
     */
    @Nullable
    public static ChunkPos nearestGenerating(Structure.Context context, Identifier set, int radius) {
        StructureSet structures = context.dynamicRegistryManager().get(RegistryKeys.STRUCTURE_SET).get(set);
        if (structures == null || !(structures.placement() instanceof RandomSpreadStructurePlacement spread)) {
            return null;
        }
        ChunkPos chunk = context.chunkPos();
        StructurePlacementCalculator placements = calculator(context);
        ChunkPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = chunk.x + dx;
                int cz = chunk.z + dz;
                int distance = dx * dx + dz * dz;
                if (distance >= bestDistance || !startsAt(placements, spread, cx, cz)) {
                    continue;
                }
                ChunkPos start = new ChunkPos(cx, cz);
                for (StructureSet.WeightedEntry entry : structures.structures()) {
                    Structure other = entry.structure().value();
                    Structure.Context there = new Structure.Context(context.dynamicRegistryManager(), context.chunkGenerator(),
                            context.biomeSource(), context.noiseConfig(), context.structureTemplateManager(),
                            context.seed(), start, context.world(), other.getValidBiomes()::contains);
                    if (other.getValidStructurePosition(there).isPresent()) {
                        best = start;
                        bestDistance = distance;
                        break;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Whether vanilla would try to start a structure of the set at chunk ({@code cx}, {@code cz}): it's the
     * grid slot's start chunk, passes the frequency roll, and isn't in the set's {@code exclusion_zone}
     * (vanilla excludes a start near any grid slot of the other set, whether or not that one generates).
     */
    private static boolean startsAt(StructurePlacementCalculator placements, RandomSpreadStructurePlacement spread, int cx, int cz) {
        return spread.shouldGenerate(placements, cx, cz);
    }

    /**
     * A placement calculator for the world seed. Random-spread placements only read its seed (and other sets'
     * placements through their exclusion zones), so it needs no structure sets of its own.
     */
    private static StructurePlacementCalculator calculator(Structure.Context context) {
        return StructurePlacementCalculator.create(context.noiseConfig(), context.seed(), context.biomeSource(), Stream.empty());
    }

    /** Whether a structure of {@code set} really generates within {@code radius} chunks (see {@link #nearestGenerating}). */
    public static boolean nearGenerating(Structure.Context context, Identifier set, int radius) {
        return nearestGenerating(context, set, radius) != null;
    }
}
