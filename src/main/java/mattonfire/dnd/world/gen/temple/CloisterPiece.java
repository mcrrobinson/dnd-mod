package mattonfire.dnd.world.gen.temple;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The cloister along one side of the courtyard: six small rooms off a narrow corridor (4-6 of them monks'
 * cells with a bed and a chest, the rest storerooms), a cross-hall with the door onto the courtyard, and
 * the library behind it with lecterns and walls of bookshelves.
 */
public class CloisterPiece extends TemplePiece {
    static final int WIDTH = DragonTemplePlanner.SIDE_WIDTH;
    static final int DEPTH = DragonTemplePlanner.DEPTH;
    static final int HEIGHT = 7;
    private static final int CORRIDOR = WIDTH / 2;
    /** Each room's first z; rooms are 3 deep with a wall after them. */
    private static final int[] ROOMS = {1, 5, 9};
    private static final int HALL = 13;
    private static final int LIBRARY = HALL + 1;

    public CloisterPiece(BlockPos front, Direction facing, long seed) {
        super(DragonTempleStructures.CLOISTER, boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, seed);
    }

    public CloisterPiece(NbtCompound nbt) {
        super(DragonTempleStructures.CLOISTER, nbt);
    }

    @Override
    protected void build(Builder b) {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                b.foundation(x, 0, z, Blocks.COBBLED_DEEPSLATE.getDefaultState());
                b.set(x, 0, z, x == CORRIDOR || z == HALL ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.DEEPSLATE_TILES);
                boolean outer = x == 0 || z == 0 || x == WIDTH - 1 || z == DEPTH - 1;
                boolean inner = z <= HALL - 1 && (x == CORRIDOR - 1 || x == CORRIDOR + 1 || z == 4 || z == 8 || z == HALL - 1)
                        || z == LIBRARY;
                for (int y = 1; y < HEIGHT; y++) {
                    if (y == HEIGHT - 1) {
                        b.set(x, y, z, outer ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.DEEPSLATE_TILES);
                    } else if (outer || inner && y < HEIGHT - 2) {
                        b.set(x, y, z, outer && y == 1 ? Blocks.POLISHED_BLACKSTONE_BRICKS : masonry(b));
                    } else {
                        b.set(x, y, z, Blocks.AIR);
                    }
                }
            }
        }
        // The corridor between the rooms, and its ends: into the cross-hall and on into the library.
        b.air(CORRIDOR, 1, 1, CORRIDOR, 3, LIBRARY);
        // The door onto the courtyard, at the end of the cross-hall.
        b.air(0, 1, HALL, 0, 3, HALL);
        b.set(0, 4, HALL, Blocks.GILDED_BLACKSTONE);

        int cells = 4 + b.random.nextInt(3);
        int made = 0;
        int slot = 0;
        for (int side = -1; side <= 1; side += 2) {
            for (int z0 : ROOMS) {
                int needed = cells - made;
                boolean cell = needed > 0 && (needed >= 6 - slot || b.chance(0.6F));
                this.room(b, side, z0, cell);
                made += cell ? 1 : 0;
                slot++;
            }
        }
        for (int z = 2; z <= 10; z += 4) {
            b.set(CORRIDOR, HEIGHT - 2, z, lantern(true));
        }
        b.set(CORRIDOR, HEIGHT - 2, HALL, lantern(true));

        // The library: bookshelves along both long walls, two lecterns and a reading table.
        for (int z = LIBRARY + 2; z < DEPTH - 2; z++) {
            if (z == LIBRARY + 7) {
                continue;
            }
            b.fill(1, 1, z, 1, 2, z, Blocks.BOOKSHELF);
            b.fill(WIDTH - 2, 1, z, WIDTH - 2, 2, z, Blocks.BOOKSHELF);
            if (z % 3 == 0) {
                b.set(1, 3, z, b.candle(Blocks.WHITE_CANDLE));
            }
        }
        b.set(CORRIDOR, 1, LIBRARY + 4, facing(Blocks.LECTERN, FRONT).with(LecternBlock.HAS_BOOK, false));
        b.set(CORRIDOR, 1, DEPTH - 4, facing(Blocks.LECTERN, FRONT).with(LecternBlock.HAS_BOOK, false));
        b.set(CORRIDOR - 1, 1, LIBRARY + 7, Blocks.DARK_OAK_PLANKS);
        b.set(CORRIDOR + 1, 1, LIBRARY + 7, Blocks.DARK_OAK_PLANKS);
        b.set(CORRIDOR - 1, 2, LIBRARY + 7, b.candle(Blocks.YELLOW_CANDLE));
        b.set(CORRIDOR, HEIGHT - 2, LIBRARY + 3, lantern(true));
        b.set(CORRIDOR, HEIGHT - 2, DEPTH - 4, lantern(true));
        b.banner(1, 4, LIBRARY + 7, Direction.EAST, DyeColor.RED);
        b.banner(WIDTH - 2, 4, LIBRARY + 7, Direction.WEST, DyeColor.YELLOW);

        b.dragonborn(CORRIDOR, 1, HALL, ModEntityTypes.DRAGONBORN_TEMPLAR);
    }

    /** One room off the corridor: {@code side} -1 is the courtyard side, +1 the valley side. */
    private void room(Builder b, int side, int z0, boolean cell) {
        int door = CORRIDOR + side;
        int wall = side < 0 ? 1 : WIDTH - 2;
        int inside = side < 0 ? 2 : WIDTH - 3;
        b.air(door, 1, z0 + 1, door, 2, z0 + 1);
        if (cell) {
            b.bed(wall, 1, z0, Direction.NORTH, DyeColor.RED);
            b.chest(wall, 1, z0 + 2, side < 0 ? Direction.EAST : Direction.WEST, CELL);
            b.set(inside, 1, z0 + 2, b.candle(Blocks.RED_CANDLE));
        } else {
            b.container(wall, 1, z0, facing(Blocks.BARREL, Direction.UP), CELL);
            b.set(wall, 1, z0 + 2, Blocks.BARREL);
            b.set(inside, 1, z0 + 2, Blocks.COMPOSTER);
        }
        if (side > 0) {
            // A barred window out over the valley.
            b.set(WIDTH - 1, 2, z0 + 1, Blocks.IRON_BARS);
        }
    }
}
