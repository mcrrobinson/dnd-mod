package mattonfire.dnd.world.gen.enclave;

import java.util.Set;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the elven enclave structure type and its pieces. Where enclaves generate is data:
 * {@code data/dndclasses/worldgen/structure/elven_enclave.json} and the matching structure set.
 */
public final class ElvenEnclaveStructures {
    public static final Identifier ID = id("elven_enclave");
    public static final RegistryKey<Structure> KEY = RegistryKey.of(RegistryKeys.STRUCTURE, ID);
    /** The structure set, which goblin camps keep clear of. */
    public static final Identifier SET = id("elven_enclaves");

    public static final StructureType<ElvenEnclaveStructure> ELVEN_ENCLAVE = Registry.register(
            Registries.STRUCTURE_TYPE, ID, () -> ElvenEnclaveStructure.CODEC);

    public static final StructurePieceType GROUNDS = piece("elven_enclave_grounds", EnclaveGroundsPiece::new);
    public static final StructurePieceType HEART_TREE = piece("elven_heart_tree", HeartTreePiece::new);
    /** The Speaker's Hall up in the Heart Tree: the enclave's hearth. */
    public static final StructurePieceType HALL = piece("elven_speakers_hall", SpeakersHallPiece::new);
    public static final StructurePieceType TALAN = piece("elven_talan", TalanPiece::new);
    public static final StructurePieceType BRIDGE = piece("elven_bridge", BridgePiece::new);
    public static final StructurePieceType MOONWELL = piece("elven_moonwell", MoonwellPiece::new);
    public static final StructurePieceType GLADE = piece("elven_archery_glade", ArcheryGladePiece::new);
    public static final StructurePieceType GARDEN = piece("elven_garden", EnclaveGardenPiece::new);

    /** The pieces whose logs and leaves are living trees under Sylvan Law. */
    public static final Set<StructurePieceType> TREES = Set.of(HEART_TREE, TALAN);

    private ElvenEnclaveStructures() {
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    private static StructurePieceType piece(String name, StructurePieceType.Simple loader) {
        return Registry.register(Registries.STRUCTURE_PIECE, id(name), loader);
    }

    /** Loads the class, registering everything above. */
    public static void register() {
    }
}
