package mattonfire.dnd.world.gen.camp;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the goblin camp structure type and its piece. Where camps generate is data:
 * {@code data/dndclasses/worldgen/structure/goblin_camp.json} and the matching structure set.
 */
public final class GoblinCampStructures {
    public static final StructureType<GoblinCampStructure> GOBLIN_CAMP = Registry.register(
            Registries.STRUCTURE_TYPE, id("goblin_camp"), () -> GoblinCampStructure.CODEC);

    public static final StructurePieceType CAMP = Registry.register(Registries.STRUCTURE_PIECE, id("goblin_camp"),
            (StructurePieceType.Simple) GoblinCampPiece::new);

    private GoblinCampStructures() {
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    /** Loads the class, registering everything above. */
    public static void register() {
    }
}
