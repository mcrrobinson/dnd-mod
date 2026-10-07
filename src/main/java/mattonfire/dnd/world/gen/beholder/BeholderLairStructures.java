package mattonfire.dnd.world.gen.beholder;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the Beholder lair structure type and its pieces. Where lairs generate is data:
 * {@code data/dndclasses/worldgen/structure/beholder_lair.json} and the matching structure set.
 */
public final class BeholderLairStructures {
    public static final StructureType<BeholderLairStructure> BEHOLDER_LAIR = Registry.register(
            Registries.STRUCTURE_TYPE, id("beholder_lair"), () -> BeholderLairStructure.CODEC);

    public static final StructurePieceType CAVERN = Registry.register(Registries.STRUCTURE_PIECE, id("beholder_cavern"),
            (StructurePieceType.Simple) BeholderCavernPiece::new);
    public static final StructurePieceType SHAFT = Registry.register(Registries.STRUCTURE_PIECE, id("beholder_shaft"),
            (StructurePieceType.Simple) BeholderShaftPiece::new);

    private BeholderLairStructures() {
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    /** Loads the class, registering everything above. */
    public static void register() {
    }

    /** A cheap, repeatable hash of a block position, in [0, 1): the same in every chunk that builds it. */
    static float noise(long seed, int x, int y, int z) {
        long h = net.minecraft.util.math.MathHelper.hashCode(x, y, z) ^ seed;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        return ((h >>> 40) & 0xFFFFFFL) / (float) 0x1000000;
    }
}
