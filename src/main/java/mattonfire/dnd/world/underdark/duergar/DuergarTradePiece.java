package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.entity.DuergarEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Duergar Hold's Trade Hall, the factor's home, its door towards local +b: an 11 x 9 hall with chamfered corners,
 * walls 6 high on a battered plinth under a stepped roof. Most halls raise a <b>counting house</b> over the back
 * half, walls 10 high with slit windows, its own roof stepping up over the hall's. A <b>portico</b> on two basalt
 * posts shelters the door, and an outside <b>chimney stack</b> on one side smokes from the hearth.
 * <p>
 * Inside, a Greysteel counter runs across the room with the factor behind it; barrels of the factor's stock
 * ({@code chests/duergar_trade}: Darklake Pearls, gold nuggets) stand along the back wall, and the <b>hearth</b>, a
 * Duergar Brazier in a brick fireplace, warms one side.
 */
public class DuergarTradePiece extends DuergarPiece {
    public static final int HALF_A = 5;
    public static final int HALF_B = 4;
    /** The hall's walls' top course; the counting house's. */
    public static final int ROOF = 5;
    public static final int UPPER = 9;
    /** Blocks from the centre to just outside the door. */
    public static final int DOOR = 8;
    /** The counter's b. */
    static final int COUNTER = -1;

    public DuergarTradePiece(BlockPos center, Direction front, long seed, boolean mirror, int style) {
        super(DuergarHolds.TRADE, center, front, seed, mirror, style, -8, -3, -6, 7, UPPER + 4, DOOR);
    }

    public DuergarTradePiece(NbtCompound nbt) {
        super(DuergarHolds.TRADE, nbt);
    }

    /** Where the factor lives, behind the counter. */
    public BlockPos factorHome() {
        return this.at(0, 0, COUNTER - 1);
    }

    /** Where you stand in front of the counter. */
    public BlockPos atCounter() {
        return this.at(0, 0, COUNTER + 1);
    }

    /** The hearth brazier. */
    public BlockPos hearth() {
        return this.at(-HALF_A + 1, 0, 1);
    }

