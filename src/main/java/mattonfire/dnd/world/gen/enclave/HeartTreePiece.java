package mattonfire.dnd.world.gen.enclave;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CaveVines;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.Direction;

/**
 * The Heart Tree in the middle of the enclave: a 5x5 trunk of birch wood with stripped-birch inlays,
 * {@link #HEIGHT} tall, roots flaring out 3 blocks at the base and a canopy of birch and flowering azalea
 * leaves {@link #CANOPY_RADIUS} across, hung with glow berries and lanterns. A spiral stair winds once
 * round the trunk, past the bridge deck at {@link ElvenEnclavePlanner#DECK} up to the Speaker's Hall at
 * {@link #HALL} (built by {@link SpeakersHallPiece}).
 */
public class HeartTreePiece extends EnclavePiece {
    public static final int HEIGHT = 34;
    public static final int HALL = 22;
    /** Half the trunk's width (5x5). */
    static final int TRUNK = 2;
    /** The bridge deck round the trunk: Chebyshev distance DECK_IN to DECK_OUT from the middle. */
    static final int DECK_IN = 4;
    static final int DECK_OUT = 6;
    static final int CANOPY_RADIUS = 9;
    private static final int CANOPY_Y = 31;
    private static final int CANOPY_HALF_HEIGHT = 5;
    /** Box half-size: the canopy plus a little. */
    static final int REACH = CANOPY_RADIUS + 1;
    /** The ground the trunk, roots and stair take up, which the ground pieces keep off. */
    static final int FOOTPRINT = DECK_OUT + 1;

    /** The stair's ring at Chebyshev distance 3, in walking order (clockwise seen from above). */
    private static final int[][] RING = ring(TRUNK + 1);

    public HeartTreePiece(int x, int y, int z, long seed) {
        super(ElvenEnclaveStructures.HEART_TREE, box(x, y, z, -REACH, -4, -REACH, REACH, CANOPY_Y + CANOPY_HALF_HEIGHT + 2, REACH),
                x, y, z, seed);
    }

    public HeartTreePiece(NbtCompound nbt) {
        super(ElvenEnclaveStructures.HEART_TREE, nbt);
    }

    /** The cells of the square ring at Chebyshev distance {@code r}, walking clockwise from (r, -r). */
    static int[][] ring(int r) {
        int[][] cells = new int[8 * r][];
        int i = 0;
        for (int z = -r; z < r; z++) {
            cells[i++] = new int[]{r, z};
        }
        for (int x = r; x > -r; x--) {
            cells[i++] = new int[]{x, r};
        }
        for (int z = r; z > -r; z--) {
            cells[i++] = new int[]{-r, z};
        }
        for (int x = -r; x < r; x++) {
            cells[i++] = new int[]{x, -r};
        }
        return cells;
    }

    /** Which way the stair climbs out of ring cell {@code i} (towards cell i + 1). */
    static Direction climb(int i) {
        int[] a = RING[i % RING.length];
        int[] b = RING[(i + 1) % RING.length];
        int dx = b[0] - a[0];
        int dz = b[1] - a[1];
        return dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /**
     * Local y of the stair block in ring cell {@code i}, or -1 where the stair doesn't pass. It climbs one
     * block a cell, less than once round, and its last step is level with the hall's floor.
     */
    static int stairY(int i) {
        return i < HALL ? i + 1 : -1;
    }

    static int ringIndex(int x, int z) {
        for (int i = 0; i < RING.length; i++) {
            if (RING[i][0] == x && RING[i][1] == z) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void build(Builder b) {
        // The forest's own trees are left to mingle with ours (clearing a box out of them leaves cut-off
        // walls of leaves); the stair, deck and hall clear their own headroom.
        this.trunk(b);
        this.roots(b);
        this.stair(b);
        this.deck(b);
        this.branches(b);
        this.canopy(b);
        // A warden and a wood elf at the foot of the tree.
        b.elf(5, 1, 0, ModEntityTypes.ELF_WARDEN);
        b.elf(-5, 1, 2, ModEntityTypes.WOOD_ELF);
    }

    private boolean inTrunk(int x, int y, int z) {
        if (Math.abs(x) > TRUNK || Math.abs(z) > TRUNK || y < 0 || y >= HEIGHT) {
            return false;
        }
        // Rounded corners above the flared base.
        return y < 3 || Math.abs(x) != TRUNK || Math.abs(z) != TRUNK;
    }

    private void trunk(Builder b) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = -TRUNK; x <= TRUNK; x++) {
                for (int z = -TRUNK; z <= TRUNK; z++) {
                    if (!this.inTrunk(x, y, z)) {
                        continue;
                    }
                    boolean face = Math.abs(x) == TRUNK || Math.abs(z) == TRUNK;
                    // Pale stripped inlays climbing the middle of each face.
                    boolean inlay = face && (x == 0 || z == 0) && y % 6 >= 3 && y % 6 <= 4;
                    b.set(x, y, z, inlay || !face ? Blocks.STRIPPED_BIRCH_WOOD : Blocks.BIRCH_WOOD);
                }
            }
        }
        for (int x = -TRUNK; x <= TRUNK; x++) {
            for (int z = -TRUNK; z <= TRUNK; z++) {
                b.anchor(x, 0, z, Blocks.ROOTED_DIRT.getDefaultState());
            }
        }
    }

