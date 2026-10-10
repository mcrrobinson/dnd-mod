package mattonfire.dnd.entity.raid;

import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.enclave.ElvenEnclaveStructures;
import mattonfire.dnd.world.gen.enclave.MoonwellPiece;
import mattonfire.dnd.world.gen.fortress.GatePiece;
import mattonfire.dnd.world.gen.village.VillageGreenPiece;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * A place goblins raid: a hobbit village, a dwarven fortress, an elven enclave, or (for /goblinraid start here)
 * just a spot in the world.
 *
 * @param key     identifies the settlement for raid cooldowns (its structure start chunk)
 * @param rally   where the war party marches to: the village green, or the fortress's gate terrace
 * @param outward direction the war party comes from (out of the fortress gate), or null for any
 */
public record Settlement(Kind kind, long key, BlockPos rally, @Nullable Direction outward, BlockBox bounds) {
    public enum Kind {
        VILLAGE("hobbit_village"),
        FORTRESS("dwarven_fortress"),
        ENCLAVE("elven_enclave"),
        WILDS("wilds");

        public final String id;

        Kind(String id) {
            this.id = id;
        }

        public static Kind byId(String id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return WILDS;
        }

        /** Translation key of the settlement's name, e.g. "the hobbit village". */
        public String nameKey() {
            return "raid.dndclasses.goblin.place." + this.id;
        }
    }

    private static final RegistryKey<Structure> HOBBIT_VILLAGE = RegistryKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "hobbit_village"));
    private static final RegistryKey<Structure> DWARVEN_FORTRESS = RegistryKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "dwarven_fortress"));

    /** A raid on open ground round {@code pos}. */
    public static Settlement wilds(BlockPos pos) {
        return new Settlement(Kind.WILDS, new ChunkPos(pos).toLong(), pos.toImmutable(), null,
                new BlockBox(pos).expand(16));
    }

    /**
     * The hobbit village or dwarven fortress nearest {@code pos} whose bounds, grown by
     * {@code margin} blocks, reach it. Only looks at loaded chunks within {@code chunkRadius}.
     */
    public static Optional<Settlement> find(ServerWorld world, BlockPos pos, int chunkRadius, int margin) {
        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Structure village = registry.get(HOBBIT_VILLAGE);
        Structure fortress = registry.get(DWARVEN_FORTRESS);
        Structure enclave = registry.get(ElvenEnclaveStructures.KEY);
        if (village == null && fortress == null && enclave == null) {
            return Optional.empty();
        }
        ChunkPos center = new ChunkPos(pos);
        StructureStart best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = center.x - chunkRadius; x <= center.x + chunkRadius; x++) {
            for (int z = center.z - chunkRadius; z <= center.z + chunkRadius; z++) {
                if (!world.getChunkManager().isChunkLoaded(x, z)) {
                    continue;
                }
                for (StructureStart start : world.getStructureAccessor().getStructureStarts(new ChunkPos(x, z),
                        structure -> structure == village || structure == fortress || structure == enclave)) {
                    if (!start.hasChildren() || !reaches(start.getBoundingBox(), pos, margin)) {
                        continue;
                    }
                    double distance = start.getBoundingBox().getCenter().getSquaredDistance(pos);
                    if (distance < bestDistance) {
                        best = start;
                        bestDistance = distance;
                    }
                }
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        Kind kind = best.getStructure() == fortress ? Kind.FORTRESS
                : best.getStructure() == enclave ? Kind.ENCLAVE : Kind.VILLAGE;
        return Optional.of(of(best, kind));
    }

    private static boolean reaches(BlockBox box, BlockPos pos, int margin) {
        return pos.getX() >= box.getMinX() - margin && pos.getX() <= box.getMaxX() + margin
                && pos.getZ() >= box.getMinZ() - margin && pos.getZ() <= box.getMaxZ() + margin;
    }

    private static Settlement of(StructureStart start, Kind kind) {
        BlockBox bounds = start.getBoundingBox();
        BlockPos rally = null;
        Direction outward = null;
        for (StructurePiece piece : start.getChildren()) {
            if (kind == Kind.VILLAGE && piece instanceof VillageGreenPiece) {
                // The lawn is the box's bottom layer; stand on it.
                BlockBox box = piece.getBoundingBox();
                rally = new BlockPos(box.getCenter().getX(), box.getMinY() + 1, box.getCenter().getZ());
            } else if (kind == Kind.FORTRESS && piece instanceof GatePiece && piece.getFacing() != null) {
                // The gate faces into the mountain; its terrace is the bottom layer of the box.
                BlockBox box = piece.getBoundingBox();
                rally = new BlockPos(box.getCenter().getX(), box.getMinY() + 1, box.getCenter().getZ());
                outward = piece.getFacing().getOpposite();
            } else if (kind == Kind.ENCLAVE && piece instanceof MoonwellPiece moonwell) {
                // The elves rally to the Moonwell; stand on its rim.
                rally = moonwell.origin().add(MoonwellPiece.RIM, 1, 0);
            }
        }
        if (rally == null) {
            BlockPos c = bounds.getCenter();
            rally = new BlockPos(c.getX(), bounds.getMinY() + 1, c.getZ());
        }
        return new Settlement(kind, start.getPos().toLong(), rally, outward, bounds);
    }
}
