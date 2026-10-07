package mattonfire.dnd.world.gen.village;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.SweetBerryBushBlock;
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
import net.minecraft.util.math.random.Random;

/**
 * A hobbit hole: rooms dug into a grassy hill, a round door and round windows in the front wall,
 * a fenced front garden, and inside a panelled hall with a fireplace, a kitchen and a table laid
 * for second breakfast. Bigger smials add a pantry and a bedroom either side of the hall.
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
    private static final DyeColor[] RUGS = {DyeColor.RED, DyeColor.ORANGE, DyeColor.BROWN, DyeColor.GREEN, DyeColor.YELLOW};
    private static final Block[] CROPS = {Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS, Blocks.WHEAT};

    /** Blocks of hillside around the rooms. */
    private static final int SLOPE = 6;
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
     * Height of the hill at a column, or -1 for none: level over the rooms, sloping down all
     * round, with two wings reaching forward beside the front garden.
     */
    private int hillTop(int x, int z) {
        int x0 = this.cx() - this.size.half - 1;
        int x1 = this.cx() + this.size.half + 1;
        int z1 = this.backWall();
        int dx = x < x0 ? x0 - x : Math.max(x - x1, 0);
        int dz = z < FACADE ? FACADE - z : Math.max(z - z1, 0);
        if (z < FACADE && dx == 0) {
            return -1;
        }
        if (dx == 0 && dz == 0) {
            int edge = Math.min(Math.min(x - x0, x1 - x), Math.min(z - FACADE, z1 - z));
            return edge >= 3 ? 7 : 6;
        }
        int top = (int) Math.round(6.4D - Math.sqrt(dx * dx + dz * dz) * 1.15D);
        return top >= 1 ? top : -1;
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

        b.lawn(x0, 0, x1, FACADE - 1, 10);
        this.buildHill(b, x0, x1);
        this.buildRooms(b, cx, x0, x1, iz0, iz1, back);
        this.buildGarden(b, cx, x0, x1);
        this.buildFacade(b, cx, x0, x1);

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
        for (int x = 0; x < this.width(); x++) {
            for (int z = 0; z < this.depth(); z++) {
                int top = this.hillTop(x, z);
                if (top < 1) {
                    continue;
                }
                // Fieldstone retaining walls where the wings meet the front garden.
                boolean wall = z < FACADE && (x == x0 - 1 || x == x1 + 1);
                b.ground(x, z, Blocks.DIRT);
                for (int y = 1; y < top; y++) {
                    b.set(x, y, z, wall ? fieldstone(b) : Blocks.DIRT);
                }
                b.set(x, top, z, Blocks.GRASS_BLOCK);
                float roll = b.random.nextFloat();
                if (roll < 0.3F) {
                    b.set(x, top + 1, z, Blocks.GRASS);
                } else if (roll < 0.38F) {
                    b.flower(x, top + 1, z);
                } else if (roll < 0.4F) {
                    b.set(x, top + 1, z, Blocks.FERN);
                } else {
                    b.set(x, top + 1, z, Blocks.AIR);
                }
            }
        }
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
            b.fill(cx - h, 5, z, cx + h, 5, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            b.fill(x0, 1, z, x0, 4, z, log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
            b.fill(x1, 1, z, x1, 4, z, log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
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

        // Fireplace in the back wall, with the chimney poking out of the hill.
        b.fill(cx - 2, 1, back, cx + 2, 4, back, Blocks.BRICKS);
        b.set(cx, 1, back, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.FACING, OUT));
        b.set(cx, 2, back, Blocks.AIR);
        b.fill(cx - 1, 1, back + 1, cx + 1, 2, back + 1, Blocks.BRICKS);
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

    private void buildFacade(Builder b, int cx, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = 1; y <= 5; y++) {
                b.set(x, y, FACADE, y == 1 ? fieldstone(b) : Blocks.MUD_BRICKS);
            }
        }

        DoorStyle style = DOORS[this.door];
        b.roundDoor(cx, 1, FACADE, style.door(), style.planks());

        this.window(b, cx - 4);
        this.window(b, cx + 4);
        if (this.size == Size.MEDIUM) {
            this.window(b, cx + 7);
        }
        if (this.size == Size.LARGE) {
            this.window(b, cx - 8);
            this.window(b, cx + 8);
        }
    }

    private void window(Builder b, int x) {
        b.roundWindow(x, 3, FACADE, true);
    }

    private void buildGarden(Builder b, int cx, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            b.set(x, 1, 0, Blocks.OAK_FENCE);
        }
        for (int z = 1; z < FACADE; z++) {
            b.set(x0, 1, z, Blocks.OAK_FENCE);
            b.set(x1, 1, z, Blocks.OAK_FENCE);
        }
        for (int z = 0; z < FACADE; z++) {
            b.ground(cx, z, Blocks.DIRT_PATH);
        }
        b.gate(cx, 1, 0, Blocks.OAK_FENCE_GATE, OUT);
        b.set(cx - 1, 2, 0, lantern(false));
        b.set(cx + 1, 2, 0, lantern(false));

        // A bed of flowers, vegetables or pumpkins and berries either side of the path.
        for (int side = -1; side <= 1; side += 2) {
            int kind = b.random.nextInt(3);
            Block crop = b.pick(CROPS);
            int from = side < 0 ? x0 + 1 : cx + 1;
            int to = side < 0 ? cx - 1 : x1 - 1;
            for (int x = from; x <= to; x++) {
                for (int z = 1; z < FACADE; z++) {
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

        // A water butt by the door and a bench against the wall.
        b.set(cx - 2, 1, FACADE - 1, Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3));
        b.set(cx - 2, 2, FACADE - 1, Blocks.AIR);
        b.set(cx + 2, 1, FACADE - 1, stairs(Blocks.OAK_STAIRS, IN, false));
        b.set(cx + 3, 1, FACADE - 1, stairs(Blocks.OAK_STAIRS, IN, false));
        b.set(cx + 2, 2, FACADE - 1, Blocks.AIR);
        b.set(cx + 3, 2, FACADE - 1, Blocks.AIR);
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
                    b.barrel(hx0, 1, z, Direction.UP, LARDER);
                    b.plate(hx0, 2, z);
                }
            }
            b.frame(hx0, 3, z, Direction.EAST, HobbitFood.hanging(b.random));
        }
        b.set(hx0, 1, iz1, Blocks.HAY_BLOCK);
        b.set(hx0, 2, iz1, Blocks.HAY_BLOCK);

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
            b.barrel(px0, 1, z, Direction.EAST, z % 2 == 0 ? PANTRY : LARDER);
            Block top = b.pick(stacked);
            if (top == Blocks.BARREL) {
                b.barrel(px0, 2, z, Direction.UP, PANTRY);
            } else if (top == Blocks.BEEHIVE) {
                b.beehive(px0, 2, z, Direction.EAST, 0);
            } else {
                b.set(px0, 2, z, top);
            }
            b.frame(px0, 3, z, Direction.EAST, HobbitFood.hanging(b.random));
        }
        if (px1 > px0 + 1) {
            for (int z = iz0 + 3; z <= iz1; z++) {
                if (z % 2 == 1) {
                    b.chest(px1, 1, z, Direction.WEST, LARDER);
                    b.set(px1, 2, z, Blocks.CAKE);
                } else {
                    b.set(px1, 1, z, Blocks.HAY_BLOCK);
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
