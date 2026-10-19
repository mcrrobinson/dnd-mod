package mattonfire.dnd.world.gen.burrow;

import static mattonfire.dnd.classes.Registry.ModBlockFamilies.*;
import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Registry.CogsdeepBlocks;
import mattonfire.dnd.classes.Blocks.RubbleBlock;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import mattonfire.dnd.classes.Registry.RuinBlocks;
import mattonfire.dnd.world.gen.Desolation;
import mattonfire.dnd.world.gen.DesolationLevel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The observatory on the surface, Cogsdeep's front door: a round tower of gnomish bricks {@link #RADIUS} blocks
 * across the middle, {@link #WALL_HEIGHT} high with glass windows, framed in riveted brass at its foot and top,
 * under a hemispherical dome of cut copper that weathers green over time, with a slit 3 wide open to the north from
 * just above the walls up over the top, topped with a lightning rod. The telescope stands on a spruce gallery
 * against the north wall, {@link #GALLERY} up a ladder: an end rod eyepiece and a tube of lightning rods ending in a
 * Telescope Lens ({@link #TUBE} blocks) on a fence tripod, level with the bottom of the slit, running out through it
 * so its end pokes out of the dome. Inside: a spruce floor, the stairwell in the middle ({@link ShaftPiece}), a
 * cartography desk with a spyglass, a lectern, bookshelves and a chest. Trees and
 * plants round it are cleared; the floor sits on a foundation down to the ground.
 *
 * <p>In a desolate Cogsdeep the dome has caved in ({@link Cave}): the crown is gone, a jagged rim of copper stands
 * where it's still held up, a section or two has slumped in over a broken wall top, the telescope is knocked off true
 * and the copper lies heaped on the floor and the gallery.
 *
 * <p>y = 0 is where you stand on the floor (the ground's first air in the middle).
 */
public class ObservatoryPiece extends CogsdeepPiece {
    public static final double RADIUS = 6.5D;
    static final int WALL_HEIGHT = 5;
    /** The dome's sphere: centre height and radius. */
    static final int DOME_BASE = WALL_HEIGHT;
    static final double DOME = 6.5D;
    /** Trees and plants are cleared out to this radius, this high. */
    static final int CLEAR_RADIUS = 9;
    static final int CLEAR_HEIGHT = 16;
    /** The gallery's floor block (you stand one above it), and its south edge (b; it runs to the north wall). */
    public static final int GALLERY = 4;
    static final int GALLERY_B = -2;
    /** The telescope: the tube's height (the slit's lowest row), its eyepiece at a/b, and how many rods long. */
    static final int SCOPE_Y = DOME_BASE + 1;
    public static final int SCOPE_B = -4;
    static final int TUBE = 4;
    /** Half the slit's width: it's 3 blocks wide. */
    static final int SLIT = 1;

    public ObservatoryPiece(BlockPos center, Direction door, long seed, long burrow) {
        super(CogsdeepStructures.OBSERVATORY, center, door, seed, burrow, -CLEAR_RADIUS, -8, -CLEAR_RADIUS - 4,
                CLEAR_RADIUS, CLEAR_HEIGHT, CLEAR_RADIUS + 4);
    }

    public ObservatoryPiece(NbtCompound nbt) {
        super(CogsdeepStructures.OBSERVATORY, nbt);
    }

    /** The telescope's eyepiece, in the world. */
    public BlockPos eyepiece() {
        return this.at(0, SCOPE_Y, SCOPE_B);
    }

    /** The telescope tube's far end, outside the dome, in the world. */
    public BlockPos scopeEnd() {
        return this.at(0, SCOPE_Y, SCOPE_B - TUBE);
    }

    /** Just outside the door, in the world. */
    public BlockPos outside() {
        return this.at(0, 0, 9);
    }

    /** Whether a/c is a plank of the gallery's floor (at {@link #GALLERY}), the ladder's gap left out. */
    static boolean gallery(int a, int c) {
        return Math.abs(a) <= 2 && c >= -6 && c <= GALLERY_B && r(a, c) <= RADIUS - 1.0D && !(a == -1 && c == SCOPE_B - 1);
    }

    /** Whether a/y/c is a block of the dome's copper shell (the slit left out). */
    static boolean shell(int a, int y, int c) {
        if (y < DOME_BASE || Math.abs(a) <= SLIT && c < 0 && y > DOME_BASE) {
            return false;
        }
        double d = Math.sqrt(a * a + c * c + (y - DOME_BASE) * (y - DOME_BASE));
        return d <= DOME && d > DOME - 1.15D;
    }

    /** Window {@code i} of 8 (a/c): every 45 degrees, but not by the door or the telescope. */
    static int[] window(int i) {
        double angle = i * Math.PI / 4.0D + Math.PI / 8.0D;
        return new int[]{(int) Math.round(Math.sin(angle) * 6.0D), (int) Math.round(Math.cos(angle) * 6.0D)};
    }

    /** Whether a/c is in the tower's wall. */
    static boolean wall(int a, int c) {
        double r = r(a, c);
        return r <= RADIUS && r > RADIUS - 1.0D;
    }

    /**
     * How a desolate Cogsdeep's dome caved in. It's all worked out from the piece's seed ({@link Desolation#roll}),
     * never from blocks, so every chunk the observatory crosses agrees on it:
     * <ul>
     *   <li>the dome is split into {@link #SECTORS} sectors round the middle, and each keeps its shell up to a
     *   rim height (rows above the walls, the level's {@code rimMin} to {@code rimMax}), blended into its
     *   neighbours and ragged by a block either way, so the crown is always gone and the rim is jagged;</li>
     *   <li>some sectors (at least one) have slumped in: no shell at all, the wall top broken down a row or two
     *   (never by the door or the ladder), and a heap of copper up to 3 high against the wall under them;</li>
     *   <li>a few blocks of what's left are knocked out too ({@code holes});</li>
     *   <li>then only what's held up stays: row by row from the walls up, a block stands on a block of the wall,
     *   the shell or the gallery, or reaches at most {@link #REACH} blocks sideways along its row to one that
     *   does, so nothing floats;</li>
     *   <li>the telescope is knocked askew on its tripod or off it altogether, and copper lies on the floor (kept
     *   off the stairwell's rail and the way in from the door) and the gallery.</li>
     * </ul>
     */
    static final class Cave {
        static final int SECTORS = 10;
        static final int TOP = DOME_BASE + 7;
        /** How far along a row a block of the shell can reach from one that's standing on something. */
        static final int REACH = 2;
        static final Block[] CUT = {Blocks.CUT_COPPER, Blocks.EXPOSED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER};
        static final Block[] SLABS = {Blocks.CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER_SLAB,
                Blocks.OXIDIZED_CUT_COPPER_SLAB};
        static final Block[] STAIRS = {Blocks.CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER_STAIRS,
                Blocks.OXIDIZED_CUT_COPPER_STAIRS};

        private final long seed;
        /** Rows of shell above the walls each sector keeps (sector 0 starts due north and they go round east). */
        final int rimMin;
        final int rimMax;
        /** Each sector's chance of having slumped in (one always has), and how many rows its wall top loses at most. */
        final float slump;
        final int wallDrop;
        /** Blocks of what's left of the shell knocked out anyway. */
        final float holes;
        /** Floor cells with copper on them, outside a slumped sector's heap; half as many on the gallery. */
        final float debris;
        /** The chance the telescope is only knocked askew on its tripod, not off it. */
        final float askew;
        /** Stages of weathering the copper's gone on (0 or 1). */
        final int age;

        private final int[] rim = new int[SECTORS];
        private final boolean[] slumped = new boolean[SECTORS];
        /** The wall's top row in each column. */
        private final int[][] wallTop = new int[15][15];
        /** The wall and shell blocks still standing, at [a + 7][y][c + 7]. */
        private final boolean[][][] kept = new boolean[15][TOP + 1][15];

        Cave(long seed, DesolationLevel level) {
            this.seed = seed;
            switch (level) {
                case RAIDED -> {
                    this.rimMin = 0;
                    this.rimMax = 3;
                    this.slump = 0.25F;
                    this.wallDrop = 2;
                    this.holes = 0.12F;
                    this.debris = 0.55F;
                    this.askew = 0.25F;
                    this.age = 0;
                }
                case ABANDONED -> {
                    this.rimMin = 2;
                    this.rimMax = 4;
                    this.slump = 0.0F;
                    this.wallDrop = 1;
                    this.holes = 0.05F;
                    this.debris = 0.4F;
                    this.askew = 0.75F;
                    this.age = 0;
                }
                case FORSAKEN -> {
                    this.rimMin = 1;
                    this.rimMax = 3;
                    this.slump = 0.2F;
                    this.wallDrop = 2;
                    this.holes = 0.1F;
                    this.debris = 0.5F;
                    this.askew = 0.45F;
                    this.age = 1;
                }
                default -> {
                    this.rimMin = 0;
                    this.rimMax = 3;
                    this.slump = 0.3F;
                    this.wallDrop = 2;
                    this.holes = 0.15F;
                    this.debris = 0.5F;
                    this.askew = 0.2F;
                    this.age = 1;
                }
            }
            for (int s = 0; s < SECTORS; s++) {
                this.rim[s] = this.rimMin + (int) (this.roll(s, 0, 0, 80) * (this.rimMax - this.rimMin + 1));
                this.slumped[s] = this.roll(s, 0, 0, 81) < this.slump;
            }
            this.slumped[(int) (this.roll(0, 0, 0, 82) * SECTORS)] = true;
            for (int s = 0; s < SECTORS; s++) {
                if (this.slumped[s]) {
                    this.rim[s] = -2;
                }
            }

            for (int a = -7; a <= 7; a++) {
                for (int c = -7; c <= 7; c++) {
                    int top = WALL_HEIGHT - 1;
                    double u = sector(a, c);
                    int nearest = Math.floorMod((int) Math.floor(u), SECTORS);
                    // The wall top only breaks in the middle of a slumped sector, and never where the door's banner
                    // or the ladder hang on it.
                    if (wall(a, c) && Math.abs(a) > 2 && this.slumped[nearest] && Math.abs(u - Math.floor(u) - 0.5D) < 0.35D) {
                        top -= this.wallDrop == 1 ? 1 : 1 + (this.roll(a, 0, c, 85) < 0.55F ? 1 : 0);
                    }
                    this.wallTop[a + 7][c + 7] = top;
                }
            }
            // A window's top course stands on its glass: with the wall beside it broken down, it comes down too.
            for (int i = 0; i < 8; i++) {
                int[] w = window(i);
                for (Direction side : Direction.Type.HORIZONTAL) {
                    int na = w[0] + side.getOffsetX();
                    int nc = w[1] + side.getOffsetZ();
                    if (wall(na, nc) && this.wallTop[na + 7][nc + 7] < WALL_HEIGHT - 1) {
                        this.wallTop[w[0] + 7][w[1] + 7] = 1;
                    }
                }
            }
            for (int a = -7; a <= 7; a++) {
                for (int c = -7; c <= 7; c++) {
                    int top = this.wallTop[a + 7][c + 7];
                    int rimHere = this.rimAt(a, c, sector(a, c));
                    for (int y = 0; y <= TOP; y++) {
                        if (y < WALL_HEIGHT) {
                            this.kept[a + 7][y][c + 7] = wall(a, c) && y <= top;
                        } else if (ObservatoryPiece.shell(a, y, c)) {
                            int dy = y - DOME_BASE;
                            this.kept[a + 7][y][c + 7] = dy <= rimHere
                                    && this.roll(a, y, c, 84) >= (dy == 0 ? this.holes * 0.4F : this.holes);
                        }
                    }
                }
            }
            this.settle();
        }

        float roll(int a, int y, int c, int k) {
            return Desolation.roll(this.seed, a, y, c, k);
        }

        /** Where a/c lies round the middle, in sectors from due north (0) going east. */
        static double sector(int a, int c) {
            double angle = Math.atan2(a, -c);
            return (angle < 0.0D ? angle + 2.0D * Math.PI : angle) / (2.0D * Math.PI) * SECTORS;
        }

        /** How many rows above the walls the shell still stands at a/c: the sectors' rims blended, a block either way. */
        private int rimAt(int a, int c, double u) {
            double v = u - 0.5D;
            int s0 = (int) Math.floor(v);
            double t = v - s0;
            double h = this.rim[Math.floorMod(s0, SECTORS)] * (1.0D - t) + this.rim[Math.floorMod(s0 + 1, SECTORS)] * t
                    + (this.roll(a, 0, c, 83) - 0.5F) * 1.6F;
            return Math.min(this.rimMax, (int) Math.floor(h + 0.5D));
        }

        /** Takes out, row by row from the walls up, every block of the shell that's left with nothing holding it up. */
        private void settle() {
            for (int y = DOME_BASE; y <= TOP; y++) {
                int[][] reach = new int[15][15];
                java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
                for (int i = 0; i < 15; i++) {
                    for (int j = 0; j < 15; j++) {
                        reach[i][j] = -1;
                        boolean onGallery = y - 1 == GALLERY && gallery(i - 7, j - 7);
                        if (this.kept[i][y][j] && (this.kept[i][y - 1][j] || onGallery)) {
                            reach[i][j] = 0;
                            queue.add(new int[]{i, j});
                        }
                    }
                }
                while (!queue.isEmpty()) {
                    int[] cell = queue.poll();
                    int d = reach[cell[0]][cell[1]];
                    if (d >= REACH) {
                        continue;
                    }
                    for (Direction side : Direction.Type.HORIZONTAL) {
                        int i = cell[0] + side.getOffsetX();
                        int j = cell[1] + side.getOffsetZ();
                        if (i >= 0 && i < 15 && j >= 0 && j < 15 && this.kept[i][y][j] && reach[i][j] < 0) {
                            reach[i][j] = d + 1;
                            queue.add(new int[]{i, j});
                        }
                    }
                }
                for (int i = 0; i < 15; i++) {
                    for (int j = 0; j < 15; j++) {
                        if (reach[i][j] < 0) {
                            this.kept[i][y][j] = false;
                        }
                    }
                }
            }
        }

        /** The dome's block at a/y/c, {@code copper} as the living dome draws it: gone, or still there and older. */
        BlockState shell(int a, int y, int c, Block copper) {
            if (!this.kept[a + 7][y][c + 7]) {
                return Blocks.AIR.getDefaultState();
            }
            int stage = 0;
            while (stage < CUT.length - 1 && CUT[stage] != copper) {
                stage++;
            }
            if (this.age > 0 && this.roll(a, y, c, 90) < 0.6F) {
                stage = Math.min(CUT.length - 1, stage + this.age);
            }
            return CUT[stage].getDefaultState();
        }

        /** How weathered a fallen piece of copper is: about as the dome's, plus the level's age. */
        private int stage(int a, int y, int c) {
            float r = this.roll(a, y, c, 91);
            int stage = r < 0.45F ? 0 : r < 0.7F ? 1 : r < 0.9F ? 2 : 3;
            return Math.min(CUT.length - 1, stage + this.age);
        }

        /** The top of a heap of fallen copper: a block, a slab, a stair lying at some angle, or rubble. */
        private BlockState piece(int a, int y, int c) {
            int stage = this.stage(a, y, c);
            float r = this.roll(a, y, c, 86);
            if (r < 0.25F) {
                return CUT[stage].getDefaultState();
            }
            if (r < 0.5F) {
                return bottomSlab(SLABS[stage]);
            }
            if (r < 0.85F) {
                Direction facing = Direction.fromHorizontal((int) (this.roll(a, y, c, 87) * 4));
                return STAIRS[stage].getDefaultState().with(StairsBlock.FACING, facing)
                        .with(StairsBlock.HALF, this.roll(a, y, c, 88) < 0.2F ? BlockHalf.TOP : BlockHalf.BOTTOM);
            }
            return RuinBlocks.COLLAPSED_RUBBLE.getDefaultState().with(RubbleBlock.LAYERS, 1 + (int) (this.roll(a, y, c, 89) * 3));
        }

        /** How high the copper lies at a/c on the floor: a heap against the wall under a slumped sector. */
        private int heap(int a, int c) {
            double r = r(a, c);
            if (this.slumped[Math.floorMod((int) Math.floor(sector(a, c)), SECTORS)]) {
                return Math.min(3, 1 + (r > 4.4D ? 1 : 0) + (this.roll(a, 0, c, 92) < 0.45F ? 1 : 0));
            }
            if (this.roll(a, 0, c, 93) >= this.debris) {
                return 0;
            }
            return this.roll(a, 0, c, 94) < 0.2F ? 2 : 1;
        }

        /** Piles copper from {@code y} up at a/c, {@code height} high, on air only. */
        private void pile(Builder b, int a, int y, int c, int height) {
            for (int i = 0; i < height; i++) {
                if (!b.inChunk(a, y + i, c) || !b.get(a, y + i, c).isAir()) {
                    return;
                }
                b.set(a, y + i, c, i == height - 1 ? this.piece(a, y + i, c) : CUT[this.stage(a, y + i, c)].getDefaultState());
            }
        }

        /** The rest of the cave-in, once the observatory is built (the shell's already gone by {@link #shell}). */
        void ruin(Builder b) {
            // The wall top that came down with a slumped section, windows and all.
            for (int a = -7; a <= 7; a++) {
                for (int c = -7; c <= 7; c++) {
                    if (wall(a, c)) {
                        for (int y = this.wallTop[a + 7][c + 7] + 1; y < WALL_HEIGHT; y++) {
                            b.set(a, y, c, Blocks.AIR);
                        }
                    }
                }
            }

            // The telescope: slid off true down onto the gallery, or knocked off its tripod and lying across it with
            // its lens on the floor below.
            for (int c = SCOPE_B - TUBE; c <= SCOPE_B; c++) {
                b.set(0, SCOPE_Y, c, Blocks.AIR);
            }
            BlockState rodNorth = Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.NORTH);
            if (this.roll(0, 0, 0, 95) < this.askew) {
                b.set(0, GALLERY + 1, SCOPE_B + 1, facing(Blocks.END_ROD, Direction.SOUTH));
                b.set(0, GALLERY + 1, SCOPE_B, rodNorth);
                b.set(0, SCOPE_Y, SCOPE_B - 1, rodNorth);
                b.set(0, SCOPE_Y, SCOPE_B - 2, rodNorth);
                b.set(0, SCOPE_Y, SCOPE_B - 3, facing(CogsdeepBlocks.TELESCOPE_LENS, Direction.NORTH));
            } else {
                BlockState rodEast = Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.EAST);
                b.set(0, GALLERY + 1, SCOPE_B - 1, Blocks.AIR);
                b.set(-1, GALLERY + 1, SCOPE_B + 1, facing(Blocks.END_ROD, Direction.WEST));
                b.set(0, GALLERY + 1, SCOPE_B + 1, rodEast);
                b.set(1, GALLERY + 1, SCOPE_B + 1, rodEast);
                b.set(4, 0, -2, facing(CogsdeepBlocks.TELESCOPE_LENS, Direction.EAST));
            }
            // The lightning rod from the top of the dome.
            b.set(-4, 0, -1, Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.EAST));

            // Copper on the floor, kept off the stairwell's rail (and the hole inside it) and the way in from the door.
            for (int a = -6; a <= 6; a++) {
                for (int c = -6; c <= 6; c++) {
                    if (Math.max(Math.abs(a), Math.abs(c)) <= 3 || r(a, c) > RADIUS - 1.05D || c >= 4 && Math.abs(a) <= 1) {
                        continue;
                    }
                    this.pile(b, a, 0, c, this.heap(a, c));
                }
            }
            // And on the gallery, clear of the top of the ladder.
            for (int a = -2; a <= 2; a++) {
                for (int c = -6; c <= GALLERY_B; c++) {
                    if (gallery(a, c) && !(a == -1 && c >= SCOPE_B - 1) && this.roll(a, GALLERY, c, 96) < this.debris * 0.5F) {
                        this.pile(b, a, GALLERY + 1, c, 1);
                    }
                }
            }
        }
    }

    private static double r(int a, int b) {
        return Math.sqrt(a * a + b * b);
    }

    @Override
    protected void build(Builder b) {
        DesolationLevel level = Desolation.level();
        Cave cave = level == null ? null : new Cave(this.seed, level);
        // Clear trees and plants round the tower (never the ground itself), and everything inside it.
        for (int a = -CLEAR_RADIUS; a <= CLEAR_RADIUS; a++) {
            for (int c = -CLEAR_RADIUS; c <= CLEAR_RADIUS; c++) {
                double r = r(a, c);
                if (r > CLEAR_RADIUS + 0.5D) {
                    continue;
                }
                for (int y = 0; y <= CLEAR_HEIGHT; y++) {
                    if (!b.inChunk(a, y, c)) {
                        continue;
                    }
                    BlockState state = b.get(a, y, c);
                    if (r <= RADIUS || state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES) || state.isReplaceable()
                            && state.getFluidState().isEmpty() && !state.isAir()) {
                        b.set(a, y, c, Blocks.AIR);
                    }
                }
            }
        }

        for (int a = -7; a <= 7; a++) {
            for (int c = -7; c <= 7; c++) {
                double r = r(a, c);
                if (r > RADIUS) {
                    continue;
                }
                boolean wall = r > RADIUS - 1.0D;
                // Floor (and a foundation down to the ground).
                b.set(a, -1, c, wall ? RIVETED_BRASS.base() : r > 4.4D && r <= 5.4D ? CUT_RIVETED_BRASS.base() : Blocks.SPRUCE_PLANKS);
                b.foundation(a, -1, c, GNOMISH_BRICKS.base().getDefaultState(), 8);
                if (wall) {
                    // Gnomish bricks in a brass frame: a riveted brass footing, a cut brass course under the dome.
                    for (int y = 0; y < WALL_HEIGHT; y++) {
                        b.set(a, y, c, y == 0 ? RIVETED_BRASS.base() : y == WALL_HEIGHT - 1 ? CUT_RIVETED_BRASS.base() : masonry(b));
                    }
                }
            }
        }
        // Windows every 45 degrees, but not by the door or the telescope.
        for (int i = 0; i < 8; i++) {
            int[] w = window(i);
            b.set(w[0], 2, w[1], Blocks.GLASS_PANE);
            b.set(w[0], 3, w[1], Blocks.GLASS_PANE);
        }

        // The dome: a shell of cut copper on the walls, with a slit 3 wide open to the north.
        for (int a = -7; a <= 7; a++) {
            for (int c = -7; c <= 7; c++) {
                for (int y = DOME_BASE; y <= DOME_BASE + 7; y++) {
                    double d = Math.sqrt(a * a + c * c + (y - DOME_BASE) * (y - DOME_BASE));
                    if (d > DOME) {
                        continue;
                    }
                    boolean slit = Math.abs(a) <= SLIT && c < 0 && y > DOME_BASE;
                    if (d > DOME - 1.15D && !slit) {
                        // The copper's drawn either way, so a caved-in dome leaves the rest of the build as it was.
                        Block copper = cutCopper(b);
                        b.set(a, y, c, cave == null ? copper.getDefaultState() : cave.shell(a, y, c, copper));
                    } else {
                        b.set(a, y, c, Blocks.AIR);
                    }
                }
            }
        }
        if (cave == null) {
            b.set(0, DOME_BASE + 7, 0, Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.UP));
            b.hangingLantern(0, DOME_BASE + 5, 0, 2);
        }

        // The door, facing out to the south, and a dirt path from it.
        b.set(0, 0, 6, door(DoubleBlockHalf.LOWER));
        b.set(0, 1, 6, door(DoubleBlockHalf.UPPER));
        // On a slope the ground before the door can stand higher than the floor: cut a way through it, and fill
        // a gap under the first steps out.
        for (int c = 7; c <= 12; c++) {
            for (int a = -1; a <= 1; a++) {
                float r = b.random.nextFloat();
                if (!b.inChunk(a, -1, c)) {
                    continue;
                }
                for (int y = 0; y <= 3; y++) {
                    if (!b.get(a, y, c).isAir()) {
                        b.set(a, y, c, Blocks.AIR);
                    }
                }
                if (c <= 8) {
                    b.foundation(a, -1, c, Blocks.COBBLESTONE.getDefaultState(), 3);
                }
                BlockState ground = b.get(a, -1, c);
                if (ground.isIn(BlockTags.DIRT) || ground.isAir() && c <= 8) {
                    b.set(a, -1, c, a == 0 || r < 0.6F ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT);
                }
            }
        }

        // The Gnome Cog banner over the door, brass on blue.
        b.banner(0, 4, 7, Direction.SOUTH, DyeColor.BLUE, ModBannerPatterns.GNOME_COG.in(DyeColor.YELLOW));

        // Lanterns on gnomish brick posts either side of the door.
        b.set(-1, 2, 7, Blocks.LANTERN);
        b.set(1, 2, 7, Blocks.LANTERN);
        b.set(-1, 1, 7, GNOMISH_BRICKS.wall());
        b.set(1, 1, 7, GNOMISH_BRICKS.wall());
        b.set(-1, 0, 7, GNOMISH_BRICKS.wall());
        b.set(1, 0, 7, GNOMISH_BRICKS.wall());

        // The gallery: spruce planks from the north wall out over the stairwell's rail, on log posts, railed with
        // fences on its open sides, with a ladder up the wall through a gap in its floor.
        for (int a = -2; a <= 2; a++) {
            for (int c = -6; c <= GALLERY_B; c++) {
                if (r(a, c) > RADIUS - 1.0D || a == -1 && c == SCOPE_B - 1) {
                    continue;
                }
                b.set(a, GALLERY, c, Blocks.SPRUCE_PLANKS);
                // (Where the dome's shell already comes down to the gallery, it's the rail.)
                boolean dome = Math.sqrt(a * a + c * c + (GALLERY + 1 - DOME_BASE) * (GALLERY + 1 - DOME_BASE)) > DOME - 1.15D;
                if ((c == GALLERY_B || Math.abs(a) == 2) && !dome) {
                    b.set(a, GALLERY + 1, c, Blocks.SPRUCE_FENCE);
                }
            }
        }
        for (int a = -2; a <= 2; a += 4) {
            for (int y = 0; y < GALLERY; y++) {
                b.set(a, y, SCOPE_B, Blocks.SPRUCE_LOG);
            }
        }
        for (int y = 0; y <= GALLERY; y++) {
            b.set(-1, y, SCOPE_B - 1, facing(Blocks.LADDER, Direction.SOUTH));
        }

        // The telescope: an end rod eyepiece at head height for someone standing on the gallery, then a tube of
        // lightning rods on a fence tripod, out through the slit, ending in the lens clear of the dome.
        b.set(0, SCOPE_Y, SCOPE_B, facing(Blocks.END_ROD, Direction.SOUTH));
        for (int i = 1; i < TUBE; i++) {
            b.set(0, SCOPE_Y, SCOPE_B - i, Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.NORTH));
        }
        b.set(0, SCOPE_Y, SCOPE_B - TUBE, facing(CogsdeepBlocks.TELESCOPE_LENS, Direction.NORTH));
        b.set(0, GALLERY + 1, SCOPE_B - 1, Blocks.SPRUCE_FENCE);
        b.set(1, GALLERY + 1, SCOPE_B - 1, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));

        // A cartography desk with a spyglass lying on it, a lectern, bookshelves and the chest.
        b.set(4, 0, -3, Blocks.CARTOGRAPHY_TABLE);
        b.frame(4, 1, -3, Direction.UP, new ItemStack(Items.SPYGLASS));
        b.set(3, 0, -4, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
        b.set(-4, 0, -3, facing(Blocks.LECTERN, Direction.EAST));
        b.set(-5, 0, 1, Blocks.BOOKSHELF);
        b.set(-5, 1, 1, Blocks.BOOKSHELF);
        b.set(-5, 0, 2, Blocks.BOOKSHELF);
        b.set(-5, 1, 2, Blocks.BOOKSHELF);
        b.set(-5, 2, 2, Blocks.LANTERN);
        b.container(5, 0, 1, facing(Blocks.CHEST, Direction.WEST), COMMONS_LOOT);
        b.set(5, 0, 2, Blocks.BARREL);
        b.set(5, 1, 2, Blocks.LANTERN);

        if (cave != null) {
            cave.ruin(b);
        }
    }

    private static BlockState door(DoubleBlockHalf half) {
        return Blocks.SPRUCE_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.HALF, half).with(DoorBlock.HINGE, DoorHinge.LEFT);
    }
}
