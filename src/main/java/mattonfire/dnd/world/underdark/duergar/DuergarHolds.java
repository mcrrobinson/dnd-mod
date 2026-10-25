package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.DuergarEntity;
import mattonfire.dnd.world.gen.PieceBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Registers the Duergar Holds (design 9, 8.2; docs/structures/duergar-holds.md): the Grey Clans' walled forge-towns
 * on a lava lake in the Underdark's Grey Reaches. Where they generate is data: {@code worldgen/structure/duergar_hold.json}
 * and the {@code duergar_holds} structure set; the site check is {@link DuergarHoldStructure}, the layout
 * {@link DuergarHoldPlanner}.
 */
public final class DuergarHolds {
    public static final Identifier ID = id("duergar_hold");
    public static final RegistryKey<Structure> KEY = RegistryKey.of(RegistryKeys.STRUCTURE, ID);
    /** The structure set. */
    public static final Identifier SET = id("duergar_holds");
    /** {@code #dndclasses:duergar_holds}, for /locate-style lookups. */
    public static final TagKey<Structure> TAG = TagKey.of(RegistryKeys.STRUCTURE, id("duergar_holds"));

    public static final StructureType<DuergarHoldStructure> DUERGAR_HOLD = Registry.register(
            Registries.STRUCTURE_TYPE, ID, () -> DuergarHoldStructure.CODEC);

    /** The levelled cavern floor, the lava lake, the yard's paths and braziers, the yard guards. */
    public static final StructurePieceType GROUNDS = piece("duergar_hold_grounds", DuergarGroundsPiece::new);
    /** The curtain wall and the gatehouse with its portcullis. */
    public static final StructurePieceType WALL = piece("duergar_hold_wall", DuergarWallPiece::new);
    /** The Forge Hall. */
    public static final StructurePieceType FORGE = piece("duergar_hold_forge", DuergarForgePiece::new);
    /** The Trade Hall, the factor's home. */
    public static final StructurePieceType TRADE = piece("duergar_hold_trade", DuergarTradePiece::new);
    /** The mine head, its stair and the gallery with the slave pen. */
    public static final StructurePieceType MINE = piece("duergar_hold_mine", DuergarMinePiece::new);
    /** A barracks. */
    public static final StructurePieceType BARRACKS = piece("duergar_hold_barracks", DuergarBarracksPiece::new);
    /** The Taskmaster's tower by the lava. */
    public static final StructurePieceType TOWER = piece("duergar_hold_tower", DuergarTowerPiece::new);

    public static final Identifier FORGE_LOOT = PieceBlocks.loot("duergar_forge");
    public static final Identifier MINE_LOOT = PieceBlocks.loot("duergar_mine");
    public static final Identifier STRONGBOX_LOOT = PieceBlocks.loot("duergar_strongbox");
    public static final Identifier BARRACKS_LOOT = PieceBlocks.loot("duergar_barracks");
    public static final Identifier TRADE_LOOT = PieceBlocks.loot("duergar_trade");

    private DuergarHolds() {
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

    // ---- Blocks the pieces share ----

    static BlockState bricks() {
        return ModBlockFamilies.BASALT_FORGE_BRICKS.base().getDefaultState();
    }

    static BlockState greysteel() {
        return ModBlockFamilies.GREYSTEEL_PLATE.base().getDefaultState();
    }

    static BlockState brazier() {
        return UnderdarkBlocks.DUERGAR_BRAZIER.getDefaultState();
    }

    /**
     * A duergar of {@code type} living at local a/y/c: persistent, its home here and kept within {@code range}
     * blocks of it ({@code SettlementNpcEntity.setHome}).
     */
    static <E extends DuergarEntity> void duergar(UnderdarkPiece.Builder b, int a, int y, int c, EntityType<E> type,
                                                  int range) {
        BlockPos home = b.pos(a, y, c);
        b.npc(a, y, c, type, mob -> {
            mob.setPersistent();
            mob.setHome(home, range);
        });
    }
}
