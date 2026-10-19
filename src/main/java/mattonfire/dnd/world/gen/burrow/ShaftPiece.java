package mattonfire.dnd.world.gen.burrow;

import static mattonfire.dnd.classes.Registry.ModBlockFamilies.*;
import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Registry.CogsdeepBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The spiral stair from the observatory floor down to the Commons. A 3 by 3 pillar of gnomish brick and brass
 * clockwork stands on the stair's axis ({@link #pillar}), with an amber Glowcrystal set into it every
 * {@link #BAND_EVERY} steps and a clock-faced brass cap on the observatory floor; the stair winds round it one
 * block wide (16 steps a turn, so 15 blocks of headroom), each step a stone brick stair on a block of its own;
 * a sealed wall of masonry 7 by 7 keeps the rock, caves and water out. Below the Commons' ceiling the walls stop
 * and the last turn winds down open in the middle of the room. At the top, a fence rails the opening except
 * for a one-block gap onto the landing by the first step, on the side of the observatory's door.
 *
 * <p>y = 0 is the burrow floor; {@link #top} is the observatory's floor block, one below where you stand up there.
 */
public class ShaftPiece extends CogsdeepPiece {
    /** The ring round the pillar, walking down from the top step (a/b; the first is by the door, then clockwise). */
    public static final int[][] RING = {{0, 2}, {1, 2}, {2, 2}, {2, 1}, {2, 0}, {2, -1}, {2, -2}, {1, -2}, {0, -2},
            {-1, -2}, {-2, -2}, {-2, -1}, {-2, 0}, {-2, 1}, {-2, 2}, {-1, 2}};
    /** The pillar's brass bands, one every this many blocks up from the Commons floor (one just under its ceiling). */
    static final int BAND_EVERY = 5;
    static final Block LIGHT = CogsdeepBlocks.GLOWCRYSTALS.get("amber");
    /** The walls start at the Commons' ceiling. */
    static final int WALLS_FROM = CommonsPiece.TOP;

    private final int top;

    public ShaftPiece(BlockPos center, Direction door, long seed, long burrow, int top) {
        super(CogsdeepStructures.SHAFT, center, door, seed, burrow, -3, -1, -3, 3, top + 3, 3);
        this.top = top;
    }

    public ShaftPiece(NbtCompound nbt) {
        super(CogsdeepStructures.SHAFT, nbt);
        this.top = nbt.getInt("Top");
    }

    @Override
    protected void writeNbt(net.minecraft.structure.StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Top", this.top);
    }

    /** The observatory floor's height above the burrow floor. */
    public int top() {
        return this.top;
    }

    /** Where the first step down is, in the world (one below the observatory floor). */
    public BlockPos firstStep() {
        return this.at(RING[1][0], this.top - 1, RING[1][1]);
    }

    /** Step {@code i} below the top (0 is the landing), in the world; i is clamped to the stair's length. */
    public BlockPos stepAt(int i) {
        int[] s = step(this.top, Math.max(0, Math.min(this.top, i)));
        return this.at(s[0], s[1], s[2]);
    }

    /** The step {@code i} below the top (0 is the landing by the gap), in local a, y, b. */
    static int[] step(int top, int i) {
        int[] cell = RING[i % RING.length];
        return new int[]{cell[0], top - i, cell[1]};
    }

    /**
     * The stair for a step climbed towards {@code climb} and reached, going up, by walking towards {@code in}. On
     * a straight run they're the same. Where the stair turns, the step faces the way you come in, so its raised
     * half is on the far side: up it's half a block onto the step, half onto its back, half onto the next step.
     * Faced the way you leave, its raised half would lie across your way a whole block above the step below. (A
     * corner shape wouldn't stay: stairs are joined up with their neighbours once the chunk is finished.)
     */
    static BlockState step(Block block, Direction climb, Direction in) {
        return stairs(block, in != null ? in : climb, false);
    }

    @Override
    protected void build(Builder b) {
        int top = this.top;
        for (int y = -1; y <= top; y++) {
            for (int a = -3; a <= 3; a++) {
                for (int c = -3; c <= 3; c++) {
                    int ring = Math.max(Math.abs(a), Math.abs(c));
                    if (ring <= 1) {
                        b.set(a, y, c, y < 0 || y >= top ? GNOMISH_BRICKS.base().getDefaultState() : pillar(a, y, c));
                    } else if (ring == 2) {
                        b.set(a, y, c, y == -1 ? GNOMISH_BRICKS.base() : Blocks.AIR);
                    } else if (y >= WALLS_FROM && y < top) {
                        b.set(a, y, c, masonry(b));
                    } else if (y == top) {
                        b.set(a, y, c, Blocks.SPRUCE_PLANKS);
                    }
                }
            }
        }
        // The steps, each on a block of its own. Step 0, the landing by the gap, is a spruce stair onto the
        // observatory floor (a full block there would be a whole block above the first step).
        b.set(RING[0][0], top, RING[0][1], step(Blocks.SPRUCE_STAIRS, Direction.SOUTH,
                Direction.fromVector(RING[0][0] - RING[1][0], 0, RING[0][1] - RING[1][1])));
        for (int i = 1; i <= top; i++) {
            int[] s = step(top, i);
            int[] up = RING[(i - 1) % RING.length];
            int[] down = RING[(i + 1) % RING.length];
            Direction climb = Direction.fromVector(up[0] - s[0], 0, up[1] - s[2]);
            if (climb == null) {
                climb = Direction.NORTH;
            }
            Direction in = Direction.fromVector(s[0] - down[0], 0, s[2] - down[1]);
            b.set(s[0], s[1], s[2], step(GNOMISH_BRICKS.stairs(), climb, in));
            if (s[1] - 1 >= 0) {
                b.set(s[0], s[1] - 1, s[2], GNOMISH_BRICKS.base());
            }
            // An amber Glowcrystal in the pillar beside every few steps, halfway up a bay.
            if (Math.floorMod(s[1] + 1, BAND_EVERY) == 2 && s[1] + 1 < top) {
                int pa = Math.max(-1, Math.min(1, s[0]));
                int pb = Math.max(-1, Math.min(1, s[2]));
                b.set(pa, s[1] + 1, pb, LIGHT);
            }
        }
        // The rail round the opening, open only onto the landing by the first step (and railed on its far side).
        for (int a = -3; a <= 3; a++) {
            for (int c = -3; c <= 3; c++) {
                if (Math.max(Math.abs(a), Math.abs(c)) == 3 && !(c == 3 && a == 0)) {
                    b.set(a, top + 1, c, Blocks.SPRUCE_FENCE);
                }
            }
        }
        b.set(RING[RING.length - 1][0], top + 1, RING[RING.length - 1][1], Blocks.SPRUCE_FENCE);
        // The pillar stands a block proud of the floor: riveted brass corners and a clock face on each side,
        // brass slabs over the clocks and the copper beacon in the middle.
        for (int a = -1; a <= 1; a++) {
            for (int c = -1; c <= 1; c++) {
                Direction side = Direction.fromVector(a, 0, c);
                if (a == 0 && c == 0) {
                    b.set(a, top + 1, c, RIVETED_BRASS.base());
                    b.set(a, top + 2, c, Blocks.COPPER_BLOCK);
                } else if (side != null) {
                    b.set(a, top + 1, c, facing(CogsdeepBlocks.CLOCKFACE, side));
                    b.set(a, top + 2, c, slab(CUT_RIVETED_BRASS, false));
                } else {
                    b.set(a, top + 1, c, RIVETED_BRASS.base());
                }
            }
        }
        b.set(0, top + 3, 0, Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.UP));
    }

    /**
     * The pillar's block at local a/y/c (ring 0 or 1, 0 <= y < top): Gnomish Brick corners, a band of Cut Riveted
     * Brass every {@link #BAND_EVERY} blocks (the lowest, at the Commons floor, Riveted Brass), and between the
     * bands a column of turning gears down the middle of each face, a Cogwork Panel above and below it.
     */
    static BlockState pillar(int a, int y, int c) {
        int bay = Math.floorMod(y, BAND_EVERY);
        if (y == 0) {
            return RIVETED_BRASS.base().getDefaultState();
        }
        if (bay == 0) {
            return CUT_RIVETED_BRASS.base().getDefaultState();
        }
        if (a == 0 || c == 0) {
            return (bay == 1 || bay == BAND_EVERY - 1 ? CogsdeepBlocks.COGWORK_PANEL : CogsdeepBlocks.GEARWALL)
                    .getDefaultState();
        }
        return GNOMISH_BRICKS.base().getDefaultState();
    }
}
