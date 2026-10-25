package mattonfire.dnd.world.underdark.drow;

import static mattonfire.dnd.world.gen.PieceBlocks.facing;
import static mattonfire.dnd.world.gen.PieceBlocks.stairs;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.drow.Drow;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.enums.Thickness;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Drow Outpost tower: a hollow stalagmite spire. The door faces the shrine (local +b).
 * <ul>
 * <li>Outside it's rough dripstone and deepslate, {@value #BASE_RADIUS} blocks round at the foot narrowing a block by
 * the platform, then a single dripstone spike up to the tip (14-24 blocks high in all). Inside, a 7 by 7 shell of
 * Drowstone Bricks round a 5 by 5 hollow.</li>
 * <li>A stair of Drowstone Brick stairs spirals up round the wall ({@link #RING}, one block a step, each on a block of
 * its own), from beside the door to the platform. The middle 3 by 3 is open, so each floor's landing is the middle.</li>
 * <li>Floors of Web Bridge Planks: at 4 and 8 with a platform at 12-13, at 5 with one at 10-11. Each floor and the
 * platform leave the two cells under them open where the stair climbs through.</li>
 * <li>On the floors, spidersilk sleeping mats and barrels; on the top floor the tower's chest
 * ({@code chests/drow_outpost_tower}). A Faerie Fire Lantern hangs under every floor; arrow slits look out three ways
 * on every level; a Spidersilk Curtain hangs in the door.</li>
 * <li>Cobwebs across the stair above a floor, 20% per flight (the cobweb trap).</li>
 * <li>The platform: a Drowstone parapet (bridges cut through it), the spike in the middle; a lookout tower keeps a
 * warrior and a lantern up here.</li>
 * <li>A warrior on the ground floor; a mage on the first floor of one tower in two.</li>
 * </ul>
 * The stair makes less than one turn of its 16-cell ring, so the platform can be at most {@value #MAX_PLATFORM} up; a
 * taller tower (the Drow Spire's) raises its tip, not its platform.
 */
public class DrowTowerPiece extends UnderdarkPiece {
    /** The stair's ring round the middle, from the door cell clockwise (as {@code ShaftPiece}'s). */
    static final int[][] RING = {{0, 2}, {1, 2}, {2, 2}, {2, 1}, {2, 0}, {2, -1}, {2, -2}, {1, -2}, {0, -2},
            {-1, -2}, {-2, -2}, {-2, -1}, {-2, 0}, {-2, 1}, {-2, 2}, {-1, 2}};
    /** Outer radius at the foot (the rough outside, before its noise). */
    public static final double BASE_RADIUS = 5.3D;
    /** The box's half width. */
    static final int HALF = 6;
    /** Blocks out from the middle to just outside the door. */
    public static final int DOOR_OUT = 7;
    /** The highest platform the stair can reach without its ring overlapping itself. */
    public static final int MAX_PLATFORM = 14;
    /** Chance of cobwebs across each flight of stairs. */
    public static final float WEB_CHANCE = 0.2F;

    private final int platform;
    private final int tip;
    private final boolean lookout;
    private final boolean mage;

    public DrowTowerPiece(BlockPos center, Direction door, long seed, int platform, int tip, boolean lookout, boolean mage) {
        super(DrowOutposts.TOWER, center, door, seed, -HALF, -8, -HALF, HALF, Math.max(tip, platform + 4), HALF);
        if (platform > MAX_PLATFORM) {
            throw new IllegalArgumentException("A drow tower's platform can be at most " + MAX_PLATFORM + " up: " + platform);
        }
        this.platform = platform;
        this.tip = tip;
        this.lookout = lookout;
        this.mage = mage;
    }

    public DrowTowerPiece(NbtCompound nbt) {
        super(DrowOutposts.TOWER, nbt);
        this.platform = nbt.getInt("Platform");
        this.tip = nbt.getInt("Tip");
        this.lookout = nbt.getBoolean("Lookout");
        this.mage = nbt.getBoolean("Mage");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Platform", this.platform);
        nbt.putInt("Tip", this.tip);
        nbt.putBoolean("Lookout", this.lookout);
        nbt.putBoolean("Mage", this.mage);
    }

    public int platform() {
        return this.platform;
    }

    public int tip() {
        return this.tip;
    }

    public boolean lookout() {
        return this.lookout;
    }

    /** The heights of the floors between the ground and the platform. */
    public static int[] floors(int platform) {
        if (platform >= 12) {
            return new int[] {4, 8};
        }
        return platform >= 10 ? new int[] {5} : new int[0];
    }

    /** Where you stand on the platform, in the world (beside the spike). */
    public BlockPos platformSpot() {
        return this.at(1, this.platform + 1, -1);
    }

    /** Where you stand on the ground floor, in the world. */
    public BlockPos groundSpot() {
        return this.at(0, 0, 0);
    }

    /**
     * The inner shell's Drowstone Bricks from the ground to the platform, in the world (the door and the slits left
     * out): the survey checks every one is there, across the chunk borders the tower straddles.
     */
    public List<BlockPos> shell() {
        java.util.Set<Long> openings = new java.util.HashSet<>();
        openings.add(BlockPos.asLong(0, 0, 3));
        openings.add(BlockPos.asLong(0, 1, 3));
        List<Integer> bases = new ArrayList<>();
        bases.add(-1);
        for (int f : floors(this.platform)) {
            bases.add(f);
        }
        for (int base : bases) {
            for (int[] out : new int[][] {{0, -1}, {1, 0}, {-1, 0}}) {
                openings.add(BlockPos.asLong(out[0] * 3, base + 2, out[1] * 3));
                openings.add(BlockPos.asLong(out[0] * 3, base + 3, out[1] * 3));
            }
        }
        List<BlockPos> shell = new ArrayList<>();
        for (int y = 0; y <= this.platform; y++) {
            for (int a = -3; a <= 3; a++) {
                for (int c = -3; c <= 3; c++) {
                    if (Math.max(Math.abs(a), Math.abs(c)) == 3 && !openings.contains(BlockPos.asLong(a, y, c))) {
                        shell.add(this.at(a, y, c));
                    }
                }
            }
        }
        return shell;
    }

    /** The stair's steps from the ground (step 0) to the platform, in the world. */
    public List<BlockPos> steps() {
        List<BlockPos> steps = new ArrayList<>();
        for (int k = 0; k <= this.platform; k++) {
            int[] cell = stepCell(k);
            steps.add(this.at(cell[0], k, cell[1]));
        }
        return steps;
    }

    /** The ring cell of step {@code k} (step k is at height k). */
    static int[] stepCell(int k) {
        return RING[(k + 1) % RING.length];
    }

    /** The ring index of local a/b, or -1 off the ring. */
    static int ringIndex(int a, int b) {
        for (int j = 0; j < RING.length; j++) {
            if (RING[j][0] == a && RING[j][1] == b) {
                return j;
            }
        }
        return -1;
    }

    /** The outer radius at height y. */
    private double outer(int y) {
        return BASE_RADIUS - Math.max(0, y) / (double) this.platform;
    }

    @Override
    protected void build(Builder b) {
        int top = this.platform;
        BlockState bricks = ModBlockFamilies.DROWSTONE_BRICKS.base().getDefaultState();
        // The shell: Drowstone inside, rough rock outside, the hollow cleared.
        for (int y = -1; y <= top; y++) {
            double rr = this.outer(y);
            for (int a = -HALF; a <= HALF; a++) {
                for (int c = -HALF; c <= HALF; c++) {
                    int cheb = Math.max(Math.abs(a), Math.abs(c));
                    double d = Math.sqrt(a * a + c * c);
                    if (cheb <= 2) {
                        b.set(a, y, c, y == -1 || y == top ? bricks : Blocks.AIR.getDefaultState());
                    } else if (cheb == 3) {
                        b.set(a, y, c, bricks);
                    } else if (d <= rr + noise(this.seed, a, y, c) * 0.9D - 0.2D) {
                        b.set(a, y, c, rock(b));
                    }
                }
            }
        }
        // Foundations: rock under the shell down to the cave floor.
        for (int a = -HALF; a <= HALF; a++) {
            for (int c = -HALF; c <= HALF; c++) {
                if (Math.sqrt(a * a + c * c) <= BASE_RADIUS + 0.4D) {
                    b.propUp(a, -1, c, Blocks.COBBLED_DEEPSLATE.getDefaultState(), 7, false);
                }
            }
        }
        // The door: through the wall and the rock, a curtain hung in it.
        for (int c = 3; c <= HALF; c++) {
            b.clear(0, 0, c);
            b.clear(0, 1, c);
        }
        b.set(0, 0, 3, UnderdarkBlocks.SPIDERSILK_CURTAIN.getDefaultState());
        b.set(0, 1, 3, UnderdarkBlocks.SPIDERSILK_CURTAIN.getDefaultState());

        int[] floors = floors(top);
        // The floors and the platform, whole for now.
        for (int f : floors) {
            for (int a = -2; a <= 2; a++) {
                for (int c = -2; c <= 2; c++) {
                    b.set(a, f, c, UnderdarkBlocks.WEB_BRIDGE.getDefaultState());
                }
            }
        }
        // The stair, each step on a block of its own.
        Block stair = ModBlockFamilies.DROWSTONE_BRICKS.stairs();
        for (int k = 0; k <= top; k++) {
            int[] cell = stepCell(k);
            int[] from = RING[k % RING.length];
            Direction climb = Direction.fromVector(cell[0] - from[0], 0, cell[1] - from[1]);
            b.set(cell[0], k, cell[1], stairs(stair, climb == null ? Direction.EAST : climb, false));
            if (k >= 1) {
                b.set(cell[0], k - 1, cell[1], bricks);
            }
        }
        // Holes over the stair where it climbs through each floor and the platform.
        List<Integer> levels = new ArrayList<>();
        for (int f : floors) {
            levels.add(f);
        }
        levels.add(top);
        for (int f : levels) {
            for (int k = f - 2; k <= f - 1; k++) {
                if (k >= 0) {
                    int[] cell = stepCell(k);
                    b.clear(cell[0], f, cell[1]);
                }
            }
        }
        // Slits on every level, out of the back and both sides.
        List<Integer> bases = new ArrayList<>();
        bases.add(-1);
        bases.addAll(levels.subList(0, levels.size() - 1));
        for (int base : bases) {
            for (int[] out : new int[][] {{0, -1}, {1, 0}, {-1, 0}}) {
                for (int k = 3; k <= HALF; k++) {
                    b.clear(out[0] * k, base + 2, out[1] * k);
                    b.clear(out[0] * k, base + 3, out[1] * k);
                }
            }
        }
        // A lantern under every floor and under the platform.
        for (int f : levels) {
            b.hangingLantern(0, f - 1, 0, 1, UnderdarkBlocks.FAERIE_LANTERN);
        }
        // Cobwebs across a flight now and then.
        for (int base : bases) {
            if (b.chance(WEB_CHANCE)) {
                for (int k = base + 3; k <= base + 4 && k <= top; k++) {
                    int[] cell = stepCell(k);
                    b.set(cell[0], k + 1, cell[1], Blocks.COBWEB);
                }
            }
        }
        // Furnishings on the floors' free ring cells: mats and barrels, and the chest on the top floor.
        for (int i = 0; i < floors.length; i++) {
            int f = floors[i];
            List<int[]> free = this.freeCells(f);
            boolean topFloor = i == floors.length - 1;
            for (int n = 0; n < free.size(); n++) {
                int[] cell = free.get(n);
                Direction in = inward(cell);
                if (n == 0 && topFloor) {
                    b.chest(cell[0], f + 1, cell[1], in, DrowOutposts.TOWER_LOOT);
                } else if (n < 3) {
                    b.set(cell[0], f + 1, cell[1], UnderdarkBlocks.SPIDERSILK_CARPET);
                } else if (n == 3) {
                    b.set(cell[0], f + 1, cell[1], facing(Blocks.BARREL, Direction.UP));
                }
            }
        }
        if (floors.length == 0) {
            b.chest(-1, 0, 2, Direction.NORTH, DrowOutposts.TOWER_LOOT);
        }
        // The platform: the parapet, the spike and air over it.
        for (int a = -3; a <= 3; a++) {
            for (int c = -3; c <= 3; c++) {
                int cheb = Math.max(Math.abs(a), Math.abs(c));
                if (cheb == 3) {
                    b.set(a, top + 1, c, ModBlockFamilies.DROWSTONE_BRICKS.wall().getDefaultState());
                    b.clear(a, c, top + 2, top + 3);
                } else if (a != 0 || c != 0) {
                    b.clear(a, c, top + 1, top + 3);
                }
            }
        }
        for (int y = top + 1; y < this.tip; y++) {
            b.set(0, y, 0, Blocks.DRIPSTONE_BLOCK);
        }
        b.set(0, this.tip, 0, Blocks.POINTED_DRIPSTONE.getDefaultState()
                .with(PointedDripstoneBlock.VERTICAL_DIRECTION, Direction.UP)
                .with(PointedDripstoneBlock.THICKNESS, Thickness.TIP));
        b.lantern(0, top + 1, -1, UnderdarkBlocks.FAERIE_LANTERN);

        // Folk.
        b.resident(0, 0, 0, Drow.DROW_WARRIOR, 6);
        if (this.mage) {
            int y = floors.length > 0 ? floors[0] + 1 : 0;
            b.resident(1, y, 0, Drow.DROW_MAGE, 4);
        }
        if (this.lookout) {
            b.lantern(3, top + 2, 3, UnderdarkBlocks.FAERIE_LANTERN);
            b.resident(1, top + 1, -1, Drow.DROW_WARRIOR, 2);
        }
    }

    /**
     * The ring cells you can stand on at floor {@code f} and put things on without blocking the stair: the door cell,
     * the cells past the stair's top, and those over its steps at least three below the floor. Farthest first.
     */
    List<int[]> freeCells(int f) {
        List<int[]> free = new ArrayList<>();
        for (int j = RING.length - 1; j >= 0; j--) {
            int k = j - 1;
            boolean hasStep = j >= 1 && k <= this.platform;
            if (!hasStep || k <= f - 3) {
                free.add(RING[j]);
            }
        }
        return free;
    }

    /** The way from a ring cell towards the middle. */
    static Direction inward(int[] cell) {
        if (Math.abs(cell[0]) == 2) {
            return cell[0] > 0 ? Direction.WEST : Direction.EAST;
        }
        return cell[1] > 0 ? Direction.NORTH : Direction.SOUTH;
    }
}
