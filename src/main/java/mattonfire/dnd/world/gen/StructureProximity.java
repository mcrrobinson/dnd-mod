package mattonfire.dnd.world.gen;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.structure.Structure;

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
}
