package mattonfire.dnd.world.underdark.lair;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the Underdark's monster lairs (design 9, section 10, T11; docs/structures/underdark-lairs.md): the
 * Basilisk Den, the Troll Bridge and the Illithid Colony. Where they generate is data
 * ({@code worldgen/structure/<lair>.json}, the structure sets and {@code #has_structure/<lair>}); the site checks are
 * the {@link UnderdarkLairStructure} subclasses.
 */
public final class UnderdarkLairs {
    /** One lair: its structure, structure set and locate tag. */
    public enum Lair {
        BASILISK_DEN("basilisk_den", "basilisk_dens"),
        TROLL_BRIDGE("troll_bridge", "troll_bridges"),
        ILLITHID_COLONY("illithid_colony", "illithid_colonies");

        public final Identifier id;
        public final RegistryKey<Structure> key;
        /** The structure set. */
        public final Identifier set;
        /** {@code #dndclasses:<plural>}, for /locate-style lookups. */
        public final TagKey<Structure> tag;

        Lair(String name, String plural) {
            this.id = id(name);
            this.key = RegistryKey.of(RegistryKeys.STRUCTURE, this.id);
            this.set = id(plural);
            this.tag = TagKey.of(RegistryKeys.STRUCTURE, id(plural));
        }

        /** By its command name: {@code basilisk}, {@code troll} or {@code illithid} (or the structure's path). */
        public static Lair byName(String name) {
            return switch (name) {
                case "basilisk", "basilisk_den" -> BASILISK_DEN;
                case "troll", "troll_bridge" -> TROLL_BRIDGE;
                case "illithid", "mind_flayer", "illithid_colony" -> ILLITHID_COLONY;
                default -> null;
            };
        }
    }

    public static final StructureType<BasiliskDenStructure> BASILISK_DEN = Registry.register(
            Registries.STRUCTURE_TYPE, Lair.BASILISK_DEN.id, () -> BasiliskDenStructure.CODEC);
    public static final StructureType<TrollBridgeStructure> TROLL_BRIDGE = Registry.register(
            Registries.STRUCTURE_TYPE, Lair.TROLL_BRIDGE.id, () -> TrollBridgeStructure.CODEC);
    public static final StructureType<IllithidColonyStructure> ILLITHID_COLONY = Registry.register(
            Registries.STRUCTURE_TYPE, Lair.ILLITHID_COLONY.id, () -> IllithidColonyStructure.CODEC);

    public static final StructurePieceType DEN_PIECE = piece("basilisk_den", BasiliskDenPiece::new);
    public static final StructurePieceType BRIDGE_PIECE = piece("troll_bridge", TrollBridgePiece::new);
    public static final StructurePieceType COLONY_PIECE = piece("illithid_colony", IllithidColonyPiece::new);

    /** The Basilisk Den's chest: the victims' gear. */
    public static final Identifier DEN_LOOT = PieceBlocks.loot("basilisk_den");
    /** The Troll Bridge's toll chest. */
    public static final Identifier TOLL_LOOT = PieceBlocks.loot("troll_toll");
    /** The Illithid Colony's chest. */
    public static final Identifier COLONY_LOOT = PieceBlocks.loot("illithid_colony");

    private UnderdarkLairs() {
    }

    static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    private static StructurePieceType piece(String name, StructurePieceType.Simple loader) {
        return Registry.register(Registries.STRUCTURE_PIECE, id(name), loader);
    }

    /** Loads the class, registering everything above. */
    public static void register() {
    }
}
