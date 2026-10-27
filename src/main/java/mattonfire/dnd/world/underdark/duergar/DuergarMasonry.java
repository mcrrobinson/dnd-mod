package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.world.gen.PieceBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The Duergar Holds' building kit (docs/structures/duergar-holds.md#architecture): heavy, battered, chamfered
 * masonry under stepped roofs.
 * <ul>
 * <li>A {@link Mass} is a building's footprint as cells with a wall height each, made of boxes with chamfered
 * corners. Taller boxes over lower ones give towers, clerestories and lean-to wings.</li>
 * <li>{@link #build} raises it: a blackstone plinth of stairs round the foot (the battered base), walls of Basalt
 * Forge Bricks on a blackstone base course with polished basalt at every corner and chamfer, a trim course, a
 * Greysteel band at the eaves; then a hipped, stepped roof of the hold's {@link Style} that overhangs the walls by a
 * block on corbels, rising a block per block in from the edge, capped with a ridge or, past {@code layers}, a flat top
 * with a parapet. Each cell's roof slopes only towards cells of its own height, so a lower wing leans against a
 * taller hall.</li>
 * <li>Then the details: {@link #portal recessed doorways}, {@link #window windows} with hoods, sills and Greysteel
 * shutters, {@link #buttress stepped buttresses}, {@link #chimney chimneys} smoking from campfires.</li>
 * </ul>
 * Everything is placed from the footprint and the piece's own random, so every chunk builds the same piece.
 */
final class DuergarMasonry {
    static final int NONE = Integer.MIN_VALUE / 4;

    private DuergarMasonry() {
    }

    // ---- The hold's style ----

    /** The hold's look, shared by its buildings: the roof material (3), the ridge cresting (2) and the trim course (2). */
    record Style(int roof, int crest, int course) {
        static final int COUNT = 12;

        static Style of(int bits) {
            int b = Math.floorMod(bits, COUNT);
            return new Style(b % 3, (b / 3) % 2, (b / 6) % 2);
        }

        static int random(Random random) {
            return random.nextInt(COUNT);
        }

        Block roofBlock() {
            return switch (this.roof) {
                case 0 -> Blocks.DEEPSLATE_TILES;
                case 1 -> Blocks.POLISHED_BLACKSTONE;
                default -> ModBlockFamilies.GREYSTEEL_PLATE.base();
            };
        }

        Block roofStairs() {
            return switch (this.roof) {
                case 0 -> Blocks.DEEPSLATE_TILE_STAIRS;
                case 1 -> Blocks.POLISHED_BLACKSTONE_STAIRS;
                default -> ModBlockFamilies.GREYSTEEL_PLATE.stairs();
            };
        }

        Block roofSlab() {
            return switch (this.roof) {
                case 0 -> Blocks.DEEPSLATE_TILE_SLAB;
                case 1 -> Blocks.POLISHED_BLACKSTONE_SLAB;
                default -> ModBlockFamilies.GREYSTEEL_PLATE.slab();
            };
        }

        BlockState roofStairs(Direction facing, boolean upsideDown) {
            return PieceBlocks.stairs(this.roofStairs(), facing, upsideDown);
        }

        /** What runs along a roof's ridge: a crest of forge-brick or blackstone wall. */
        BlockState ridge() {
            return this.crest == 0 ? PieceBlocks.wall(ModBlockFamilies.BASALT_FORGE_BRICKS) : Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState();
        }

        /** The course of stone round the walls at {@link #TRIM} high. */
        BlockState trim() {
            return this.course == 0 ? Blocks.POLISHED_DEEPSLATE.getDefaultState() : Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
        }
    }

    /** The trim course's height (on walls at least {@value} + 2 high). */
    static final int TRIM = 3;

    // ---- Blocks ----

    static BlockState bricks() {
        return DuergarHolds.bricks();
    }

    static BlockState base() {
        return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    }

    static BlockState baseStairs(Direction facing, boolean upsideDown) {
        return PieceBlocks.stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, facing, upsideDown);
    }

    static BlockState baseSlab(boolean top) {
        return PieceBlocks.slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, top);
    }

    static BlockState pillar() {
        return PieceBlocks.pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y);
    }

    static BlockState greysteel() {
        return DuergarHolds.greysteel();
    }

    static BlockState greysteelStairs(Direction facing, boolean upsideDown) {
        return PieceBlocks.stairs(ModBlockFamilies.GREYSTEEL_PLATE, facing, upsideDown);
    }

    static BlockState greysteelSlab(boolean top) {
        return PieceBlocks.slab(ModBlockFamilies.GREYSTEEL_PLATE, top);
    }

    static BlockState brickWall() {
        return PieceBlocks.wall(ModBlockFamilies.BASALT_FORGE_BRICKS);
    }

    /** A Greysteel shutter standing open against the wall behind it ({@code out} is away from the wall). */
    static BlockState shutter(Direction out) {
        return ModBlockFamilies.GREYSTEEL_PLATE.trapdoor().getDefaultState().with(TrapdoorBlock.FACING, out)
                .with(TrapdoorBlock.HALF, BlockHalf.BOTTOM).with(TrapdoorBlock.OPEN, true);
    }

    static Direction dir(int da, int dc) {
        if (da > 0) {
            return Direction.EAST;
        }
        if (da < 0) {
            return Direction.WEST;
        }
        return dc > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static final int[][] N4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] N8 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    // ---- Footprints ----

    /** A footprint: cells of a box of local a/b, each with the y of its walls' top course ({@link #NONE} if empty). */
    static final class Mass {
        final int a1;
        final int a2;
        final int c1;
        final int c2;
        private final int[] top;
        private int[] depth;

        Mass(int a1, int a2, int c1, int c2) {
            this.a1 = a1;
            this.a2 = a2;
            this.c1 = c1;
            this.c2 = c2;
            this.top = new int[(a2 - a1 + 1) * (c2 - c1 + 1)];
            java.util.Arrays.fill(this.top, NONE);
        }

        private int index(int a, int c) {
            return a < this.a1 || a > this.a2 || c < this.c1 || c > this.c2 ? -1 : (a - this.a1) * (this.c2 - this.c1 + 1) + c - this.c1;
        }

        /**
         * Adds a box from a1/c1 to a2/c2 with walls {@code top} high, its corners chamfered: a cell is left out when
         * its distances from the nearest side in a and in b add up to less than {@code cut} (1 takes the corner
         * block, 2 a diagonal of three). Where boxes overlap the taller wins.
         */
        Mass box(int a1, int a2, int c1, int c2, int cut, int top) {
            for (int a = a1; a <= a2; a++) {
                for (int c = c1; c <= c2; c++) {
                    int da = Math.min(a - a1, a2 - a);
                    int dc = Math.min(c - c1, c2 - c);
                    int i = this.index(a, c);
                    if (da + dc >= cut && i >= 0) {
                        this.top[i] = Math.max(this.top[i], top);
                    }
                }
            }
            this.depth = null;
            return this;
        }

        /** Takes a cell out. */
        Mass cut(int a, int c) {
            int i = this.index(a, c);
            if (i >= 0) {
                this.top[i] = NONE;
            }
            this.depth = null;
            return this;
        }

        int top(int a, int c) {
            int i = this.index(a, c);
            return i < 0 ? NONE : this.top[i];
        }

        boolean in(int a, int c) {
            return this.top(a, c) != NONE;
        }

        /** Whether a/y/b is a wall: inside the footprint, at most its top, with an open or lower cell beside it. */
        boolean wall(int a, int y, int c) {
            int top = this.top(a, c);
            if (top == NONE || y > top) {
                return false;
            }
            for (int[] n : N4) {
                if (this.top(a + n[0], c + n[1]) < y) {
                    return true;
                }
            }
            return false;
        }

        /** Whether a/y/b is a square corner of the walls (not a step of a chamfer): open on two sides at right angles. */
        boolean corner(int a, int y, int c) {
            int dx = this.top(a + 1, c) < y ? 1 : this.top(a - 1, c) < y ? -1 : 0;
            int dz = this.top(a, c + 1) < y ? 1 : this.top(a, c - 1) < y ? -1 : 0;
            if (dx == 0 || dz == 0) {
                return false;
            }
            // A step of a chamfer has wall cells on both diagonals beside it; a true corner has open ground there.
            return this.top(a - dx, c + dz) < y && this.top(a + dx, c - dz) < y;
        }

        /**
         * How far in the cell is from the edge of its own height (8 directions, 1 on the edge): the roof rises a
         * block for each. Taller cells beside it don't count as edge, so a lower wing's roof leans up against them.
         */
        int depth(int a, int c) {
            if (this.depth == null) {
                this.depths();
            }
            int i = this.index(a, c);
            return i < 0 ? 0 : this.depth[i];
        }

        private void depths() {
            int big = 1000;
            int[] d = new int[this.top.length];
            for (int i = 0; i < d.length; i++) {
                d[i] = this.top[i] == NONE ? 0 : big;
            }
            this.depth = d;
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int a = this.a1; a <= this.a2; a++) {
                    for (int c = this.c1; c <= this.c2; c++) {
                        int i = this.index(a, c);
                        int top = this.top[i];
                        if (top == NONE) {
                            continue;
                        }
                        int best = d[i];
                        for (int[] n : N8) {
                            int nt = this.top(a + n[0], c + n[1]);
                            if (nt < top) {
                                best = Math.min(best, 1);
                            } else if (nt == top) {
                                best = Math.min(best, d[this.index(a + n[0], c + n[1])] + 1);
                            }
                        }
                        if (best < d[i]) {
                            d[i] = best;
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    // ---- Raising a building ----

    /** Which ring cells (just outside the footprint) to leave without a plinth: doorways and paths. */
    interface Open {
        boolean open(int a, int c);

        Open NONE = (a, c) -> false;
    }

    /**
     * Raises {@code mass}: foundation and floor, walls and the air inside them, the plinth and the eaves round it, and
     * the roof, at most {@code layers} steps high before it flattens.
     */
    static void build(UnderdarkPiece.Builder b, Mass m, Style s, int layers, Open open) {
        BlockState floorA = Blocks.POLISHED_DEEPSLATE.getDefaultState();
        BlockState floorB = Blocks.DEEPSLATE_TILES.getDefaultState();
        for (int a = m.a1 - 1; a <= m.a2 + 1; a++) {
            for (int c = m.c1 - 1; c <= m.c2 + 1; c++) {
                int top = m.top(a, c);
                if (top != NONE) {
                    b.set(a, -2, c, base());
                    b.set(a, -1, c, m.wall(a, 0, c) ? base() : Math.floorMod(a + c, 2) == 0 ? floorA : floorB);
                    for (int y = 0; y <= top; y++) {
                        if (m.wall(a, y, c)) {
                            b.set(a, y, c, wallBlock(m, s, a, y, c));
                        } else {
                            b.clear(a, y, c);
                        }
                    }
                    roof(b, m, s, layers, a, c, top);
                } else {
                    ring(b, m, s, open, a, c);
                }
            }
        }
    }

    static BlockState wallBlock(Mass m, Style s, int a, int y, int c) {
        int top = m.top(a, c);
        if (y <= 0) {
            return base();
        }
        if (y == top) {
            return greysteel();
        }
        if (m.corner(a, y, c)) {
            return pillar();
        }
        if (y == TRIM && top >= TRIM + 2) {
            return s.trim();
        }
        return bricks();
    }

    /** The roof over one cell: solid up to its slope, a stair on the slope, a ridge on top or a flat top with a parapet. */
    private static void roof(UnderdarkPiece.Builder b, Mass m, Style s, int layers, int a, int c, int top) {
        int d = m.depth(a, c);
        int h = Math.min(d, layers);
        for (int y = top + 1; y < top + h; y++) {
            b.set(a, y, c, s.roofBlock().getDefaultState());
        }
        int y = top + h;
        if (d < layers) {
            Direction up = null;
            for (int[] n : N4) {
                if (m.top(a + n[0], c + n[1]) == top && m.depth(a + n[0], c + n[1]) > d) {
                    up = dir(n[0], n[1]);
                    break;
                }
            }
            if (up == null) {
                // On a hip the slope rises diagonally: a stair turned to one side, shaped into the corner later.
                for (int[] n : N8) {
                    if (n[0] != 0 && n[1] != 0 && m.top(a + n[0], c + n[1]) == top && m.depth(a + n[0], c + n[1]) > d) {
                        up = dir(n[0], 0);
                        break;
                    }
                }
            }
            if (up == null) {
                // Against a taller part: lean on its wall rather than cresting.
                for (int[] n : N4) {
                    if (m.top(a + n[0], c + n[1]) > top) {
                        up = dir(n[0], n[1]);
                        break;
                    }
                }
            }
            if (up != null) {
                b.set(a, y, c, s.roofStairs(up, false));
            } else {
                b.set(a, y, c, s.roofBlock().getDefaultState());
                if (Math.floorMod(a + c, 2) == 0) {
                    b.set(a, y + 1, c, s.ridge());
                }
            }
        } else {
            b.set(a, y, c, s.roofBlock().getDefaultState());
            if (d == layers) {
                b.set(a, y + 1, c, brickWall());
            }
        }
    }

    /** A cell just outside the footprint: the plinth's stair at its foot and the eave's stair and corbel at the top. */
    private static void ring(UnderdarkPiece.Builder b, Mass m, Style s, Open open, int a, int c) {
        int best = NONE;
        Direction toward = null;
        for (int[] n : N4) {
            int t = m.top(a + n[0], c + n[1]);
            if (t > best) {
                best = t;
                toward = dir(n[0], n[1]);
            }
        }
        if (best == NONE) {
            for (int[] n : N8) {
                int t = m.top(a + n[0], c + n[1]);
                if (t > best) {
                    best = t;
                    toward = dir(n[0], 0);
                }
            }
        }
        if (best == NONE) {
            return;
        }
        if (!open.open(a, c)) {
            b.set(a, -1, c, base());
            b.set(a, 0, c, baseStairs(toward, false));
        }
        b.set(a, best - 1, c, s.roofStairs(toward, true));
        b.set(a, best, c, s.roofStairs(toward, false));
    }

    // ---- Details ----

    /**
     * Air at a/y/b, with any liquid beside or above it turned to cobbled deepslate: for clearing above the hold's
     * levelled air (battlements, roofs), where it can cut into the cave's rock and the springs in it.
     */
    static void openAir(UnderdarkPiece.Builder b, int a, int y, int c) {
        b.clear(a, y, c);
        BlockState seal = Blocks.COBBLED_DEEPSLATE.getDefaultState();
        b.seal(a + 1, y, c, seal);
        b.seal(a - 1, y, c, seal);
        b.seal(a, y, c + 1, seal);
        b.seal(a, y, c - 1, seal);
        b.seal(a, y + 1, c, seal);
    }

    /** A merlon of {@code state} where {@code merlon}, else a gap ({@link #openAir}). */
    static void merlon(UnderdarkPiece.Builder b, int a, int y, int c, boolean merlon, BlockState state) {
        if (merlon) {
            b.set(a, y, c, state);
        } else {
            openAir(b, a, y, c);
        }
    }

    /**
     * A recessed doorway in the wall row {@code c}, opening towards +b: {@code half} either side of {@code a0},
     * {@code height} high, its top corners arched with stairs, a Greysteel lintel over it; in front (row c + 1) a frame
     * of basalt posts under a blackstone hood, so the door sits a block deep.
     */
    static void portal(UnderdarkPiece.Builder b, int a0, int half, int height, int c) {
        for (int a = a0 - half; a <= a0 + half; a++) {
            for (int y = 0; y < height; y++) {
                b.clear(a, y, c);
                b.clear(a, y, c + 1);
            }
            b.set(a, -1, c, Blocks.POLISHED_DEEPSLATE.getDefaultState());
            b.set(a, -1, c + 1, Blocks.POLISHED_DEEPSLATE.getDefaultState());
            b.set(a, height, c, greysteel());
        }
        if (half >= 1 && height >= 3) {
            b.set(a0 - half, height - 1, c, baseStairs(Direction.WEST, true));
            b.set(a0 + half, height - 1, c, baseStairs(Direction.EAST, true));
        }
        for (int a = a0 - half - 1; a <= a0 + half + 1; a++) {
            boolean post = Math.abs(a - a0) == half + 1;
            if (post) {
                for (int y = -1; y < height; y++) {
                    b.set(a, y, c + 1, y < 0 ? base() : pillar());
                }
            }
            b.set(a, height, c + 1, post ? base() : baseStairs(Direction.NORTH, true));
            b.set(a, height + 1, c + 1, post ? greysteelSlab(false) : baseStairs(Direction.NORTH, false));
        }
    }

    /**
     * A window {@code height} high of iron bars at a/y/b in a wall, {@code out} towards the outside: a stair hood over
     * it and a slab sill under it outside, and Greysteel shutters standing open either side if {@code shutters}.
     */
    static void window(UnderdarkPiece.Builder b, int a, int y, int c, Direction out, int height, boolean shutters) {
        for (int i = 0; i < height; i++) {
            b.set(a, y + i, c, Blocks.IRON_BARS.getDefaultState());
        }
        int oa = a + out.getOffsetX();
        int oc = c + out.getOffsetZ();
        b.set(oa, y + height, oc, baseStairs(out.getOpposite(), true));
        b.set(oa, y - 1, oc, baseSlab(true));
        if (shutters) {
            Direction side = out.rotateYClockwise();
            for (int s = -1; s <= 1; s += 2) {
                for (int i = 0; i < height; i++) {
                    b.set(oa + side.getOffsetX() * s, y + i, oc + side.getOffsetZ() * s, shutter(out));
                }
            }
        }
    }

    /**
     * A stepped buttress against a wall at a/b (just outside it), {@code out} away from the wall: a column
     * {@code height} high capped with a stair, and in front of it a lower step two high.
     */
    static void buttress(UnderdarkPiece.Builder b, int a, int c, Direction out, int height) {
        Direction in = out.getOpposite();
        for (int y = -1; y < height; y++) {
            b.set(a, y, c, base());
        }
        b.set(a, height, c, baseStairs(in, false));
        int fa = a + out.getOffsetX();
        int fc = c + out.getOffsetZ();
        b.set(fa, -1, fc, base());
        b.set(fa, 0, fc, base());
        b.set(fa, 1, fc, baseStairs(in, false));
    }

    /**
     * A chimney stack from y1 to y2 over a1..a2 / c1..c2: forge bricks with basalt corners and a blackstone band,
     * then a crown of brick wall round lit campfires on magma, smoking.
     */
    static void chimney(UnderdarkPiece.Builder b, int a1, int a2, int c1, int c2, int y1, int y2) {
        boolean single = a1 == a2 && c1 == c2;
        for (int a = a1; a <= a2; a++) {
            for (int c = c1; c <= c2; c++) {
                // A stack one block thin smokes along its middle.
                boolean edge = a1 != a2 && (a == a1 || a == a2) || c1 != c2 && (c == c1 || c == c2);
                boolean corner = (a == a1 || a == a2) && (c == c1 || c == c2);
                for (int y = y1; y <= y2; y++) {
                    BlockState state = y == y2 - 2 ? base() : corner && !single ? pillar() : bricks();
                    if (!edge && y >= y2 - 1) {
                        state = Blocks.MAGMA_BLOCK.getDefaultState();
                    }
                    b.set(a, y, c, state);
                }
                if (single || !edge) {
                    b.set(a, y2 + 1, c, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
                } else {
                    b.set(a, y2 + 1, c, corner ? brickWall() : Blocks.AIR.getDefaultState());
                }
            }
        }
    }
}
