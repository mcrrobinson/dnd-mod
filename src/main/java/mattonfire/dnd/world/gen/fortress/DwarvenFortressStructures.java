package mattonfire.dnd.world.gen.fortress;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the dwarven fortress structure type and its pieces. Where fortresses generate is data:
 * {@code data/dndclasses/worldgen/structure/dwarven_fortress.json} and the matching structure set.
 */
public final class DwarvenFortressStructures {
    public static final StructureType<DwarvenFortressStructure> DWARVEN_FORTRESS = Registry.register(
            Registries.STRUCTURE_TYPE, id("dwarven_fortress"), () -> DwarvenFortressStructure.CODEC);

    public static final StructurePieceType GATE = piece("dwarven_gate", GatePiece::new);
    public static final StructurePieceType HALL = piece("dwarven_great_hall", HallPiece::new);
    public static final StructurePieceType THRONE_ROOM = piece("dwarven_throne_room", ThroneRoomPiece::new);
    public static final StructurePieceType TREASURY = piece("dwarven_treasury", TreasuryPiece::new);
    public static final StructurePieceType CORRIDOR = piece("dwarven_corridor", CorridorPiece::new);
    public static final StructurePieceType FORGE = piece("dwarven_forge", ForgePiece::new);
    public static final StructurePieceType BARRACKS = piece("dwarven_barracks", BarracksPiece::new);
    public static final StructurePieceType BREWHALL = piece("dwarven_brewhall", BrewhallPiece::new);
    public static final StructurePieceType MINE = piece("dwarven_mine", MinePiece::new);
    public static final StructurePieceType CLADDING = piece("dwarven_cladding", CladdingPiece::new);

    private DwarvenFortressStructures() {
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
