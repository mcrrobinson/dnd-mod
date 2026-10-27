package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Duergar Hold's barracks, its door towards local +b: a 9 x 7 hall, walls 5 high on a battered plinth, its back
 * corners cut, under a hipped roof; a square <b>watch turret</b> rises over one front corner under its own pyramid
 * roof, a ladder climbing inside it. Where a hold has two barracks their turrets face each other across the alley
 * between them, and a Greysteel <b>bridge</b> joins them at the top of the ladders.
 * <p>
 * Inside: four bunks along the back wall (a black bed under a Greysteel shelf with a dark wool mattress on it), a chest
 * of kit ({@code chests/duergar_barracks}) between them, barrels, and two soldiers off duty.
 */
public class DuergarBarracksPiece extends DuergarPiece {
    public static final int HALF_A = 4;
    public static final int HALF_B = 3;
    /** The hall's walls' top course. */
    public static final int ROOF = 4;
    /** Blocks from the centre to just outside the door. */
    public static final int DOOR = HALF_B + 2;
    /** The bridge's deck (players stand a block above it). */
    public static final int DECK = ROOF;

    /** The turret's end of the hall (local a, +1 or -1). */
    private final int turret;
    /** 0: no bridge; 1: a door in the turret onto the bridge; 2: that, and this barracks builds the bridge too. */
    private final int bridge;

    public DuergarBarracksPiece(BlockPos center, Direction front, long seed, boolean mirror, int style, int turret, int bridge) {
        super(DuergarHolds.BARRACKS, center, front, seed, mirror, style, -HALF_A - 3, -2, -HALF_B - 2, HALF_A + 3, 12,
                HALF_B + 2);
        this.turret = turret;
        this.bridge = bridge;
    }

    public DuergarBarracksPiece(NbtCompound nbt) {
        super(DuergarHolds.BARRACKS, nbt);
        this.turret = nbt.getInt("Turret") < 0 ? -1 : 1;
        this.bridge = nbt.getInt("Bridge");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Turret", this.turret);
        nbt.putInt("Bridge", this.bridge);
    }

    /** Just inside the door. */
    public BlockPos inside() {
        return this.at(0, 0, HALF_B - 1);
    }

    @Override
    protected void build(Builder b) {
        DuergarMasonry.Style style = this.style();
        int t = this.turret;
        int turretTop = 8 + b.random.nextInt(2);
        DuergarMasonry.Mass mass = new DuergarMasonry.Mass(-HALF_A, HALF_A, -HALF_B, HALF_B)
                .box(-HALF_A, HALF_A, -HALF_B, HALF_B, 0, ROOF)
                .box(Math.min(t * 2, t * 4), Math.max(t * 2, t * 4), 1, 3, 0, turretTop)
                .cut(-HALF_A, -HALF_B).cut(HALF_A, -HALF_B).cut(-t * HALF_A, HALF_B);
        DuergarMasonry.build(b, mass, style, 4, DuergarMasonry.Open.NONE);

        DuergarMasonry.portal(b, 0, 0, 2, HALF_B);
        DuergarMasonry.window(b, -2, 1, HALF_B, Direction.SOUTH, 2, false);
        DuergarMasonry.window(b, 2, 1, HALF_B, Direction.SOUTH, 2, false);
        b.set(-2, 3, -HALF_B, Blocks.IRON_BARS.getDefaultState());
        b.set(2, 3, -HALF_B, Blocks.IRON_BARS.getDefaultState());
        DuergarMasonry.buttress(b, -2, -HALF_B - 1, Direction.NORTH, 3);
        DuergarMasonry.buttress(b, 2, -HALF_B - 1, Direction.NORTH, 3);
        // The turret: arrow slits, a ladder up the inside.
        for (int y = ROOF + 2; y <= ROOF + 3; y++) {
            b.set(t * 3, y, HALF_B, Blocks.IRON_BARS.getDefaultState());
            b.set(t * 2, y, 2, Blocks.IRON_BARS.getDefaultState());
        }
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH);
        for (int y = 0; y <= ROOF + 2; y++) {
            b.set(t * 3, y, 2, ladder);
        }
        b.set(t * 3, turretTop, 2, PieceBlocks.soulLantern(true));
        if (this.bridge > 0) {
            // A door out of the turret onto the bridge.
            b.clear(t * 4, ROOF + 1, 2);
            b.clear(t * 4, ROOF + 2, 2);
        } else {
            b.set(t * 4, ROOF + 2, 2, Blocks.IRON_BARS.getDefaultState());
        }
        if (this.bridge > 1) {
            // The bridge across the alley to the other barracks' turret: a Greysteel deck, brick-wall railings,
            // corbels under its ends and a lantern hanging beneath.
            for (int i = 1; i <= 2; i++) {
                int a = t * (HALF_A + i);
                b.set(a, DECK, 2, DuergarMasonry.greysteelSlab(true));
                b.set(a, DECK - 1, 1, DuergarMasonry.baseStairs(Direction.SOUTH, true));
                b.set(a, DECK - 1, 3, DuergarMasonry.baseStairs(Direction.NORTH, true));
                b.set(a, DECK, 1, DuergarMasonry.base());
                b.set(a, DECK, 3, DuergarMasonry.base());
                b.set(a, DECK + 1, 1, DuergarMasonry.brickWall());
                b.set(a, DECK + 1, 3, DuergarMasonry.brickWall());
                b.clear(a, DECK + 1, 2);
                b.clear(a, DECK + 2, 2);
            }
            b.hangingLantern(t * (HALF_A + 1), DECK - 1, 2, 1, Blocks.SOUL_LANTERN);
        }

        // Bunks.
        BlockState shelf = DuergarMasonry.greysteelSlab(true);
        for (int a : new int[] {-3, -1, 1, 3}) {
            BlockState bed = Blocks.BLACK_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
            b.set(a, 0, -1, bed.with(BedBlock.PART, BedPart.FOOT));
            b.set(a, 0, -2, bed.with(BedBlock.PART, BedPart.HEAD));
            for (int c = -2; c <= -1; c++) {
                b.set(a, 1, c, shelf);
                b.set(a, 2, c, Blocks.GRAY_CARPET.getDefaultState());
            }
        }
        b.chest(0, 0, -2, Direction.SOUTH, DuergarHolds.BARRACKS_LOOT);
        b.set(-t * 3, 0, 2, PieceBlocks.facing(Blocks.BARREL, Direction.UP));
        b.set(-t * 3, 1, 2, PieceBlocks.facing(Blocks.BARREL, Direction.UP));
        b.set(-t * 2, 0, 2, DuergarHolds.brazier());
        b.hangingLantern(0, ROOF, 0, 1, Blocks.SOUL_LANTERN);
        DuergarHolds.duergar(b, -1, 0, 0, DuergarEntities.DUERGAR_SOLDIER, 8);
        DuergarHolds.duergar(b, 1, 0, 1, DuergarEntities.DUERGAR_SOLDIER, 8);
    }
}
