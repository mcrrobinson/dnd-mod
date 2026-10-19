package mattonfire.dnd.world.gen.temple;

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
 * Registers the Dragon Temple structure type and its pieces. Where temples generate is data:
 * {@code data/dndclasses/worldgen/structure/dragon_temple.json} and the matching structure set; the rule that
 * they only generate near a lair is in {@link DragonTempleStructure}.
 */
public final class DragonTempleStructures {
    public static final Identifier ID = id("dragon_temple");
    public static final RegistryKey<Structure> KEY = RegistryKey.of(RegistryKeys.STRUCTURE, ID);
    public static final Identifier SET = id("dragon_temples");

    public static final StructureType<DragonTempleStructure> DRAGON_TEMPLE = Registry.register(
            Registries.STRUCTURE_TYPE, ID, () -> DragonTempleStructure.CODEC);

    /** The levelled terrace everything stands on. */
    public static final StructurePieceType TERRACE = piece("dragon_temple_terrace", TerracePiece::new);
    public static final StructurePieceType STAIR = piece("dragon_temple_stair", GrandStairPiece::new);
    public static final StructurePieceType COURTYARD = piece("dragon_temple_courtyard", CourtyardPiece::new);
    /** One of the three dragon statues in the courtyard (each its own piece, so its box is the statue). */
    public static final StructurePieceType STATUE = piece("dragon_temple_statue", StatuePiece::new);
    /** The sanctum with the three breath altars: the temple's hearth. */
    public static final StructurePieceType SANCTUM = piece("dragon_temple_sanctum", SanctumPiece::new);
    /** The monks' cells and the library. */
    public static final StructurePieceType CLOISTER = piece("dragon_temple_cloister", CloisterPiece::new);
    public static final StructurePieceType YARD = piece("dragon_temple_yard", TrainingYardPiece::new);
    /** The ground round the terrace, reshaped to blend it into the slope (built first; only ground). */
    public static final StructurePieceType SLOPE = piece("dragon_temple_slope", SlopePiece::new);

    private DragonTempleStructures() {
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
