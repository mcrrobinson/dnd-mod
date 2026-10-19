package mattonfire.dnd.world.gen.village;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructureContext;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * A hobbit hole: rooms dug into a grassy hill, a round door and windows in a front wall under the hill's cut
 * turf face, a fenced front garden, and inside a
 * panelled hall with a fireplace on a sooty hearth, a kitchen with larder shelves and herbs hanging
 * from the ceiling, and a table laid for second breakfast. Bigger smials add a pantry and a bedroom either side of the hall.
 * <p>
 * Each smial's outside is picked from its seed (see {@link Look}): the front wall's material and whether it arches
 * down into the hill at the ends, the door frame, round or square windows and where they sit, leaves draping over
 * the turf lip, and the garden's fence, path and lamps. So the smials of a village don't look alike.
 */
public class SmialPiece extends HobbitPiece {
    public enum Size {
        SMALL(5, 7), MEDIUM(8, 8), LARGE(9, 10);

        final int half;
        final int depth;

        Size(int half, int depth) {
            this.half = half;
            this.depth = depth;
        }
    }

    private record DoorStyle(Block door, Block planks) {
    }

    // Mostly green, the Bag End way, with the odd red or yellow door.
    private static final DoorStyle[] DOORS = {
            new DoorStyle(Blocks.WARPED_DOOR, Blocks.WARPED_PLANKS),
            new DoorStyle(Blocks.WARPED_DOOR, Blocks.WARPED_PLANKS),
            new DoorStyle(Blocks.WARPED_DOOR, Blocks.WARPED_PLANKS),
            new DoorStyle(Blocks.MANGROVE_DOOR, Blocks.MANGROVE_PLANKS),
            new DoorStyle(Blocks.OAK_DOOR, Blocks.OAK_PLANKS),
            new DoorStyle(Blocks.BIRCH_DOOR, Blocks.BIRCH_PLANKS),
            new DoorStyle(Blocks.CRIMSON_DOOR, Blocks.CRIMSON_PLANKS)
    };
    /** What the front wall is made of. */
    private enum Front { EARTH, FIELDSTONE, MUD, DAUB, TIMBER }

    /** What the round door's frame is made of. */
    private enum Frame { STONE, TIMBER, BRICK }

    /** What the front garden is fenced with. */
    private enum Fence { OAK, SPRUCE, BIRCH, HEDGE, WALL }

    /** What the garden path is laid with. */
    private enum Path { DIRT, GRAVEL, STEPPING }

    /**
     * A smial's outside, hashed from its seed rather than drawn from the builder's random, so it's the same in
     * every chunk and leaves the rest of the piece's draws alone. {@code arch}: the wall's top curves down
     * towards the ends, with bare earth above. {@code windows}: x offsets from the door. {@code flip}: the water
     * butt and bench swap sides. {@code lamps}: 0 none, 1 on both gate posts, 2 on one.
     */
    private record Look(Front front, boolean arch, Frame frame, boolean roundWindows, int[] windows, boolean drape,
                        Fence fence, Path path, boolean flip, int lamps) {
    }

    private static final Front[] FRONTS = {Front.EARTH, Front.FIELDSTONE, Front.FIELDSTONE, Front.MUD, Front.DAUB, Front.TIMBER};
    private static final int[][] SMALL_WINDOWS = {{-4, 4}, {-4}, {4}};
    private static final int[][] MEDIUM_WINDOWS = {{-4, 4, 7}, {-7, -4, 4}, {-4, 5}};
    private static final int[][] LARGE_WINDOWS = {{-8, -4, 5}, {-7, 4, 8}, {-8, -4, 4}, {-5, 4}};
    private static final DyeColor[] RUGS = {DyeColor.RED, DyeColor.ORANGE, DyeColor.BROWN, DyeColor.GREEN, DyeColor.YELLOW};
    private static final Block[] CROPS = {Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS, Blocks.WHEAT};

    /** Blocks of hillside around the rooms: room for the farthest a hill reaches, plus its ragged edge. */
    private static final int SLOPE = 10;
    /** z of the front wall; the garden is in front of it. */
    private static final int FACADE = 5;
    private static final int HEIGHT = 13;

    private final Size size;
    private final int door;

    public SmialPiece(BlockBox box, Direction facing, long seed, Size size, int door) {
        super(HobbitVillageStructures.SMIAL, box, facing, seed);
        this.size = size;
        this.door = door;
    }

    public SmialPiece(NbtCompound nbt) {
        super(HobbitVillageStructures.SMIAL, nbt);
        this.size = Size.values()[nbt.getInt("Size")];
        this.door = nbt.getInt("Door");
    }

