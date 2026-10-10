package mattonfire.dnd.world.gen.stronghold;

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
 * Registers the Orc Stronghold structure type and its pieces. Where strongholds generate is data:
 * {@code data/dndclasses/worldgen/structure/orc_stronghold.json} and the matching structure set; the site
 * rule is in {@link OrcStrongholdStructure}.
 */
public final class OrcStrongholdStructures {
    public static final Identifier ID = id("orc_stronghold");
    public static final RegistryKey<Structure> KEY = RegistryKey.of(RegistryKeys.STRUCTURE, ID);
    public static final Identifier SET = id("orc_strongholds");

    public static final StructureType<OrcStrongholdStructure> ORC_STRONGHOLD = Registry.register(
            Registries.STRUCTURE_TYPE, ID, () -> OrcStrongholdStructure.CODEC);

    /** The levelled clearing, the palisade, totems and bone heaps, and the warriors. */
    public static final StructurePieceType GROUNDS = piece("orc_stronghold_grounds", GroundsPiece::new);
    /** The two watchtowers and the skull-topped gate between them. */
    public static final StructurePieceType GATEHOUSE = piece("orc_stronghold_gatehouse", GatehousePiece::new);
    /** The Great Longhouse: the hold's hearth, the War Chief's seat and the hoard. */
    public static final StructurePieceType LONGHOUSE = piece("orc_stronghold_longhouse", LonghousePiece::new);
    /** The Fighting Pit, where the Trial of Strength is fought. */
    public static final StructurePieceType PIT = piece("orc_stronghold_pit", FightingPitPiece::new);
    public static final StructurePieceType SMITHY = piece("orc_stronghold_smithy", SmithyPiece::new);
    public static final StructurePieceType YURT = piece("orc_stronghold_yurt", YurtPiece::new);

    private OrcStrongholdStructures() {
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