    @Override
    protected void build(Builder b) {
        DuergarMasonry.Style style = this.style();
        boolean upper = b.random.nextInt(5) < 3;
        int stack = 8 + b.random.nextInt(3);

        DuergarMasonry.Mass mass = new DuergarMasonry.Mass(-HALF_A, HALF_A, -HALF_B, HALF_B)
                .box(-HALF_A, HALF_A, -HALF_B, HALF_B, 2, ROOF);
        if (upper) {
            mass.box(-3, 3, -HALF_B, COUNTER, 0, UPPER);
        }
        DuergarMasonry.build(b, mass, style, 3, (a, c) -> c == HALF_B + 1 && Math.abs(a) <= 3);
        BlockState bricks = DuergarHolds.bricks();

        // The door, under an arch, and the portico in front: two basalt posts under a blackstone roof.
        for (int a = -1; a <= 1; a++) {
            for (int y = 0; y <= 2; y++) {
                b.clear(a, y, HALF_B);
            }
            b.set(a, -1, HALF_B, Blocks.POLISHED_DEEPSLATE.getDefaultState());
            b.set(a, 3, HALF_B, DuergarMasonry.greysteel());
        }
        b.set(-1, 2, HALF_B, DuergarMasonry.baseStairs(Direction.WEST, true));
        b.set(1, 2, HALF_B, DuergarMasonry.baseStairs(Direction.EAST, true));
        for (int a = -3; a <= 3; a++) {
            for (int c = HALF_B + 1; c <= HALF_B + 2; c++) {
                b.set(a, -1, c, Math.abs(a) <= 2 ? Blocks.POLISHED_DEEPSLATE.getDefaultState() : DuergarMasonry.base());
                for (int y = 0; y <= 3; y++) {
                    b.set(a, y, c, Math.abs(a) == 3 && c == HALF_B + 2 ? DuergarMasonry.pillar() : Blocks.AIR.getDefaultState());
                }
                b.set(a, 4, c, Math.abs(a) == 3 ? DuergarMasonry.base() : DuergarMasonry.baseSlab(true));
            }
            // Its roof slopes on up into the hall's.
            b.set(a, 3, HALF_B + 3, DuergarMasonry.baseStairs(Direction.NORTH, true));
            b.set(a, 4, HALF_B + 3, style.roofStairs(Direction.NORTH, false));
            b.set(a, 5, HALF_B + 2, style.roofStairs(Direction.NORTH, false));
            b.set(a, 5, HALF_B + 1, style.roofBlock().getDefaultState());
            b.set(a, 6, HALF_B + 1, style.roofStairs(Direction.NORTH, false));
        }
        b.hangingLantern(0, 3, HALF_B + 1, 1, Blocks.SOUL_LANTERN);

        // Windows: shuttered on the sides, plain either side of the door, slits in the counting house.
        DuergarMasonry.window(b, -3, 1, HALF_B, Direction.SOUTH, 2, false);
        DuergarMasonry.window(b, 3, 1, HALF_B, Direction.SOUTH, 2, false);
        DuergarMasonry.window(b, -HALF_A, 1, -2, Direction.WEST, 2, true);
        DuergarMasonry.window(b, HALF_A, 1, -2, Direction.EAST, 2, true);
        DuergarMasonry.window(b, HALF_A, 1, 1, Direction.EAST, 2, true);
        if (upper) {
            for (int a : new int[] {-1, 1}) {
                b.set(a, 7, -HALF_B, Blocks.IRON_BARS.getDefaultState());
                b.set(a, 8, -HALF_B, Blocks.IRON_BARS.getDefaultState());
            }
            for (int a : new int[] {-3, 3}) {
                b.set(a, 7, -2, Blocks.IRON_BARS.getDefaultState());
                b.set(a, 8, -2, Blocks.IRON_BARS.getDefaultState());
            }
            b.set(0, 7, COUNTER, Blocks.IRON_BARS.getDefaultState());
            b.set(0, 8, COUNTER, Blocks.IRON_BARS.getDefaultState());
            b.hangingLantern(0, UPPER, -2, 3, Blocks.SOUL_LANTERN);
        }
        // Buttresses on the back.
        DuergarMasonry.buttress(b, -2, -HALF_B - 1, Direction.NORTH, upper ? 6 : 4);
        DuergarMasonry.buttress(b, 2, -HALF_B - 1, Direction.NORTH, upper ? 6 : 4);

        // The counter: Greysteel slabs on top, a brick end, a gap at the other end to get behind it.
        for (int a = -3; a <= 3; a++) {
            b.set(a, 0, COUNTER, DuergarMasonry.greysteelSlab(true));
        }
        b.set(-4, 0, COUNTER, bricks);
        // Barrels of stock along the back wall.
        for (int a : new int[] {-3, -2, 2, 3}) {
            b.barrel(a, 0, -HALF_B + 1, Direction.UP, DuergarHolds.TRADE_LOOT);
        }
        b.set(0, 1, -HALF_B + 1, Blocks.SOUL_LANTERN.getDefaultState());
        b.set(0, 0, -HALF_B + 1, DuergarMasonry.pillar());
        // The hearth: a brazier in a brick fireplace on the side wall, a basalt chimney breast over it, and the stack
        // outside the wall.
        b.set(-HALF_A + 1, -1, 1, bricks);
        b.set(-HALF_A + 1, 0, 1, DuergarHolds.brazier());
        b.set(-HALF_A + 1, 0, 0, DuergarMasonry.brickWall());
        b.set(-HALF_A + 1, 0, 2, DuergarMasonry.brickWall());
        b.set(-HALF_A + 1, 1, 0, DuergarMasonry.baseStairs(Direction.NORTH, true));
        b.set(-HALF_A + 1, 1, 2, DuergarMasonry.baseStairs(Direction.SOUTH, true));
        for (int y = 2; y <= ROOF - 1; y++) {
            b.set(-HALF_A + 1, y, 1, DuergarMasonry.pillar());
        }
        DuergarMasonry.chimney(b, -HALF_A - 1, -HALF_A - 1, 0, 2, -1, stack);
        b.hangingLantern(2, ROOF, 2, 1, Blocks.SOUL_LANTERN);
        b.hangingLantern(-2, ROOF, 2, 1, Blocks.SOUL_LANTERN);
        // Braziers either side of the portico.
        b.set(-4, 0, HALF_B + 3, DuergarHolds.brazier());
        b.set(4, 0, HALF_B + 3, DuergarHolds.brazier());
        DuergarHolds.duergar(b, 0, 0, COUNTER - 1, DuergarEntities.DUERGAR_FACTOR, 3);
    }
}
