package mattonfire.dnd.world.gen.enclave;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the enclave's walkways clear of the forest. The enclave builds in the last worldgen step, after its
 * own chunk's trees, but a tree in a neighbouring chunk that decorates later can still grow its leaves over
 * the Heart Tree's stair. A chunk only loads in full once all its neighbours have decorated, so each time an
 * enclave chunk loads its pieces clear what's grown into them ({@link EnclavePiece#clearRegrowth}). Only
 * forest growth goes (non-persistent leaves, logs, vines), so player-built blocks are left alone.
 */
public final class EnclaveRegrowth {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnclaveRegrowth.class);

    private EnclaveRegrowth() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register(EnclaveRegrowth::onLoad);
    }

    private static void onLoad(ServerWorld world, WorldChunk chunk) {
        Structure enclave = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(ElvenEnclaveStructures.KEY);
        if (enclave == null) {
            return;
        }
        LongSet starts = chunk.getStructureReferences(enclave);
        if (starts == null || starts.isEmpty()) {
            return;
        }
        ChunkPos at = chunk.getPos();
        BlockBox chunkBox = new BlockBox(at.getStartX(), world.getBottomY(), at.getStartZ(), at.getEndX(), world.getTopY(), at.getEndZ());
        for (LongIterator it = starts.iterator(); it.hasNext(); ) {
            ChunkPos startPos = new ChunkPos(it.nextLong());
            // Don't load anything: a start whose chunk isn't in memory is too far off to reach the walkways here.
            Chunk startChunk = world.getChunkManager().getChunk(startPos.x, startPos.z, ChunkStatus.STRUCTURE_STARTS, false);
            StructureStart start = startChunk == null ? null : startChunk.getStructureStart(enclave);
            if (start == null || !start.hasChildren()) {
                continue;
            }
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof EnclavePiece enclavePiece && piece.getBoundingBox().intersects(chunkBox)) {
                    int cleared = enclavePiece.clearRegrowth(world, chunk);
                    if (cleared > 0) {
                        LOGGER.debug("[EnclaveRegrowth] cleared {} forest blocks from {} in chunk {}",
                                cleared, Registries.STRUCTURE_PIECE.getId(piece.getType()), at);
                    }
                }
            }
        }
    }
}
