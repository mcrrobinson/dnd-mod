package mattonfire.dnd.world.underdark.duergar;

import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.LAKE_A1;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.LAKE_A2;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.WALL_BACK;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.WALL_FRONT;
import static mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner.WALL_SIDE;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Duergar Hold's curtain wall and gatehouse, in the hold's own coordinates (gate towards +b).
 * <ul>
 * <li>The <b>wall</b>: two thick, Basalt Forge Bricks 6 high with a Greysteel band on top and Greysteel battlements
 * on the outer row, round the front (b 25-26), the sides (|a| 25-26) and the back (b -16 to -15) except over the
 * lava lake, which closes the back. Stepped buttresses shore it up outside, and a solid <b>tower</b> 10-11 high
 * stands on each corner: a battered foot, slits, a crown of merlons overhanging on corbels, a brazier on top.</li>
 * <li>Two <b>bastions</b> flank the gatehouse, 4 wide and 8 high with their outer corners cut, standing out a block
 * past it, so the gate sits back between them.</li>
 * <li>The <b>gatehouse</b> in the middle of the front, 9 wide, 5 deep and 9 high: a passage 3 wide and 2 high under a
 * Greysteel lintel, closed by a <b>portcullis</b> of three Greysteel trapdoors across it ({@value #GATE_B}). They open
 * on redstone, and start open. Above the passage is the <b>lever room</b>: a lever on the floor feeds redstone dust
 * laid on the lintel, so flipping it shuts or opens the portcullis. A ladder by the yard-side door climbs to the lever
 * room and on through a hatch to the roof, where two arbalesters stand watch behind Greysteel battlements.</li>
 * </ul>
 */
public class DuergarWallPiece extends UnderdarkPiece {
    /** The wall's height: bricks to 4, the Greysteel band at 5, battlements at 6. */
    public static final int HEIGHT = 6;
    /** The gatehouse: |a| up to this... */
    public static final int GATE_HALF = 4;
    /** ...b from this to {@link #GATE_OUT}. */
    public static final int GATE_IN = 23;
    public static final int GATE_OUT = 27;
    /** The portcullis's b. */
    public static final int GATE_B = 25;
    /** The lever room's floor (the lintel's y). */
    public static final int LINTEL = 2;
    /** The gatehouse roof (players stand a block above it). */
    public static final int ROOF = 7;
    /** The lever (local). */
    public static final int LEVER_A = -2;
    /** The ladder (local). */
    public static final int LADDER_A = 3;
    public static final int LADDER_B = 24;

    public DuergarWallPiece(BlockPos center, Direction front, long seed) {
        super(DuergarHolds.WALL, center, front, seed, -WALL_SIDE - 4, -1, WALL_BACK - 3, WALL_SIDE + 4, ROOF + 5, GATE_OUT + 2);
    }

    public DuergarWallPiece(NbtCompound nbt) {
        super(DuergarHolds.WALL, nbt);
    }

    /** Portcullis trapdoor {@code i} (0-2, a = i - 1), in the world. */
    public BlockPos portcullis(int i) {
        return this.at(i - 1, 1, GATE_B);
    }

    /** The lever, in the world. */
    public BlockPos lever() {
        return this.at(LEVER_A, LINTEL + 1, GATE_B);
    }

    /** Where you stand in the lever room, facing the lever. */
    public BlockPos leverRoom() {
        return this.at(LEVER_A, LINTEL + 1, GATE_B - 1);
    }

    /** Just outside the gate, in the world. */
    public BlockPos outside() {
        return this.at(0, 0, GATE_OUT + 3);
    }

    /** Just inside the gate. */
    public BlockPos inside() {
        return this.at(0, 0, GATE_IN - 2);
    }

    /** The gatehouse roof. */
    public BlockPos roof() {
        return this.at(0, ROOF + 1, GATE_B);
    }

    @Override
    protected void build(Builder b) {
        BlockState bricks = DuergarHolds.bricks();
        BlockState greysteel = DuergarHolds.greysteel();
        int side = WALL_SIDE;
        // Front, either side of the gatehouse.
        for (int a = -side - 1; a <= side + 1; a++) {
            if (Math.abs(a) > GATE_HALF) {
                this.wall(b, a, WALL_FRONT, false, bricks, greysteel);
                this.wall(b, a, WALL_FRONT + 1, true, bricks, greysteel);
            }
        }
        // Sides.
        for (int c = WALL_BACK; c <= WALL_FRONT - 1; c++) {
            this.wall(b, -side, c, false, bricks, greysteel);
            this.wall(b, -side - 1, c, true, bricks, greysteel);
            this.wall(b, side, c, false, bricks, greysteel);
            this.wall(b, side + 1, c, true, bricks, greysteel);
        }
        // Back, up to the lake's shore.
        for (int a = -side + 1; a <= side - 1; a++) {
            if (a < LAKE_A1 - 1 || a > LAKE_A2 + 1) {
                this.wall(b, a, WALL_BACK + 1, false, bricks, greysteel);
                this.wall(b, a, WALL_BACK, true, bricks, greysteel);
            }
        }
        // Buttresses along the outside.
        for (int a : new int[] {-17, -11, 11, 17}) {
            DuergarMasonry.buttress(b, a, WALL_FRONT + 2, Direction.SOUTH, HEIGHT - 2);
        }
        for (int c : new int[] {-8, 0, 8, 16}) {
            DuergarMasonry.buttress(b, -side - 2, c, Direction.WEST, HEIGHT - 2);
            DuergarMasonry.buttress(b, side + 2, c, Direction.EAST, HEIGHT - 2);
        }
        for (int a : new int[] {-20, -14, 14, 20}) {
            DuergarMasonry.buttress(b, a, WALL_BACK - 1, Direction.NORTH, HEIGHT - 2);
        }
        // Towers on the four corners, and two bastions flanking the gate, so the gate sits back between them.
        int towerTop = 9 + b.random.nextInt(2);
        for (int s = -1; s <= 1; s += 2) {
            this.tower(b, s * (side + 1) - 2, s * (side + 1) + 2, WALL_FRONT - 1, WALL_FRONT + 3, 1, towerTop);
            this.tower(b, s * (side + 1) - 2, s * (side + 1) + 2, WALL_BACK - 2, WALL_BACK + 2, 1, towerTop);
            this.tower(b, Math.min(s * (GATE_HALF + 1), s * (GATE_HALF + 4)), Math.max(s * (GATE_HALF + 1), s * (GATE_HALF + 4)),
                    GATE_IN + 1, GATE_OUT + 1, 2, ROOF + 1);
        }
        this.gatehouse(b, bricks, greysteel);
    }

    /**
     * Air at hold-local a/y/b, but only inside the air the grounds cleared ({@link DuergarGroundsPiece#clearAt}):
     * higher up, at the rim, it would cut pockets into the cave's rock, where a lava spring decorated later in the
     * next chunk can open and pour down the wall. There the block is left as it is.
     */
    private static void air(Builder b, int a, int y, int c) {
        if (y <= DuergarGroundsPiece.clearAt(Math.max(Math.abs(a), Math.abs(c)))) {
            DuergarMasonry.openAir(b, a, y, c);
        }
    }

    private static void merlon(Builder b, int a, int y, int c, boolean merlon, BlockState state) {
        if (merlon) {
            b.set(a, y, c, state);
        } else {
            air(b, a, y, c);
        }
    }

    /** Whether hold-local a/b is part of the curtain wall's two rows. */
    private static boolean curtain(int a, int c) {
        boolean front = (c == WALL_FRONT || c == WALL_FRONT + 1) && Math.abs(a) <= WALL_SIDE + 1;
        boolean sides = (Math.abs(a) == WALL_SIDE || Math.abs(a) == WALL_SIDE + 1) && c >= WALL_BACK && c <= WALL_FRONT;
        boolean back = (c == WALL_BACK || c == WALL_BACK + 1) && Math.abs(a) <= WALL_SIDE;
        return front || sides || back;
    }

    /**
     * A solid tower over a1..a2 / c1..c2 with its corners chamfered by {@code cut} ({@link DuergarMasonry.Mass#box}),
     * {@code top} high: a blackstone base, forge bricks with basalt at the corners, a Greysteel band two below the top,
     * slit windows; round its foot a battered skirt (left off the curtain wall), round its top a crown overhanging on
     * corbels with Greysteel merlons, and a brazier burning on the roof.
     */
    private void tower(Builder b, int a1, int a2, int c1, int c2, int cut, int top) {
        DuergarMasonry.Mass mass = new DuergarMasonry.Mass(a1, a2, c1, c2).box(a1, a2, c1, c2, cut, top);
        for (int a = a1 - 1; a <= a2 + 1; a++) {
            for (int c = c1 - 1; c <= c2 + 1; c++) {
                if (mass.in(a, c)) {
                    for (int y = -1; y <= top; y++) {
                        BlockState state = y <= 1 ? DuergarMasonry.base() : y == top || y == top - 3 ? DuergarHolds.greysteel()
                                : mass.wall(a, y, c) && mass.corner(a, y, c) ? DuergarMasonry.pillar() : DuergarHolds.bricks();
                        b.set(a, y, c, state);
                    }
                    air(b, a, top + 1, c);
                    air(b, a, top + 2, c);
                    continue;
                }
                Direction in = null;
                for (Direction d : Direction.Type.HORIZONTAL) {
                    if (mass.in(a + d.getOffsetX(), c + d.getOffsetZ())) {
                        in = d;
                    }
                }
                if (in == null) {
                    for (int da = -1; da <= 1 && in == null; da += 2) {
                        for (int dc = -1; dc <= 1; dc += 2) {
                            if (mass.in(a + da, c + dc)) {
                                in = da > 0 ? Direction.EAST : Direction.WEST;
                                break;
                            }
                        }
                    }
                }
                if (in == null) {
                    continue;
                }
                if (!curtain(a, c) && !(Math.abs(a) <= GATE_HALF && c >= GATE_IN && c <= GATE_OUT)) {
                    b.set(a, -1, c, DuergarMasonry.base());
                    b.set(a, 0, c, DuergarMasonry.base());
                    b.set(a, 1, c, DuergarMasonry.baseStairs(in, false));
                }
                b.set(a, top - 1, c, DuergarMasonry.baseStairs(in, true));
                b.set(a, top, c, DuergarHolds.greysteel());
                merlon(b, a, top + 1, c, Math.floorMod(a + c, 2) == 0, DuergarHolds.greysteel());
                air(b, a, top + 2, c);
            }
        }
        // Slits on every face, halfway up.
        int am = (a1 + a2) / 2;
        int cm = (c1 + c2) / 2;
        for (int y = top / 2; y <= top / 2 + 1; y++) {
            b.set(a1, y, cm, Blocks.IRON_BARS.getDefaultState());
            b.set(a2, y, cm, Blocks.IRON_BARS.getDefaultState());
            b.set(am, y, c1, Blocks.IRON_BARS.getDefaultState());
            b.set(am, y, c2, Blocks.IRON_BARS.getDefaultState());
        }
        b.set(am, top + 1, cm, DuergarHolds.brazier());
    }

    /** One column of the wall: bricks to 4, a band at 5 (Greysteel outside), a battlement on the outer row. */
    private void wall(Builder b, int a, int c, boolean outer, BlockState bricks, BlockState greysteel) {
        for (int y = -1; y < HEIGHT - 1; y++) {
            b.set(a, y, c, bricks);
        }
        b.set(a, HEIGHT - 1, c, outer ? greysteel : bricks);
        if (outer && Math.floorMod(a + c, 2) == 0) {
            b.set(a, HEIGHT, c, greysteel);
        } else {
            b.clear(a, HEIGHT, c);
        }
    }

    private void gatehouse(Builder b, BlockState bricks, BlockState greysteel) {
        int h = GATE_HALF;
        for (int a = -h; a <= h; a++) {
            for (int c = GATE_IN; c <= GATE_OUT; c++) {
                boolean edge = Math.abs(a) == h || c == GATE_IN || c == GATE_OUT;
                for (int y = -1; y <= ROOF; y++) {
                    b.set(a, y, c, edge && y <= 1 ? DuergarMasonry.base() : edge && (y == 3 || y == 5) ? greysteel : bricks);
                }
                // Battlements round the roof's edge, air over the rest.
                merlon(b, a, ROOF + 1, c, edge && Math.floorMod(a + c, 2) == 0, greysteel);
                air(b, a, ROOF + 2, c);
                air(b, a, ROOF + 3, c);
            }
        }
        // The passage, its road and lintel.
        for (int c = GATE_IN; c <= GATE_OUT; c++) {
            for (int a = -1; a <= 1; a++) {
                b.set(a, -1, c, Math.floorMod(a + c, 3) == 0 ? Blocks.POLISHED_DEEPSLATE.getDefaultState()
                        : Blocks.SMOOTH_BASALT.getDefaultState());
                b.clear(a, 0, c);
                b.clear(a, 1, c);
                b.set(a, LINTEL, c, c == GATE_B ? greysteel : bricks);
            }
        }
        // Outside, machicolations overhang the gate on corbels, and a hood on basalt posts frames the arch.
        for (int a = -h; a <= h; a++) {
            int c = GATE_OUT + 1;
            b.set(a, ROOF - 2, c, DuergarMasonry.baseStairs(Direction.NORTH, true));
            b.set(a, ROOF - 1, c, bricks);
            b.set(a, ROOF, c, greysteel);
            merlon(b, a, ROOF + 1, c, Math.floorMod(a, 2) == 0, greysteel);
            if (Math.abs(a) <= 2) {
                b.set(a, LINTEL, c, Math.abs(a) == 2 ? DuergarMasonry.base() : DuergarMasonry.baseStairs(Direction.NORTH, true));
                b.set(a, LINTEL + 1, c, DuergarMasonry.baseStairs(Direction.NORTH, false));
            }
            if (Math.abs(a) <= 1) {
                b.set(a, LINTEL, GATE_IN - 1, DuergarMasonry.baseStairs(Direction.SOUTH, true));
            }
        }
        for (int a : new int[] {-2, 2}) {
            b.set(a, 0, GATE_OUT + 1, DuergarMasonry.pillar());
            b.set(a, 1, GATE_OUT + 1, DuergarMasonry.pillar());
        }
        // The portcullis: open (standing against each cell's west side) while the lever is on.
        BlockState trapdoor = ModBlockFamilies.GREYSTEEL_PLATE.trapdoor().getDefaultState()
                .with(TrapdoorBlock.FACING, Direction.EAST).with(TrapdoorBlock.HALF, BlockHalf.BOTTOM)
                .with(TrapdoorBlock.OPEN, true).with(TrapdoorBlock.POWERED, true);
        for (int a = -1; a <= 1; a++) {
            b.set(a, 1, GATE_B, trapdoor);
        }
        // The lever room over the passage.
        for (int a = -h + 1; a <= LADDER_A - 1; a++) {
            for (int c = GATE_IN + 1; c <= GATE_OUT - 1; c++) {
                for (int y = LINTEL + 1; y <= ROOF - 1; y++) {
                    b.clear(a, y, c);
                }
            }
        }
        b.set(LEVER_A, LINTEL + 1, GATE_B, Blocks.LEVER.getDefaultState().with(LeverBlock.FACE, WallMountLocation.FLOOR)
                .with(LeverBlock.FACING, Direction.NORTH).with(LeverBlock.POWERED, true));
        for (int a = -1; a <= 1; a++) {
            b.set(a, LINTEL + 1, GATE_B, Blocks.REDSTONE_WIRE.getDefaultState()
                    .with(RedstoneWireBlock.WIRE_CONNECTION_EAST, WireConnection.SIDE)
                    .with(RedstoneWireBlock.WIRE_CONNECTION_WEST, WireConnection.SIDE)
                    .with(RedstoneWireBlock.POWER, 14 - a));
        }
        b.hangingLantern(0, ROOF - 1, GATE_B - 1, 1, Blocks.SOUL_LANTERN);
        // The ladder from the yard-side door to the roof.
        b.clear(LADDER_A, 0, GATE_IN);
        b.clear(LADDER_A, 1, GATE_IN);
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH);
        for (int y = 0; y <= ROOF; y++) {
            b.set(LADDER_A, y, LADDER_B, ladder);
        }
        // Braziers flanking the gate outside and in.
        for (int a : new int[] {-3, 3}) {
            b.set(a, 0, GATE_OUT + 1, DuergarHolds.brazier());
        }
        b.set(-3, 0, GATE_IN - 1, DuergarHolds.brazier());
        // Two arbalesters on the roof.
        DuergarHolds.duergar(b, -2, ROOF + 1, GATE_B, DuergarEntities.DUERGAR_ARBALESTER, 2);
        DuergarHolds.duergar(b, 1, ROOF + 1, GATE_B + 1, DuergarEntities.DUERGAR_ARBALESTER, 2);
    }
}
