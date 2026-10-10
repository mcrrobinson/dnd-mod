package mattonfire.dnd.world.gen.dungeon;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the dungeon structure type and its pieces. One structure type serves every theme;
 * each theme is its own structure JSON ({@code data/dndclasses/worldgen/structure/crypt.json}, ...)
 * with its structure set, and all of them are in the {@code #dndclasses:dungeons} tag.
 */
public final class DungeonStructures {
    public static final StructureType<DungeonStructure> DUNGEON = Registry.register(
            Registries.STRUCTURE_TYPE, id("dungeon"), () -> DungeonStructure.CODEC);

    public static final StructurePieceType ENTRANCE = piece("dungeon_entrance", EntrancePiece::new);
    public static final StructurePieceType STAIR = piece("dungeon_stair", StairPiece::new);
    public static final StructurePieceType CORRIDOR = piece("dungeon_corridor", DungeonCorridorPiece::new);
    public static final StructurePieceType ANTECHAMBER = piece("dungeon_antechamber", AntechamberPiece::new);
    public static final StructurePieceType ENCOUNTER_ROOM = piece("dungeon_encounter_room", EncounterRoomPiece::new);
    public static final StructurePieceType TRAP_CORRIDOR = piece("dungeon_trap_corridor", TrapCorridorPiece::new);
    public static final StructurePieceType GATE_ROOM = piece("dungeon_gate_room", GateRoomPiece::new);
    public static final StructurePieceType PUZZLE_ROOM = piece("dungeon_puzzle_room", PuzzleRoomPiece::new);
    public static final StructurePieceType CHAMPION_ROOM = piece("dungeon_champion_room", ChampionRoomPiece::new);
    public static final StructurePieceType BOSS_ROOM = piece("dungeon_boss_room", BossRoomPiece::new);
    public static final StructurePieceType VAULT = piece("dungeon_vault", VaultPiece::new);
    public static final StructurePieceType SECRET_ROOM = piece("dungeon_secret_room", SecretRoomPiece::new);

    private DungeonStructures() {
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
