package mattonfire.dnd.world.gen.lair;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the dragon lair structure type and its piece. Where lairs generate is data:
 * {@code data/dndclasses/worldgen/structure/dragon_lair.json} and the matching structure set.
 */
public final class DragonLairStructures {
    public static final StructureType<DragonLairStructure> DRAGON_LAIR = Registry.register(
            Registries.STRUCTURE_TYPE, id("dragon_lair"), () -> DragonLairStructure.CODEC);

    public static final StructurePieceType LAIR = Registry.register(Registries.STRUCTURE_PIECE, id("dragon_lair"),
            (StructurePieceType.Simple) LairPiece::new);

    private DragonLairStructures() {
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    /** Loads the class, registering everything above. */
    public static void register() {
    }
}
