package mattonfire.dnd.world.gen.fortress;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The great hall: a long, high hall of polished deepslate with two rows of gold-capped pillars
 * hung with banners, chandeliers down the middle, feasting tables, and doorways down both sides
 * to the rest of the fortress.
 */
public class HallPiece extends FortressPiece {
    private static final int WIDTH = 17;
    private static final int HEIGHT = 13;
    private static final int MID = WIDTH / 2;
    /** Pillars stand every {@code BAY} blocks; side doorways open in every other bay. */
    private static final int BAY = 7;
    private static final int FIRST_PILLAR = 5;
    private static final int[] PILLAR_X = {3, WIDTH - 4};

    public record Exit(BlockPos front, Direction facing) {
    }

    public HallPiece(BlockBox box, Direction facing, long seed) {
        super(DwarvenFortressStructures.HALL, box, facing, seed);
    }

    public HallPiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.HALL, nbt);
    }

    /** Two or three side rooms down each side. */
    public static HallPiece create(Random random, BlockPos front, Direction facing) {
        int pairs = random.nextBoolean() ? 2 : 3;
        int length = 3 + 2 * BAY * pairs;
        return new HallPiece(boxFrom(front, facing, WIDTH, HEIGHT, length), facing, random.nextLong());
    }

    /** The z of each pair of side doorways: the middle of every other bay. */
    private List<Integer> exitZs() {
        List<Integer> zs = new ArrayList<>();
        for (int z = FIRST_PILLAR + BAY / 2 + 1; z + 4 < this.depth(); z += 2 * BAY) {
            zs.add(z);
        }
        return zs;
    }

    /** Where the side corridors start, just outside each side doorway, and which way they head. */
    public List<Exit> exits() {
        List<Exit> exits = new ArrayList<>();
        for (int z : this.exitZs()) {
            exits.add(new Exit(this.worldPos(-1, 0, z), this.worldDirection(-1, 0)));
            exits.add(new Exit(this.worldPos(WIDTH, 0, z), this.worldDirection(1, 0)));
        }
        return exits;
    }

    private boolean nearExit(int z, int margin) {
        return this.exitZs().stream().anyMatch(e -> Math.abs(e - z) <= margin);
    }

    @Override
    protected void build(Builder b) {
        int length = this.depth();
        int top = HEIGHT - 1;
        DyeColor[] colours = b.pick(new DyeColor[][]{
                {DyeColor.RED, DyeColor.YELLOW}, {DyeColor.BLUE, DyeColor.YELLOW},
                {DyeColor.BLACK, DyeColor.ORANGE}, {DyeColor.PURPLE, DyeColor.YELLOW}});

        b.shell(WIDTH, HEIGHT, length, Blocks.POLISHED_DEEPSLATE.getDefaultState());
        for (int z = 1; z < length - 1; z++) {
            boolean tiled = (z - FIRST_PILLAR) % BAY == 0;
            for (int x = 1; x < WIDTH - 1; x++) {
                if (tiled) {
                    b.set(x, 0, z, Blocks.DEEPSLATE_TILES);
                }
            }
            b.fill(MID - 1, 0, z, MID + 1, 0, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            b.fill(MID - 1, 1, z, MID + 1, 1, z, Blocks.RED_CARPET);
            // A gold band round the walls and a coved ceiling.
            b.set(0, 6, z, Blocks.GILDED_BLACKSTONE);
            b.set(WIDTH - 1, 6, z, Blocks.GILDED_BLACKSTONE);
            b.set(1, top - 1, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, true));
            b.set(WIDTH - 2, top - 1, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, true));
        }
        for (int x = 0; x < WIDTH; x++) {
            b.set(x, 6, 0, Blocks.GILDED_BLACKSTONE);
            b.set(x, 6, length - 1, Blocks.GILDED_BLACKSTONE);
        }

        b.doorway(MID, 0, 5, 7);
        b.doorway(MID, length - 1, 5, 7);
        for (int z : this.exitZs()) {
            this.sideDoorway(b, 0, z);
            this.sideDoorway(b, WIDTH - 1, z);
        }

        // Pillars, each with a banner on its aisle side and a lamp on the wall behind it.
        for (int z = FIRST_PILLAR; z <= length - 5; z += BAY) {
            DyeColor colour = colours[((z - FIRST_PILLAR) / BAY) % 2];
            for (int x : PILLAR_X) {
                b.column(x, z, top - 1);
                boolean west = x < MID;
                b.banner(west ? x + 1 : x - 1, 9, z, west ? Direction.EAST : Direction.WEST, colour);
            }
            b.set(1, 5, z, bottomSlab(Blocks.POLISHED_BLACKSTONE_SLAB));
            b.set(1, 4, z, lantern(true));
            b.set(WIDTH - 2, 5, z, bottomSlab(Blocks.POLISHED_BLACKSTONE_SLAB));
            b.set(WIDTH - 2, 4, z, lantern(true));
            b.banner(1, 9, z, Direction.EAST, colour);
            b.banner(WIDTH - 2, 9, z, Direction.WEST, colour);
        }

        // Chandeliers down the middle, and froglights set into the ceiling between them.
        for (int z = FIRST_PILLAR + 4; z <= length - 5; z += BAY) {
            b.chandelier(MID, top, z, 4);
        }
        for (int z = 2; z < length - 1; z += BAY) {
            b.set(MID, top, z, pillar(Blocks.OCHRE_FROGLIGHT, Direction.Axis.Y));
            b.set(4, top, z + 3, pillar(Blocks.OCHRE_FROGLIGHT, Direction.Axis.Y));
            b.set(WIDTH - 5, top, z + 3, pillar(Blocks.OCHRE_FROGLIGHT, Direction.Axis.Y));
        }

        // Feasting tables either side of the aisle, in the bays without doorways.
        for (int z = FIRST_PILLAR + 1; z <= length - 5; z++) {
            if ((z - FIRST_PILLAR) % BAY == 0 || this.nearExit(z, 3)) {
                continue;
            }
            for (int x : new int[]{5, WIDTH - 6}) {
                b.set(x, 1, z, topSlab(Blocks.DARK_OAK_SLAB));
                this.tableTop(b, x, z);
                b.set(x - 1, 1, z, stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST, false));
                b.set(x + 1, 1, z, stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
            }
        }

        int dwarves = 4 + b.random.nextInt(3);
        for (int i = 0; i < dwarves; i++) {
            b.dwarf(MID + b.random.nextInt(3) - 1, 1, 3 + b.random.nextInt(length - 6));
        }
    }

    /** A three-wide, four-tall doorway in a side wall (constant x). */
    private void sideDoorway(Builder b, int x, int z) {
        b.air(x, 1, z - 1, x, 4, z + 1);
        b.fill(x, 1, z - 2, x, 4, z - 2, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
        b.fill(x, 1, z + 2, x, 4, z + 2, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
        b.fill(x, 5, z - 2, x, 5, z + 2, Blocks.GILDED_BLACKSTONE);
        b.set(x, 4, z - 1, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.SOUTH, true));
        b.set(x, 4, z + 1, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, true));
    }

    private void tableTop(Builder b, int x, int z) {
        int roll = b.random.nextInt(10);
        if (roll < 2) {
            b.set(x, 2, z, b.candleState());
        } else if (roll < 4) {
            b.frame(x, 2, z, Direction.UP, new ItemStack(Items.COOKED_BEEF));
        } else if (roll < 5) {
            b.frame(x, 2, z, Direction.UP, new ItemStack(Items.BREAD));
        } else if (roll < 6) {
            b.frame(x, 2, z, Direction.UP, new ItemStack(Items.HONEY_BOTTLE));
        } else if (roll < 7) {
            b.set(x, 2, z, Blocks.FLOWER_POT);
        }
    }
}
