package mattonfire.dnd.world.gen.burrow;

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
 * Registers the Cogsdeep structure type and its pieces. Where burrows generate is data:
 * {@code data/dndclasses/worldgen/structure/cogsdeep.json} and the matching structure set; the site check is in
 * {@link CogsdeepStructure}.
 */
public final class CogsdeepStructures {
    public static final Identifier ID = id("cogsdeep");
    public static final RegistryKey<Structure> KEY = RegistryKey.of(RegistryKeys.STRUCTURE, ID);
    public static final Identifier SET = id("cogsdeeps");

    public static final StructureType<CogsdeepStructure> COGSDEEP = Registry.register(
            Registries.STRUCTURE_TYPE, ID, () -> CogsdeepStructure.CODEC);

    /** The copper-domed observatory on the surface. */
    public static final StructurePieceType OBSERVATORY = piece("cogsdeep_observatory", ObservatoryPiece::new);
    /** The spiral stair from the observatory down to the Commons. */
    public static final StructurePieceType SHAFT = piece("cogsdeep_shaft", ShaftPiece::new);
    /** The Commons at the foot of the stair: the burrow's hearth. */
    public static final StructurePieceType COMMONS = piece("cogsdeep_commons", CommonsPiece::new);
    /** One of the two workshop halls. */
    public static final StructurePieceType WORKSHOP = piece("cogsdeep_workshop", WorkshopPiece::new);
    /** The clocktower room. */
    public static final StructurePieceType CLOCKTOWER = piece("cogsdeep_clocktower", ClocktowerPiece::new);
    /** The gem-polishing room. */
    public static final StructurePieceType GEM_ROOM = piece("cogsdeep_gem_room", GemRoomPiece::new);
    /** The lush grotto beyond the east workshop. */
    public static final StructurePieceType GROTTO = piece("cogsdeep_grotto", GrottoPiece::new);

    private CogsdeepStructures() {
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
