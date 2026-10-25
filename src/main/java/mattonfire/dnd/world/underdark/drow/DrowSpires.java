package mattonfire.dnd.world.underdark.drow;

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
 * Registers the Drow Spire (design 9, 8.3; docs/structures/drow-spire.md): the drow's hanging city in the Faerzress
 * Hollows and the Matron Mother's seat. Where it generates is data: {@code worldgen/structure/drow_spire.json} and the
 * {@code drow_spires} structure set (rare: spacing 48, separation 20); the site check is {@link DrowSpireStructure},
 * the layout {@link DrowSpirePlanner}. Its minor towers and web bridges are the outposts' pieces
 * ({@link DrowTowerPiece}, {@link DrowBridgePiece}).
 */
public final class DrowSpires {
    public static final Identifier ID = DrowOutposts.id("drow_spire");
    public static final RegistryKey<Structure> KEY = RegistryKey.of(RegistryKeys.STRUCTURE, ID);
    /** The structure set. */
    public static final Identifier SET = DrowOutposts.id("drow_spires");
    /** {@code #dndclasses:drow_spires}, for /locate-style lookups. */
    public static final TagKey<Structure> TAG = TagKey.of(RegistryKeys.STRUCTURE, DrowOutposts.id("drow_spires"));

    public static final StructureType<DrowSpireStructure> DROW_SPIRE = Registry.register(
            Registries.STRUCTURE_TYPE, ID, () -> DrowSpireStructure.CODEC);

    /** The carved dome, its levelled floor, the path to the door and the spider pit. */
    public static final StructurePieceType CAVERN = piece("drow_spire_cavern", DrowSpireCavernPiece::new);
    /** The Spire itself, from the guard hall to the treasure loft. */
    public static final StructurePieceType SPIRE = piece("drow_spire", DrowSpirePiece::new);

    /** The priestesses' cells' chest. */
    public static final Identifier CELLS_LOOT = PieceBlocks.loot("drow_spire_cells");
    /** The treasure loft's chest, behind the Arcane Seal. */
    public static final Identifier LOFT_LOOT = PieceBlocks.loot("drow_spire_loft");

    private DrowSpires() {
    }

    private static StructurePieceType piece(String name, StructurePieceType.Simple loader) {
        return Registry.register(Registries.STRUCTURE_PIECE, DrowOutposts.id(name), loader);
    }

    /** Loads the class, registering everything above. */
    public static void register() {
    }
}