    /** Roots flaring out 3 blocks from each face and corner, sinking into the ground. */
    private void roots(Builder b) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] d : dirs) {
            boolean diagonal = d[0] != 0 && d[1] != 0;
            for (int step = diagonal ? 2 : 1; step <= 3; step++) {
                int reach = diagonal ? TRUNK + step - 1 : TRUNK + step;
                int x = d[0] * reach;
                int z = d[1] * reach;
                int height = 3 - step;
                for (int y = 0; y <= height; y++) {
                    // Keep out of the stair's way where it's still low.
                    int i = ringIndex(x, z);
                    if (i >= 0 && stairY(i) >= 0 && stairY(i) <= y + 2) {
                        continue;
                    }
                    b.set(x, y, z, Blocks.BIRCH_WOOD);
                }
                b.anchor(x, 0, z, Blocks.ROOTED_DIRT.getDefaultState());
            }
        }
    }

    /** The spiral stair: spruce stairs on the ring round the trunk, a fence rail on the outside. */
    private void stair(Builder b) {
        for (int i = 0; i < RING.length; i++) {
            int y = stairY(i);
            if (y < 0) {
                continue;
            }
            int x = RING[i][0];
            int z = RING[i][1];
            b.air(x, y + 1, z, x, y + 3, z);
            b.set(x, y, z, stairs(Blocks.SPRUCE_STAIRS, climb(i), false));
            // Hold each step up with an upside-down stair under it (the first steps sit on the ground).
            if (y > 1) {
                b.set(x, y - 1, z, stairs(Blocks.SPRUCE_STAIRS, climb(i).getOpposite(), true));
            } else {
                b.anchor(x, y, z, Blocks.DIRT.getDefaultState());
            }
            // Rail on the outside, except where the stair passes the bridge deck.
            if (y < ElvenEnclavePlanner.DECK - 1 || y > ElvenEnclavePlanner.DECK + 1) {
                int ox = Math.abs(x) == TRUNK + 1 ? Integer.signum(x) : 0;
                int oz = Math.abs(z) == TRUNK + 1 ? Integer.signum(z) : 0;
                b.set(x + ox, y + 1, z + oz, Blocks.BIRCH_FENCE);
                if (ox != 0 && oz != 0) {
                    b.set(x + ox, y + 1, z, Blocks.BIRCH_FENCE);
                    b.set(x, y + 1, z + oz, Blocks.BIRCH_FENCE);
                }
            }
            if (i % 6 == 3) {
                b.set(x, y + 3, z, lantern(true));
            }
        }
    }

    /** Whether local x/z is on the bridge deck round the trunk. */
    static boolean isDeck(int x, int z) {
        int c = Math.max(Math.abs(x), Math.abs(z));
        return c >= DECK_IN && c <= DECK_OUT && x * x + z * z <= (DECK_OUT + 1.6D) * (DECK_OUT + 1.6D);
    }

    /** The bridge deck round the trunk, railed on its outer edge; bridges cut through the rail where they land. */
    private void deck(Builder b) {
        int y = ElvenEnclavePlanner.DECK;
        for (int x = -DECK_OUT; x <= DECK_OUT; x++) {
            for (int z = -DECK_OUT; z <= DECK_OUT; z++) {
                if (!isDeck(x, z)) {
                    continue;
                }
                b.set(x, y, z, Blocks.BIRCH_PLANKS);
                b.air(x, y + 1, z, x, y + 3, z);
                // Beams underneath along the axes and diagonals.
                if (x == 0 || z == 0 || Math.abs(x) == Math.abs(z)) {
                    b.set(x, y - 1, z, Blocks.STRIPPED_BIRCH_WOOD);
                }
                boolean edge = false;
                for (Direction d : Direction.Type.HORIZONTAL) {
                    int nx = x + d.getOffsetX();
                    int nz = z + d.getOffsetZ();
                    if (!isDeck(nx, nz) && Math.max(Math.abs(nx), Math.abs(nz)) > DECK_IN) {
                        edge = true;
                    }
                }
                if (edge) {
                    b.set(x, y + 1, z, Blocks.BIRCH_FENCE);
                }
            }
        }
        // Lanterns on the corners of the rail.
        for (int[] c : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            b.set(c[0] * (DECK_OUT - 1), y + 1, c[1] * (DECK_OUT - 1), lantern(false));
        }
        // Struts from the trunk to under the deck's corners.
        for (int[] c : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            for (int s = 1; s < 3; s++) {
                b.set(c[0] * (TRUNK + 1 + s), y - 3 + s, c[1] * (TRUNK + 1 + s), Blocks.STRIPPED_BIRCH_WOOD);
            }
        }
    }

    /** Eight boughs reaching out from the top of the trunk into the canopy. */
    private void branches(Builder b) {
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4.0D + 0.3D;
            int start = 25 + (k % 3);
            for (int s = 2; s <= 7; s++) {
                int x = (int) Math.round(Math.cos(angle) * s);
                int z = (int) Math.round(Math.sin(angle) * s);
                int y = start + s / 2;
                b.set(x, y, z, Blocks.BIRCH_WOOD);
            }
        }
    }

    private void canopy(Builder b) {
        int r = CANOPY_RADIUS;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int lowest = Integer.MAX_VALUE;
                for (int y = CANOPY_Y - CANOPY_HALF_HEIGHT; y <= CANOPY_Y + CANOPY_HALF_HEIGHT; y++) {
                    double dy = (y - CANOPY_Y) / (double) CANOPY_HALF_HEIGHT;
                    double d = (x * x + z * z) / (double) (r * r) + dy * dy;
                    float n = this.noise(x, y, z);
                    if (d > 1.0D - 0.25D * n || this.inTrunk(x, y, z) || b.get(x, y, z).isOf(Blocks.BIRCH_WOOD)) {
                        continue;
                    }
                    BlockState leaf = n < 0.18F ? leaves(Blocks.FLOWERING_AZALEA_LEAVES)
                            : n < 0.26F ? leaves(Blocks.AZALEA_LEAVES) : leaves(Blocks.BIRCH_LEAVES);
                    b.set(x, y, z, leaf);
                    lowest = Math.min(lowest, y);
                }
                // Nothing hangs over the hall and its stair.
                if (lowest == Integer.MAX_VALUE || Math.max(Math.abs(x), Math.abs(z)) <= SpeakersHallPiece.RADIUS + 2) {
                    continue;
                }
                // Under the canopy: glow berries and the odd lantern.
                float hang = this.noise(x, -7, z);
                if (hang < 0.12F) {
                    int length = 1 + (int) (this.noise(x, -8, z) * 4);
                    for (int s = 1; s <= length; s++) {
                        boolean tip = s == length;
                        b.set(x, lowest - s, z, (tip ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).getDefaultState()
                                .with(CaveVines.BERRIES, this.noise(x, -9 - s, z) < 0.6F));
                    }
                } else if (hang > 0.95F) {
                    b.set(x, lowest - 1, z, Blocks.CHAIN);
                    b.set(x, lowest - 2, z, lantern(true));
                }
            }
        }
    }
}
