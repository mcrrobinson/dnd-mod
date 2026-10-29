package mattonfire.dnd.world.gen.ancient;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.dragon.ChromaticColor;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the Ancient dragons' lairs (design 10, section 6; docs/structures/ancient-lairs.md). One structure type,
 * {@code dndclasses:ancient_lair}, whose {@code color} picks the dragon; the Red's lair is
 * {@code worldgen/structure/ancient_red_lair.json} in the {@code ancient_lairs} structure set (spacing 32, separation
 * 12: one try per 512x512 blocks, kept only where the land allows it, which is a few in a hundred).
 */
public final class AncientLairStructures {
    public static final StructureType<AncientLairStructure> ANCIENT_LAIR = Registry.register(
            Registries.STRUCTURE_TYPE, id("ancient_lair"), () -> AncientLairStructure.CODEC);

    public static final StructurePieceType CALDERA = Registry.register(Registries.STRUCTURE_PIECE, id("ancient_caldera"),
            (StructurePieceType.Simple) CalderaPiece::new);
    public static final StructurePieceType HOARD_HALL = Registry.register(Registries.STRUCTURE_PIECE,
            id("ancient_hoard_hall"), (StructurePieceType.Simple) HoardHallPiece::new);

    /** The structure set. */
    public static final Identifier SET = id("ancient_lairs");
    /** {@code #dndclasses:ancient_dragon_lairs}: every colour's lair (the scale mail's dragon sense looks for these). */
    public static final TagKey<Structure> TAG = TagKey.of(RegistryKeys.STRUCTURE, id("ancient_dragon_lairs"));

    /** The kobolds' workshop chest. Shared with the kobold warrens, whose version of the table wins. */
    public static final Identifier WORKSHOP_LOOT = PieceBlocks.loot("kobold_workshop");

    private AncientLairStructures() {
    }

    /** {@code chests/ancient_<color>_hoard}: the four hoard chests. */
    public static Identifier hoardLoot(ChromaticColor color) {
        return PieceBlocks.loot("ancient_" + color.id() + "_hoard");
    }

    /** {@code chests/ancient_<color>_hoard_coffer}: the Hoard Coffer. */
    public static Identifier cofferLoot(ChromaticColor color) {
        return PieceBlocks.loot("ancient_" + color.id() + "_hoard_coffer");
    }

    static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    /** Loads the class, registering everything above. */
    public static void register() {
    }

    /** The lairs' server hooks: the slain state, the hoard law, the tremor dust and the guard posts. */
    public static void registerHooks() {
        AncientLairs.register();
        AncientLairGuards.register();
    }
}
