package mattonfire.dnd.world.gen.sanctum;

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
 * Registers the Brimstone Sanctum structure type and its pieces. Where sanctums generate is data:
 * {@code data/dndclasses/worldgen/structure/brimstone_sanctum.json} and the matching structure set; the Nether
 * floor scan is in {@link BrimstoneSanctumStructure}.
 */
public final class BrimstoneSanctumStructures {
    public static final Identifier ID = id("brimstone_sanctum");
    public static final RegistryKey<Structure> KEY = RegistryKey.of(RegistryKeys.STRUCTURE, ID);
    public static final Identifier SET = id("brimstone_sanctums");

    public static final StructureType<BrimstoneSanctumStructure> BRIMSTONE_SANCTUM = Registry.register(
            Registries.STRUCTURE_TYPE, ID, () -> BrimstoneSanctumStructure.CODEC);

    /** The platform on its pillars, the wall, the gate and the Exiles. */
    public static final StructurePieceType PLATFORM = piece("brimstone_sanctum_platform", PlatformPiece::new);
    /** The Pact Hall: the sanctum's hearth, with the altar and the Pactbroker. */
    public static final StructurePieceType PACT_HALL = piece("brimstone_sanctum_pact_hall", PactHallPiece::new);
    /** One stall of the Brass Market. */
    public static final StructurePieceType MARKET = piece("brimstone_sanctum_market", MarketPiece::new);
    /** The Alchemist's den. */
    public static final StructurePieceType DEN = piece("brimstone_sanctum_den", DenPiece::new);
    /** The Refuge, the dormitory with the respawn anchors. */
    public static final StructurePieceType REFUGE = piece("brimstone_sanctum_refuge", RefugePiece::new);

    private BrimstoneSanctumStructures() {
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
