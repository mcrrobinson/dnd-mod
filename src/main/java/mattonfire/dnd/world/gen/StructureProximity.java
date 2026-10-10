package mattonfire.dnd.world.gen;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
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
     * any other placement counts as "not near".
     */
    public static boolean near(Structure.Context context, Identifier set, int radius) {
        StructureSet structures = context.dynamicRegistryManager().get(RegistryKeys.STRUCTURE_SET).get(set);
        if (structures == null || !(structures.placement() instanceof RandomSpreadStructurePlacement spread)) {
            return false;
        }
        ChunkPos chunk = context.chunkPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = chunk.x + dx;
                int cz = chunk.z + dz;
                ChunkPos start = spread.getStartChunk(context.seed(), cx, cz);
                if (start.x == cx && start.z == cz) {
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
        ChunkPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = chunk.x + dx;
                int cz = chunk.z + dz;
                ChunkPos start = spread.getStartChunk(context.seed(), cx, cz);
                int distance = dx * dx + dz * dz;
                if (start.x != cx || start.z != cz || distance >= bestDistance) {
                    continue;
                }
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

    /** Whether a structure of {@code set} really generates within {@code radius} chunks (see {@link #nearestGenerating}). */
    public static boolean nearGenerating(Structure.Context context, Identifier set, int radius) {
        return nearestGenerating(context, set, radius) != null;
    }
}
