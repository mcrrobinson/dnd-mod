package mattonfire.dnd.world.gen.warren;

import mattonfire.dnd.entity.kobold.KoboldEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/** Finding kobold warrens in a loaded world: which one a spot is in, its pieces and bolt-holes. */
public final class KoboldWarren {
    /** A hurt kobold runs for a bolt-hole this close. */
    public static final double REFUGE_RANGE = 20.0D;

    private KoboldWarren() {
    }

    @Nullable
    private static Structure structure(ServerWorld world) {
        return world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(KoboldWarrens.KEY);
    }

    /** The warren with a piece round {@code pos}, or null. */
    @Nullable
    public static StructureStart startAt(ServerWorld world, BlockPos pos) {
        Structure warren = structure(world);
        if (warren == null || !world.isChunkLoaded(pos)) {
            return null;
        }
        for (StructureStart start : world.getStructureAccessor().getStructureStarts(ChunkSectionPos.from(pos), warren)) {
            for (StructurePiece piece : start.getChildren()) {
                if (piece.getBoundingBox().contains(pos)) {
                    return start;
                }
            }
        }
        return null;
    }

    /** Whether {@code pos} is inside a piece of a kobold warren (the kobolds' home: they're always brave there). */
    public static boolean contains(ServerWorld world, BlockPos pos) {
        return startAt(world, pos) != null;
    }

    /** The piece of type {@code type} round {@code pos}, or null. */
    @Nullable
    public static <T extends StructurePiece> T pieceAt(ServerWorld world, BlockPos pos, Class<T> type) {
        StructureStart start = startAt(world, pos);
        if (start == null) {
            return null;
        }
        for (StructurePiece piece : start.getChildren()) {
            if (type.isInstance(piece) && piece.getBoundingBox().contains(pos)) {
                return type.cast(piece);
            }
        }
        return null;
    }

    /** The first piece of {@code type} in {@code start}, or null. */
    @Nullable
    public static <T extends StructurePiece> T first(StructureStart start, Class<T> type) {
        for (StructurePiece piece : start.getChildren()) {
            if (type.isInstance(piece)) {
                return type.cast(piece);
            }
        }
        return null;
    }

    /** In a warren: the nearest bolt-hole cell within {@value #REFUGE_RANGE} blocks, for a hurt kobold to run to. */
    @Nullable
    public static Vec3d refuge(KoboldEntity kobold) {
        if (!(kobold.world instanceof ServerWorld world)) {
            return null;
        }
        StructureStart start = startAt(world, kobold.getBlockPos());
        if (start == null) {
            return null;
        }
        BlockPos best = null;
        double bestSq = REFUGE_RANGE * REFUGE_RANGE;
        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof BoltHolePiece hole) {
                for (BlockPos cell : hole.cells()) {
                    double sq = cell.getSquaredDistance(kobold.getPos());
                    if (sq < bestSq) {
                        bestSq = sq;
                        best = cell;
                    }
                }
            }
        }
        return best == null ? null : Vec3d.ofBottomCenter(best);
    }
}
