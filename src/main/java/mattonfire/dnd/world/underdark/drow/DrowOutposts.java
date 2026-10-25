package mattonfire.dnd.world.underdark.drow;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.entity.EntityType;
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
 * Registers the Drow Outposts (design 9, 8.1; docs/structures/drow-outposts.md): the drow's common settlement in the
 * Fungal Forest and the Faerzress Hollows. Where they generate is data: {@code worldgen/structure/drow_outpost.json}
 * and the {@code drow_outposts} structure set; the site check is {@link DrowOutpostStructure}, the layout
 * {@link DrowOutpostPlanner}.
 */
public final class DrowOutposts {
    public static final Identifier ID = id("drow_outpost");
    public static final RegistryKey<Structure> KEY = RegistryKey.of(RegistryKeys.STRUCTURE, ID);
    /** The structure set. */
    public static final Identifier SET = id("drow_outposts");
    /** {@code #dndclasses:drow_outposts}, for /locate-style lookups. */
    public static final TagKey<Structure> TAG = TagKey.of(RegistryKeys.STRUCTURE, id("drow_outposts"));

    public static final StructureType<DrowOutpostStructure> DROW_OUTPOST = Registry.register(
            Registries.STRUCTURE_TYPE, ID, () -> DrowOutpostStructure.CODEC);

    /** The levelled cavern floor, its paths and lantern posts, and the spiders. */
    public static final StructurePieceType GROUNDS = piece("drow_outpost_grounds", DrowGroundsPiece::new);
    /** A hollow stalagmite tower. */
    public static final StructurePieceType TOWER = piece("drow_outpost_tower", DrowTowerPiece::new);
    /** A web bridge between two tower tops. */
    public static final StructurePieceType BRIDGE = piece("drow_outpost_bridge", DrowBridgePiece::new);
    /** The spider shrine in the middle. */
    public static final StructurePieceType SHRINE = piece("drow_outpost_shrine", DrowShrinePiece::new);
    /** The captive pen. */
    public static final StructurePieceType PEN = piece("drow_outpost_pen", DrowPenPiece::new);
    /** The barracks dome. */
    public static final StructurePieceType BARRACKS = piece("drow_outpost_barracks", DrowBarracksPiece::new);

    /** The towers' chests. */
    public static final Identifier TOWER_LOOT = PieceBlocks.loot("drow_outpost_tower");
    /** The chest in the shrine's reliquary, behind the Dread Ward. */
    public static final Identifier SHRINE_LOOT = PieceBlocks.loot("drow_shrine");

    /** Beasts the drow keep chained in a pen cell: {@code #dndclasses:underdark/pen_beasts} (a spider by default). */
    public static final TagKey<EntityType<?>> PEN_BEASTS = TagKey.of(RegistryKeys.ENTITY_TYPE, id("underdark/pen_beasts"));

    private DrowOutposts() {
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