    public static SmialPiece create(Random random, int x, int y, int z, Direction facing) {
        int roll = random.nextInt(10);
        Size size = roll < 4 ? Size.SMALL : roll < 8 ? Size.MEDIUM : Size.LARGE;
        BlockBox box = centeredBox(x, y, z, facing, width(size), HEIGHT, depth(size));
        return new SmialPiece(box, facing, random.nextLong(), size, random.nextInt(DOORS.length));
    }

    static int width(Size size) {
        return 2 * (SLOPE + size.half + 1) + 1;
    }

    static int depth(Size size) {
        return FACADE + size.depth + 2 + SLOPE;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Size", this.size.ordinal());
        nbt.putInt("Door", this.door);
    }

    private int cx() {
        return SLOPE + this.size.half + 1;
    }

    private int backWall() {
        return FACADE + this.size.depth + 1;
    }

    /**
     * Height of the hill at a column, or -1 for none. Over the rooms it's a lumpy crown at least 7 high; from there
     * it falls away in a smooth curve, reaching out a different way on each side and behind, with a ragged edge.
     * In front it wraps round the ends of the front wall, leaving the garden's cutting open (see {@link #garden}),
     * and some hills have a second, smaller mound to one side or behind. All of it is smooth noise on the piece's
     * seed, so no two hills are the same shape and none of them looks drawn with a compass.
     */
    private int hillTop(int x, int z) {
        if (this.garden(x, z)) {
            return -1;
        }
        int x0 = this.cx() - this.size.half - 1;
        int x1 = this.cx() + this.size.half + 1;
        int z1 = this.backWall();
        boolean left = x < this.cx();
        double reach = left ? this.span(20, 6.0D, 8.5D) : this.span(21, 6.0D, 8.5D);
        double dx = (x < x0 ? x0 - x : Math.max(x - x1, 0)) * 8.0D / reach;
        double dz = (z > z1 ? z - z1 : 0) * 8.0D / this.span(22, 6.0D, 8.5D);
        // In front of the facade the hill runs out over a different distance each side.
        double front = z < FACADE ? (FACADE - z) * (left ? this.span(23, 0.9D, 2.0D) : this.span(24, 0.9D, 2.0D)) : 0.0D;
        double d = Math.sqrt(dx * dx + dz * dz + front * front) + 1.5D * this.noise(x, z, 4.5D, 25);
        int top = 0;
        if (d < 8.0D) {
            double crown = 7.0D + Math.max(0.0D, 1.8D * this.noise(x, z, 7.0D, 26) + 0.5D);
            top = (int) Math.round(crown * 0.5D * (1.0D + Math.cos(Math.PI * Math.max(d, 0.0D) / 8.0D)));
        }
        top = Math.max(top, this.mound(x, z, x0, x1, z1));
        // Over the front wall the hill comes down to the turf lip, then rises behind it, so no dirt cliff towers
        // over the door; in front of the wall it stays below the lip.
        top = Math.min(top, z >= FACADE ? 6 + (z - FACADE) : 6);
        return top >= 1 ? top : -1;
    }

    /** Whether x/z is on the garden's fence line: its front edge, or a side the hill doesn't close in. */
    private boolean rim(int x, int z) {
        if (!this.garden(x, z)) {
            return false;
        }
        return z == 0 || (!this.garden(x - 1, z) && this.hillTop(x - 1, z) < 1)
                || (!this.garden(x + 1, z) && this.hillTop(x + 1, z) < 1);
    }

    /** Height of the smaller second mound some hills have, to the left, right or behind, or 0. */
    private int mound(int x, int z, int x0, int x1, int z1) {
        int where = this.style(44, 5);
        if (where >= 3) {
            return 0; // two in five hills have none
        }
        double mx = where == 0 ? x0 - this.span(45, 1.0D, 3.0D) : where == 1 ? x1 + this.span(45, 1.0D, 3.0D)
                : this.span(45, x0 + 2, x1 - 2);
        double mz = where == 2 ? z1 + this.span(46, 1.0D, 3.0D) : this.span(46, FACADE + 2, z1);
        double radius = this.span(47, 4.0D, 6.5D);
        double d = Math.sqrt((x - mx) * (x - mx) + (z - mz) * (z - mz)) + this.noise(x, z, 3.5D, 48);
        if (d >= radius) {
            return 0;
        }
        return (int) Math.round(this.span(49, 4.0D, 7.0D) * 0.5D * (1.0D + Math.cos(Math.PI * Math.max(d, 0.0D) / radius)));
    }

