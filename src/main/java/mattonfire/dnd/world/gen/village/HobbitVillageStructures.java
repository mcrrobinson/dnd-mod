package mattonfire.dnd.world.gen.village;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the hobbit village structure type and its pieces. Where villages generate is data:
 * {@code data/dndclasses/worldgen/structure/hobbit_village.json} and the matching structure set.
 */
public final class HobbitVillageStructures {
    public static final StructureType<HobbitVillageStructure> HOBBIT_VILLAGE = Registry.register(
            Registries.STRUCTURE_TYPE, id("hobbit_village"), () -> HobbitVillageStructure.CODEC);

    public static final StructurePieceType GREEN = piece("hobbit_village_green", VillageGreenPiece::new);
    public static final StructurePieceType SMIAL = piece("hobbit_smial", SmialPiece::new);
    public static final StructurePieceType GARDEN = piece("hobbit_garden", GardenPiece::new);
    public static final StructurePieceType ORCHARD = piece("hobbit_orchard", OrchardPiece::new);
    public static final StructurePieceType MARKET = piece("hobbit_market_stall", MarketStallPiece::new);
    public static final StructurePieceType POND = piece("hobbit_pond", PondPiece::new);
    public static final StructurePieceType INN = piece("hobbit_inn", InnPiece::new);
    public static final StructurePieceType PATH = piece("hobbit_path", PathPiece::new);
    public static final StructurePieceType GROUNDS = piece("hobbit_village_grounds", VillageGroundsPiece::new);

    private HobbitVillageStructures() {
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
