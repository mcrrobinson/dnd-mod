package mattonfire.dnd.world.underdark.rift;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the Underdark Rift structures (design 9, 2.6; docs/structures/underdark-rifts.md): the Overworld
 * {@code dndclasses:underdark_rift} (a chasm deep in the deepslate with a Rift Veil pool, under a sinkhole you can
 * climb) and the Underdark {@code dndclasses:rift_landing} (a lit dome with a Rift Anchor). Where they generate is
 * data: their {@code worldgen/structure} and {@code structure_set} files. The two sets share spacing, separation and
 * salt, so every rift has a landing straight below it in the same chunk.
 */
public final class UnderdarkRiftStructures {
    public static final StructureType<UnderdarkRiftStructure> UNDERDARK_RIFT = Registry.register(
            Registries.STRUCTURE_TYPE, id("underdark_rift"), () -> UnderdarkRiftStructure.CODEC);
    public static final StructureType<RiftLandingStructure> RIFT_LANDING = Registry.register(
            Registries.STRUCTURE_TYPE, id("rift_landing"), () -> RiftLandingStructure.CODEC);

    public static final StructurePieceType CHASM = Registry.register(Registries.STRUCTURE_PIECE, id("rift_chasm"),
            (StructurePieceType.Simple) RiftChasmPiece::new);
    public static final StructurePieceType SINKHOLE = Registry.register(Registries.STRUCTURE_PIECE, id("rift_sinkhole"),
            (StructurePieceType.Simple) RiftSinkholePiece::new);
    public static final StructurePieceType LANDING = Registry.register(Registries.STRUCTURE_PIECE, id("rift_landing"),
            (StructurePieceType.Simple) RiftLandingPiece::new);

    /** The structures (as configured in data). */
    public static final RegistryKey<Structure> RIFT_KEY = RegistryKey.of(RegistryKeys.STRUCTURE, id("underdark_rift"));
    public static final RegistryKey<Structure> LANDING_KEY = RegistryKey.of(RegistryKeys.STRUCTURE, id("rift_landing"));
    /** Their structure sets. */
    public static final Identifier RIFT_SET = id("underdark_rifts");
    public static final Identifier LANDING_SET = id("rift_landings");

    private UnderdarkRiftStructures() {
    }

    static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    /** Loads the class, registering everything above. */
    public static void register() {
    }

    /** A cheap, repeatable hash of a position, in [0, 1): the same in every chunk that builds it. */
    static float noise(long seed, int x, int y, int z) {
        long h = MathHelper.hashCode(x, y, z) ^ seed;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        return ((h >>> 40) & 0xFFFFFFL) / (float) 0x1000000;
    }
}