    /**
     * How far the cutting in front of the wall is open on each side of the door, at the wall: from just past the
     * bench and water butt to the whole wall. The hill covers the rest of the wall's front.
     */
    private int openHalf(boolean left) {
        int x0 = this.cx() - this.size.half - 1;
        int x1 = this.cx() + this.size.half + 1;
        int full = left ? this.doorX() - x0 - 1 : x1 - this.doorX() - 1;
        return 4 + this.style(left ? 40 : 41, Math.max(1, full - 3));
    }

    /**
     * Whether x/z is in the front garden: the cutting in front of the wall, open {@link #openHalf} either side of
     * the door at the wall and widening towards the gate by a seeded amount each side (none: straight sides).
     */
    private boolean garden(int x, int z) {
        int x0 = this.cx() - this.size.half - 1;
        int x1 = this.cx() + this.size.half + 1;
        if (z < 0 || z >= FACADE || x < x0 || x > x1) {
            return false;
        }
        boolean left = x < this.doorX();
        double spread = this.span(left ? 42 : 43, 0.0D, 1.3D);
        return Math.abs(x - this.doorX()) <= this.openHalf(left) + (FACADE - 1 - z) * spread;
    }

    /** A number between {@code min} and {@code max} from the piece's seed and {@code salt}. */
    private double span(int salt, double min, double max) {
        return min + (max - min) * this.style(salt, 1000) / 999.0D;
    }

    /** Smooth value noise in [-1, 1] at local x/z, with features about {@code scale} blocks across. */
    private double noise(double x, double z, double scale, int salt) {
        double fx = x / scale;
        double fz = z / scale;
        int ix = MathHelper.floor(fx);
        int iz = MathHelper.floor(fz);
        double tx = fx - ix;
        double tz = fz - iz;
        tx = tx * tx * (3.0D - 2.0D * tx);
        tz = tz * tz * (3.0D - 2.0D * tz);
        return MathHelper.lerp(tz, MathHelper.lerp(tx, this.lattice(ix, iz, salt), this.lattice(ix + 1, iz, salt)),
                MathHelper.lerp(tx, this.lattice(ix, iz + 1, salt), this.lattice(ix + 1, iz + 1, salt)));
    }

    private double lattice(int x, int z, int salt) {
        long h = (MathHelper.hashCode(x, salt, z) ^ this.seed) * 0x9E3779B97F4A7C15L;
        return ((h >>> 40) & 0xFFFF) / 32767.5D - 1.0D;
    }

    /** A number in [0, n) from the piece's seed and {@code salt}. */
    private int style(int salt, int n) {
        long h = (this.seed ^ (salt * 0x632BE59BD9B4E019L)) * 0x9E3779B97F4A7C15L;
        return (int) Math.floorMod(h >>> 33, (long) n);
    }

    /** A number in [0, n) for the block at local x/y/z, from the piece's seed. */
    private int mix(int x, int y, int z, int n) {
        long h = (MathHelper.hashCode(x, y, z) ^ this.seed) * 0x9E3779B97F4A7C15L;
        return (int) Math.floorMod(h >>> 33, (long) n);
    }

    private Look look() {
        Front front = FRONTS[this.style(1, FRONTS.length)];
        int[][] layouts = this.size == Size.SMALL ? SMALL_WINDOWS : this.size == Size.MEDIUM ? MEDIUM_WINDOWS : LARGE_WINDOWS;
        return new Look(front, front != Front.EARTH && this.style(2, 4) != 0, Frame.values()[this.style(3, Frame.values().length)],
                this.style(4, 3) != 0, layouts[this.style(5, layouts.length)], this.style(6, 2) == 0,
                Fence.values()[this.style(7, Fence.values().length)], Path.values()[this.style(8, Path.values().length)],
                this.style(9, 2) == 0, this.style(10, 3));
    }

    /** Local x of the front door: a little off the middle of the front, either way, so the front isn't mirrored. */
    private int doorX() {
        int most = this.size == Size.SMALL ? 1 : 2;
        return this.cx() + this.style(11, 2 * most + 1) - most;
    }

    /** The garden path, and so the lane, leads to the door. */
    @Override
    protected int entranceX() {
        return this.doorX();
    }

    private static Block fieldstone(Builder b) {
        return b.chance(0.4F) ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE;
    }

    @Override
    protected void build(Builder b) {
        int cx = this.cx();
        int h = this.size.half;
        int x0 = cx - h - 1;
        int x1 = cx + h + 1;
        int iz0 = FACADE + 1;
        int iz1 = FACADE + this.size.depth;
        int back = iz1 + 1;

        Look look = this.look();
        b.lawn(x0, 0, x1, FACADE - 1, 10);
        this.buildHill(b, x0, x1);
        this.buildRooms(b, cx, x0, x1, iz0, iz1, back);
        this.buildGarden(b, look, this.doorX(), x0, x1);
        this.buildFacade(b, look, this.doorX(), x0, x1);

        int hx0 = this.size == Size.SMALL ? cx - h : cx - 5;
        int hx1 = this.size == Size.LARGE ? cx + 5 : cx + h;
        this.furnishHall(b, cx, hx0, hx1, iz0, iz1);
        if (this.size != Size.SMALL) {
            this.furnishPantry(b, cx - h, cx - 7, iz0, iz1);
        }
        if (this.size == Size.LARGE) {
            this.furnishBedroom(b, cx + 7, cx + h, iz0, iz1);
        }
    }

    private void buildHill(Builder b, int x0, int x1) {
        BlockState bush = Blocks.AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        BlockState flowering = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int x = 0; x < this.width(); x++) {
            for (int z = 0; z < this.depth(); z++) {
                int top = this.hillTop(x, z);
                if (top < 1) {
                    continue;
                }
                // Where a wing meets the front garden its side is a bare bank: earth with boulders showing.
                boolean bank = z < FACADE && (this.garden(x - 1, z) || this.garden(x + 1, z));
                b.ground(x, z, Blocks.DIRT);
                for (int y = 1; y < top; y++) {
                    b.set(x, y, z, bank ? this.bank(x, z, y) : Blocks.DIRT.getDefaultState());
                }
                b.set(x, top, z, Blocks.GRASS_BLOCK);
                // Long grass and flowers come in patches, with the odd bush or mossy boulder.
                double meadow = this.noise(x, z, 3.5D, 30);
                int roll = this.mix(x, top + 1, z, 1000);
                if (roll < 8 && !bank) {
                    b.set(x, top + 1, z, roll < 3 ? flowering : bush);
                } else if (roll < 11 && !bank) {
                    b.set(x, top + 1, z, Blocks.MOSSY_COBBLESTONE);
                } else if (meadow > 0.35D && roll < 400) {
                    b.flower(x, top + 1, z);
                } else if (roll < 300 + (int) (meadow * 300.0D)) {
                    b.set(x, top + 1, z, roll % 7 == 0 ? Blocks.FERN : Blocks.GRASS);
                } else {
                    b.set(x, top + 1, z, Blocks.AIR);
                }
            }
        }
    }

    /** A block in a wing's bare bank beside the garden: earth, with boulders in patches. */
    private BlockState bank(int x, int z, int y) {
        double rock = this.noise(z + x * 7.0D, y * 1.5D, 2.5D, 31);
        if (rock > 0.3D) {
            return (this.mix(x, y, z, 3) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE).getDefaultState();
        }
        int roll = this.mix(x, y, z, 10);
        return (roll < 6 ? Blocks.DIRT : roll < 8 ? Blocks.COARSE_DIRT : Blocks.ROOTED_DIRT).getDefaultState();
    }

    private void buildRooms(Builder b, int cx, int x0, int x1, int iz0, int iz1, int back) {
        int h = this.size.half;
        b.fill(x0, 0, FACADE, x1, 0, back, Blocks.SPRUCE_PLANKS);
        b.fill(x0, 1, iz0, x0, 4, back, Blocks.OAK_PLANKS);
        b.fill(x1, 1, iz0, x1, 4, back, Blocks.OAK_PLANKS);
        b.fill(x0, 1, back, x1, 4, back, Blocks.OAK_PLANKS);
        b.fill(x0, 5, FACADE, x1, 5, back, Blocks.OAK_PLANKS);
        b.air(cx - h, 1, iz0, cx + h, 4, iz1);

        // Ceiling beams and the posts holding them up.
        for (int z = iz0 + 1; z <= iz1; z += 3) {
            b.fill(cx - h, 5, z, cx + h, 5, z, pillar(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            b.fill(x0, 1, z, x0, 4, z, pillar(Blocks.SPRUCE_LOG, Direction.Axis.Y));
            b.fill(x1, 1, z, x1, 4, z, pillar(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        }

        // Partitions for the pantry (left) and bedroom (right), with a doorway near the front.
        int hx0 = cx - h;
        int hx1 = cx + h;
        if (this.size != Size.SMALL) {
            b.fill(cx - 6, 1, iz0, cx - 6, 4, iz1, Blocks.OAK_PLANKS);
            b.air(cx - 6, 1, iz0 + 1, cx - 6, 2, iz0 + 1);
            hx0 = cx - 5;
        }
        if (this.size == Size.LARGE) {
            b.fill(cx + 6, 1, iz0, cx + 6, 4, iz1, Blocks.OAK_PLANKS);
            b.air(cx + 6, 1, iz0 + 1, cx + 6, 2, iz0 + 1);
            hx1 = cx + 5;
        }

        // Rounded ceiling edges along the hall walls.
        for (int z = iz0; z <= iz1; z++) {
            b.set(hx0, 4, z, stairs(Blocks.OAK_STAIRS, Direction.WEST, true));
            b.set(hx1, 4, z, stairs(Blocks.OAK_STAIRS, Direction.EAST, true));
        }
        for (int x = hx0; x <= hx1; x++) {
            if (Math.abs(x - cx) > 2) {
                b.set(x, 4, iz1, stairs(Blocks.OAK_STAIRS, IN, true));
            }
        }

        // Fireplace in the back wall on a sooty hearth, with the chimney poking out of the hill.
        b.fill(cx - 2, 1, back, cx + 2, 4, back, ModBlockFamilies.HEARTH_STONE.base());
        b.set(cx, 1, back, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.FACING, OUT));
        b.set(cx, 2, back, Blocks.AIR);
        b.fill(cx - 1, 1, back + 1, cx + 1, 2, back + 1, ModBlockFamilies.HEARTH_STONE.base());
        int chimney = Math.max(this.hillTop(cx, back + 1), 4) + 2;
        b.fill(cx, 3, back + 1, cx, chimney, back + 1, Blocks.BRICKS);
        b.set(cx, chimney + 1, back + 1, Blocks.CAMPFIRE.getDefaultState());
        b.fill(cx - 2, 3, iz1, cx + 2, 3, iz1, topSlab(Blocks.SPRUCE_SLAB));
        b.candles(cx - 2, 4, iz1);
        b.pot(cx - 1, 4, iz1);
        b.candles(cx + 1, 4, iz1);
        b.pot(cx + 2, 4, iz1);
        if (this.size == Size.LARGE) {
            ItemStack ring = new ItemStack(Items.GOLD_NUGGET);
            ring.setCustomName(Text.literal("A Magic Ring"));
            b.frame(cx, 4, iz1, OUT, ring);
        } else {
            b.candles(cx, 4, iz1);
        }
    }

    /** {@code cx} here is the door's x, which the windows, the arch and the posts are laid out from. */
    private void buildFacade(Builder b, Look look, int cx, int x0, int x1) {
        // The front wall on a footing, up to its top course (lower towards the ends when it arches), bare earth
        // above that, and the hill's cut turf face on top.
        for (int x = x0; x <= x1; x++) {
            int top = this.wallTop(look, x, cx);
            for (int y = 1; y <= 5; y++) {
                BlockState state;
                if (y > top) {
                    state = this.earth(x, y);
                } else if (y == 1) {
                    state = look.front() == Front.EARTH ? Blocks.COARSE_DIRT.getDefaultState() : fieldstone(b).getDefaultState();
                } else {
                    state = this.wall(look.front(), x, y, top, cx, x0, x1);
                }
                b.set(x, y, FACADE, state);
            }
            // The cut turf lip only where the hill comes down to it; where it rises higher, more bare earth.
            b.set(x, 6, FACADE, this.hillTop(x, FACADE) > 6 ? this.earth(x, 6) : ModBlockFamilies.SMIAL_TURF_WALL.base().getDefaultState());
        }

        DoorStyle style = DOORS[this.door];
        switch (look.frame()) {
            case STONE -> b.roundDoor(cx, 1, FACADE, style.door(), style.planks());
            case TIMBER -> b.roundDoor(cx, 1, FACADE, style.door(), style.planks(),
                    pillar(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y), pillar(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X),
                    pillar(Blocks.SPRUCE_LOG, Direction.Axis.Z), stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, true),
                    stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, true));
            case BRICK -> b.roundDoor(cx, 1, FACADE, style.door(), style.planks(),
                    Blocks.BRICKS.getDefaultState(), Blocks.BRICKS.getDefaultState(), Blocks.CHISELED_STONE_BRICKS.getDefaultState(),
                    stairs(Blocks.BRICK_STAIRS, Direction.WEST, true), stairs(Blocks.BRICK_STAIRS, Direction.EAST, true));
        }

        for (int offset : look.windows()) {
            if (cx + offset < x0 + 2 || cx + offset > x1 - 2 || Math.abs(offset) + 1 > this.openHalf(offset < 0)) {
                continue; // past the end of the wall, or behind the hill where it wraps round the wall
            }
            if (look.roundWindows()) {
                b.roundWindow(cx + offset, 3, FACADE, true);
            } else {
                this.squareWindow(b, cx + offset);
            }
        }

        // Leaves spilling over the turf lip here and there.
        if (look.drape()) {
            BlockState[] leaves = {
                    Blocks.AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true),
                    Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true)
            };
            for (int x = x0; x <= x1; x++) {
                int roll = this.mix(x, 6, FACADE - 1, 6);
                if (roll < 3 && this.garden(x, FACADE - 1)) {
                    b.set(x, 6, FACADE - 1, leaves[roll % 2]);
                    if (roll == 0 && Math.abs(x - cx) > 2) {
                        b.set(x, 5, FACADE - 1, leaves[1]);
                    }
                }
            }
        }
    }

    /** Height of the front wall's top course at x: 5, or curving down to 3 at the ends when it arches. */
    private int wallTop(Look look, int x, int cx) {
        if (!look.arch()) {
            return 5;
        }
        double d = (x - cx) / (this.size.half + 1.0D);
        return Math.max(2, 5 - (int) Math.round(2.4D * d * d));
    }

    /** Bare earth in the hill's face: mostly dirt, with coarse and rooted dirt mixed in. */
    private BlockState earth(int x, int y) {
        int roll = this.mix(x, y, FACADE, 20);
        return (roll < 12 ? Blocks.DIRT : roll < 17 ? Blocks.COARSE_DIRT : Blocks.ROOTED_DIRT).getDefaultState();
    }

    /** The front wall's block at x/y, above the footing and up to its top course {@code top}. */
    private BlockState wall(Front front, int x, int y, int top, int cx, int x0, int x1) {
        int d = Math.abs(x - cx);
        boolean post = x == x0 || x == x1 || d == 3 || d == 7;
        return switch (front) {
            case EARTH -> this.mix(x, y, FACADE, 9) == 0 ? Blocks.MOSS_BLOCK.getDefaultState() : this.earth(x, y);
            case FIELDSTONE -> {
                int roll = this.mix(x, y, FACADE, 10);
                yield (roll < 4 ? Blocks.COBBLESTONE : roll < 7 ? Blocks.MOSSY_COBBLESTONE : roll < 9 ? Blocks.STONE : Blocks.ANDESITE)
                        .getDefaultState();
            }
            case MUD -> (y == top ? Blocks.MUD_BRICKS : Blocks.PACKED_MUD).getDefaultState();
            case DAUB -> y == top ? ModBlockFamilies.TIMBERED_WATTLE_AND_DAUB.base().getDefaultState()
                    : post ? pillar(Blocks.SPRUCE_LOG, Direction.Axis.Y) : ModBlockFamilies.WATTLE_AND_DAUB.base().getDefaultState();
            case TIMBER -> y == top ? pillar(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X)
                    : post ? pillar(Blocks.DARK_OAK_LOG, Direction.Axis.Y) : Blocks.SPRUCE_PLANKS.getDefaultState();
        };
    }

    /** A square window three wide and two high, under a little trapdoor hood. */
    private void squareWindow(Builder b, int x) {
        b.fill(x - 1, 2, FACADE, x + 1, 3, FACADE, Blocks.GLASS_PANE);
        b.fill(x - 1, 4, FACADE - 1, x + 1, 4, FACADE - 1, Blocks.SPRUCE_TRAPDOOR.getDefaultState()
                .with(TrapdoorBlock.FACING, OUT).with(TrapdoorBlock.HALF, BlockHalf.TOP));
    }

    /** {@code cx} here is the door's x: the path, the gate and the beds either side are laid out from it. */
    private void buildGarden(Builder b, Look look, int cx, int x0, int x1) {
        BlockState fence = switch (look.fence()) {
            case OAK -> Blocks.OAK_FENCE.getDefaultState();
            case SPRUCE -> Blocks.SPRUCE_FENCE.getDefaultState();
            case BIRCH -> Blocks.BIRCH_FENCE.getDefaultState();
            case HEDGE -> Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
            case WALL -> Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState();
        };
        Block gate = switch (look.fence()) {
            case SPRUCE, HEDGE -> Blocks.SPRUCE_FENCE_GATE;
            case BIRCH -> Blocks.BIRCH_FENCE_GATE;
            default -> Blocks.OAK_FENCE_GATE;
        };
        // The fence runs along the front, and up the sides only where no bank of the hill closes the garden in.
        for (int x = x0; x <= x1; x++) {
            for (int z = 0; z < FACADE; z++) {
                if (this.rim(x, z)) {
                    b.set(x, 1, z, fence);
                }
            }
        }
        for (int z = 0; z < FACADE; z++) {
            b.ground(cx, z, switch (look.path()) {
                case DIRT -> Blocks.DIRT_PATH;
                case GRAVEL -> Blocks.GRAVEL;
                case STEPPING -> z % 2 == 0 ? (this.mix(cx, 0, z, 2) == 0 ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE)
                        : Blocks.DIRT_PATH;
            });
        }
        b.gate(cx, 1, 0, gate, OUT);
        int near = look.flip() ? -1 : 1;
        if (look.lamps() == 1) {
            b.set(cx - 1, 2, 0, lantern(false));
            b.set(cx + 1, 2, 0, lantern(false));
        } else if (look.lamps() == 2) {
            b.set(cx + near, 2, 0, lantern(false));
        }

        // A bed of flowers, vegetables or pumpkins and berries either side of the path.
        for (int side = -1; side <= 1; side += 2) {
            int kind = b.random.nextInt(3);
            Block crop = b.pick(CROPS);
            int from = side < 0 ? x0 + 1 : cx + 1;
            int to = side < 0 ? cx - 1 : x1 - 1;
            for (int x = from; x <= to; x++) {
                for (int z = 1; z < FACADE; z++) {
                    if (!this.garden(x, z) || this.rim(x, z)) {
                        continue; // under the hill, or the fence
                    }
                    if (z == FACADE - 1 && Math.abs(x - cx) == 1) {
                        continue; // doorstep
                    }
                    if (kind == 0 || Math.abs(x - cx) == 1) {
                        if (b.chance(0.7F)) {
                            b.flower(x, 1, z);
                        }
                    } else if (kind == 1) {
                        b.ground(x, z, Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, 7));
                        b.set(x, 1, z, ripe(crop));
                    } else {
                        float roll = b.random.nextFloat();
                        if (roll < 0.3F) {
                            b.set(x, 1, z, roll < 0.2F ? Blocks.PUMPKIN : Blocks.MELON);
                        } else if (roll < 0.55F) {
                            b.set(x, 1, z, Blocks.SWEET_BERRY_BUSH.getDefaultState().with(SweetBerryBushBlock.AGE, 3));
                        } else if (roll < 0.75F) {
                            b.flower(x, 1, z);
                        }
                    }
                }
            }
        }

        // A water butt by the door and a bench against the wall, on either side.
        b.set(cx - 2 * near, 1, FACADE - 1, Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3));
        b.set(cx - 2 * near, 2, FACADE - 1, Blocks.AIR);
        b.set(cx + 2 * near, 1, FACADE - 1, stairs(Blocks.OAK_STAIRS, IN, false));
        b.set(cx + 3 * near, 1, FACADE - 1, stairs(Blocks.OAK_STAIRS, IN, false));
        b.set(cx + 2 * near, 2, FACADE - 1, Blocks.AIR);
        b.set(cx + 3 * near, 2, FACADE - 1, Blocks.AIR);
    }

    private void furnishHall(Builder b, int cx, int hx0, int hx1, int iz0, int iz1) {
        int tz0 = iz0 + this.size.depth / 2 - 1;
        int tz1 = tz0 + 1;

        // Rug, table laid with food, and chairs round it.
        DyeColor rug = b.pick(RUGS);
        b.fill(cx - 3, 1, tz0 - 1, cx + 3, 1, tz1 + 1, carpet(rug));
        b.fill(cx - 2, 1, tz0, cx + 2, 1, tz1, topSlab(Blocks.SPRUCE_SLAB));
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = tz0; z <= tz1; z++) {
                b.tableFood(x, 2, z);
            }
        }
        for (int x = cx - 2; x <= cx + 2; x += 2) {
            b.set(x, 1, tz0 - 1, stairs(Blocks.OAK_STAIRS, OUT, false));
            b.set(x, 1, tz1 + 1, stairs(Blocks.OAK_STAIRS, IN, false));
        }

        // Kitchen along the left wall, hams and loaves hanging above it.
        int start = this.size == Size.SMALL ? iz0 + 1 : iz0 + 2;
        for (int z = start; z < iz1; z++) {
            switch ((z - start) % 5) {
                case 0 -> {
                    b.barrel(hx0, 1, z, Direction.UP, PANTRY);
                    b.set(hx0, 2, z, Blocks.CAKE);
                }
                case 1 -> b.set(hx0, 1, z, facing(Blocks.SMOKER, Direction.EAST));
                case 2 -> b.set(hx0, 1, z, facing(Blocks.FURNACE, Direction.EAST));
                case 3 -> b.set(hx0, 1, z, Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3));
                default -> {
                    b.larder(hx0, 1, z, Direction.EAST, LARDER);
                    b.plate(hx0, 2, z);
                }
            }
            b.frame(hx0, 3, z, Direction.EAST, HobbitFood.hanging(b.random));
            if ((z - start) % 2 == 0) {
                b.herbs(hx0 + 1, 4, z, (z - start) % 4 == 0);
            }
        }
        b.larder(hx0, 1, iz1, Direction.EAST, PANTRY);
        b.larder(hx0, 2, iz1, Direction.EAST, LARDER);

        // A reading nook along the right wall.
        start = this.size == Size.LARGE ? iz0 + 2 : iz0 + 1;
        for (int z = start; z < iz1; z++) {
            switch ((z - start) % 4) {
                case 0 -> {
                    b.set(hx1, 1, z, Blocks.BOOKSHELF);
                    b.set(hx1, 2, z, Blocks.BOOKSHELF);
                    b.pot(hx1, 3, z);
                }
                case 1, 3 -> {
                    b.set(hx1, 1, z, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
                    if (b.chance(0.5F)) {
                        b.frame(hx1, 3, z, Direction.WEST, new ItemStack(Items.MAP));
                    }
                }
                default -> {
                    b.set(hx1, 1, z, topSlab(Blocks.SPRUCE_SLAB));
                    b.tableFood(hx1, 2, z);
                }
            }
        }
        b.chest(hx1, 1, iz1, Direction.WEST, LARDER);
        b.set(hx1, 2, iz1, Blocks.AIR);
        b.pot(hx0, 1, iz0);
        if (this.size != Size.LARGE) {
            b.pot(hx1, 1, iz0);
        }

        b.set(cx, 4, iz0, lantern(true));
        b.set(cx - 3, 4, tz1, lantern(true));
        b.set(cx + 3, 4, tz0, lantern(true));
        b.set(cx, 4, iz1 - 1, lantern(true));

        b.hobbit(cx + 3, 1, iz0);
        if (this.size != Size.SMALL) {
            b.hobbit(cx - 3, 1, iz1 - 1);
        }
    }

    private void furnishPantry(Builder b, int px0, int px1, int iz0, int iz1) {
        // Barrels all along the outer wall, more food stacked on top, and more hanging above.
        Block[] stacked = {Blocks.HAY_BLOCK, Blocks.PUMPKIN, Blocks.MELON, Blocks.BARREL, Blocks.BEEHIVE};
        for (int z = iz0; z <= iz1; z++) {
            if (z % 2 == 0) {
                b.barrel(px0, 1, z, Direction.EAST, PANTRY);
            } else {
                b.larder(px0, 1, z, Direction.EAST, LARDER);
            }
            Block top = b.pick(stacked);
            if (top == Blocks.HAY_BLOCK) {
                b.larder(px0, 2, z, Direction.EAST, PANTRY);
            } else if (top == Blocks.BARREL) {
                b.barrel(px0, 2, z, Direction.UP, PANTRY);
            } else if (top == Blocks.BEEHIVE) {
                b.beehive(px0, 2, z, Direction.EAST, 0);
            } else {
                b.set(px0, 2, z, top);
            }
            b.frame(px0, 3, z, Direction.EAST, HobbitFood.hanging(b.random));
            if (z % 3 == 0) {
                b.herbs(px0, 4, z, z % 2 == 0);
            }
        }
        if (px1 > px0 + 1) {
            for (int z = iz0 + 3; z <= iz1; z++) {
                if (z % 2 == 1) {
                    b.chest(px1, 1, z, Direction.WEST, LARDER);
                    b.set(px1, 2, z, Blocks.CAKE);
                } else {
                    b.larder(px1, 1, z, Direction.WEST, PANTRY);
                    b.set(px1, 2, z, Blocks.PUMPKIN);
                }
            }
        }
        b.set(px1, 4, iz0 + 2, lantern(true));
        b.set(px1, 4, iz1 - 1, lantern(true));
    }

    private void furnishBedroom(Builder b, int bx0, int bx1, int iz0, int iz1) {
        DyeColor color = b.pick(new DyeColor[]{DyeColor.RED, DyeColor.YELLOW, DyeColor.GREEN, DyeColor.ORANGE, DyeColor.LIME});
        b.fill(bx0, 1, iz0, bx1, 1, iz1, carpet(color));
        b.bed(bx1, 1, iz1 - 1, IN, color);
        b.set(bx1, 1, iz1 - 2, topSlab(Blocks.SPRUCE_SLAB));
        b.candles(bx1, 2, iz1 - 2);
        b.chest(bx1, 1, iz1 - 3, Direction.WEST, LARDER);
        b.set(bx0, 1, iz1, Blocks.BOOKSHELF);
        b.set(bx0, 2, iz1, Blocks.BOOKSHELF);
        b.pot(bx0, 3, iz1);
        b.barrel(bx1, 1, iz0, Direction.WEST, PANTRY);
        b.plate(bx1, 2, iz0);
        b.set(bx0 + 1, 4, iz0 + 3, lantern(true));
        b.hobbit(bx0 + 1, 1, iz0 + 2);
    }

    private static BlockState carpet(DyeColor color) {
        return Registries.BLOCK.get(new Identifier(color.getName() + "_carpet")).getDefaultState();
    }
}
