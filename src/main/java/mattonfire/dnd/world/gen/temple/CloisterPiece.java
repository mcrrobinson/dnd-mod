package mattonfire.dnd.world.gen.temple;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.function.IntPredicate;
import mattonfire.dnd.classes.Registry.BlockFamily;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.ModTempleBlocks;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The monks' side of the courtyard, two buildings unlike the sanctum: whitewashed calcite between lacquered
 * timbers on a deepslate footing, under roofs of Storm Dragonscale Tiles.
 * <ul>
 * <li>The monks' hall, at the front: long and low under a gabled roof whose ridge runs back from the valley,
 * horns at both ends, its gable end with a lattice window over the valley. Inside, a cross-hall with the
 * door onto the courtyard, and a corridor back between six small rooms (4-6 of them monks' cells with a bed, a
 * chest and a red-lacquered floor, the rest storerooms), prayer wheels set into the corridor walls under paper
 * lanterns.</li>
 * <li>A covered walk back to the library tower: a three-storey pagoda, each storey smaller than the one below
 * under its own skirt of eaves, topped with a pyramid roof and a horned finial. Its ground floor holds the
 * temple's enchanting table in a ring of bookshelves; a ladder climbs to the reading room above, with lecterns,
 * shelves and candles.</li>
 * </ul>
 */
public class CloisterPiece extends TemplePiece {
    /** The footprint is a block wider each side than the walls, for the eaves. */
    static final int WIDTH = DragonTemplePlanner.SIDE_WIDTH + 2;
    static final int DEPTH = DragonTemplePlanner.DEPTH;
    static final int HEIGHT = 25;
    /** The walls' outer lines (local x). */
    private static final int WEST = 1;
    private static final int EAST = WIDTH - 2;
    private static final int CORRIDOR = WIDTH / 2;
    /** The monks' hall: its front and back walls, the cross-hall, and each room's first z (rooms are 3 deep). */
    private static final int HALL_FRONT = 1;
    private static final int HALL_BACK = 15;
    private static final int CROSS = 2;
    private static final int[] ROOMS = {4, 8, 12};
    private static final int HALL_EAVE = 6;
    /** The library tower's ground-floor walls, and its storeys' floors. */
    private static final int TOWER_FRONT = 18;
    private static final int TOWER_BACK = 26;
    private static final int TOWER_MID = (TOWER_FRONT + TOWER_BACK) / 2;
    private static final int STOREY_2 = 8;
    private static final int STOREY_3 = 13;

    public CloisterPiece(BlockPos front, Direction facing, long seed) {
        super(DragonTempleStructures.CLOISTER, boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, seed);
    }

    public CloisterPiece(NbtCompound nbt) {
        super(DragonTempleStructures.CLOISTER, nbt);
    }

    @Override
    protected void build(Builder b) {
        BlockFamily lacquer = ModBlockFamilies.TEMPLE_LACQUER_PLANKS;
        for (int x = WEST; x <= EAST; x++) {
            for (int z = HALL_FRONT; z <= TOWER_BACK; z++) {
                b.foundation(x, 0, z, Blocks.COBBLED_DEEPSLATE.getDefaultState());
            }
        }
        this.hall(b, lacquer);
        this.walk(b, lacquer);
        this.tower(b, lacquer);
    }

    /**
     * A timber-framed wall from (x1, z1) to (x2, z2), y {@code y0} to {@code top}: lacquered posts at its ends and
     * where {@code post} says (by x or z along it), a lacquered beam on top, a deepslate sill and calcite panels.
     */
    private static void frame(Builder b, int x1, int z1, int x2, int z2, int y0, int top, IntPredicate post) {
        Direction.Axis axis = x1 == x2 ? Direction.Axis.Z : Direction.Axis.X;
        int from = axis == Direction.Axis.Z ? Math.min(z1, z2) : Math.min(x1, x2);
        int to = axis == Direction.Axis.Z ? Math.max(z1, z2) : Math.max(x1, x2);
        BlockState beam = ModBlockFamilies.TEMPLE_LACQUER_PLANKS.base().getDefaultState();
        for (int along = from; along <= to; along++) {
            int x = axis == Direction.Axis.Z ? x1 : along;
            int z = axis == Direction.Axis.Z ? along : z1;
            boolean isPost = along == from || along == to || post.test(along);
            for (int y = y0; y <= top; y++) {
                b.set(x, y, z, isPost || y == top ? beam
                        : y == y0 ? Blocks.POLISHED_DEEPSLATE.getDefaultState() : Blocks.CALCITE.getDefaultState());
            }
        }
    }

    /** The four walls of a box from (x1, z1) to (x2, z2). */
    private static void frameBox(Builder b, int x1, int z1, int x2, int z2, int y0, int top, IntPredicate post) {
        frame(b, x1, z1, x1, z2, y0, top, post);
        frame(b, x2, z1, x2, z2, y0, top, post);
        frame(b, x1, z1, x2, z1, y0, top, post);
        frame(b, x1, z2, x2, z2, y0, top, post);
    }

    // --- The monks' hall -----------------------------------------------------------------------------------

    private void hall(Builder b, BlockFamily lacquer) {
        int top = HALL_EAVE - 1;
        for (int x = WEST; x <= EAST; x++) {
            for (int z = HALL_FRONT; z <= HALL_BACK; z++) {
                b.set(x, 0, z, x == CORRIDOR || z == CROSS ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.DEEPSLATE_TILES);
            }
        }
        frame(b, WEST, HALL_FRONT, WEST, HALL_BACK, 1, top, along -> along % 4 == 3);
        frame(b, EAST, HALL_FRONT, EAST, HALL_BACK, 1, top, along -> along % 4 == 3);
        frame(b, WEST, HALL_FRONT, EAST, HALL_FRONT, 1, top, along -> Math.abs(along - CORRIDOR) == 2);
        frame(b, WEST, HALL_BACK, EAST, HALL_BACK, 1, top, along -> Math.abs(along - CORRIDOR) == 2);
        // A flat lacquered ceiling under the roof.
        b.fill(WEST + 1, top, HALL_FRONT + 1, EAST - 1, top, HALL_BACK - 1, lacquer.base());

        // The rooms either side of the corridor, behind the cross-hall.
        for (int z = ROOMS[0] - 1; z < HALL_BACK; z++) {
            boolean crossWall = z == ROOMS[0] - 1 || z == ROOMS[1] - 1 || z == ROOMS[2] - 1;
            for (int x = WEST + 1; x < EAST; x++) {
                boolean inner = Math.abs(x - CORRIDOR) == 1;
                if (x != CORRIDOR && (inner || crossWall)) {
                    b.fill(x, 1, z, x, top - 1, z, inner && !crossWall ? Blocks.CALCITE : Blocks.POLISHED_DEEPSLATE);
                }
            }
        }
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
        // The cloister walk: prayer wheels in the corridor walls either side of each door, paper lanterns
        // overhead.
        for (int z0 : ROOMS) {
            for (int z : new int[]{z0, z0 + 2}) {
                b.set(CORRIDOR - 1, 1, z, ModTempleBlocks.PRAYER_WHEEL);
                b.set(CORRIDOR + 1, 1, z, ModTempleBlocks.PRAYER_WHEEL);
            }
            b.set(CORRIDOR, top - 1, z0 + 1, hangingPaperLantern());
        }
        b.set(CORRIDOR, top - 1, CROSS, hangingPaperLantern());

        // Doors onto the courtyard at the end of the cross-hall and out the back to the covered walk, and a
        // lattice window under the gable, looking out over the valley.
        b.air(WEST, 1, CROSS, WEST, 3, CROSS);
        b.set(WEST, 4, CROSS, Blocks.GILDED_BLACKSTONE);
        b.air(CORRIDOR, 1, HALL_BACK, CORRIDOR, 3, HALL_BACK);
        for (int x = CORRIDOR - 1; x <= CORRIDOR + 1; x++) {
            b.fill(x, 2, HALL_FRONT, x, 3, HALL_FRONT, lacquer.fence());
        }
        BlockFamily tiles = ModBlockFamilies.STORM_DRAGONSCALE_TILES;

        // The gabled roof, its ridge running front to back, overhanging the walls and gable ends by a block.
        this.gable(b, HALL_FRONT - 1, HALL_BACK + 1, HALL_EAVE, tiles);
        // The gable ends: calcite under a lacquered king post, an Ancestor Relief over the front window.
        for (int z : new int[]{HALL_FRONT, HALL_BACK}) {
            for (int x = WEST + 1; x < EAST; x++) {
                int peak = HALL_EAVE + Math.min(x - WEST, EAST - x) - 1;
                for (int y = HALL_EAVE; y <= peak; y++) {
                    b.set(x, y, z, x == CORRIDOR || y == HALL_EAVE ? lacquer.base() : Blocks.CALCITE);
                }
            }
        }
        b.set(CORRIDOR, HALL_EAVE + 1, HALL_FRONT, facing(ModTempleBlocks.ANCESTOR_RELIEF, FRONT));

        b.dragonborn(CORRIDOR, 1, CROSS, ModEntityTypes.DRAGONBORN_TEMPLAR);
    }

    /**
     * A gabled roof over the full width from z1 to z2, eaves at {@code y}: stairs rising to a ridge of double
     * slabs down the middle, a dark crest along the ridge and a horn at each end.
     */
    private void gable(Builder b, int z1, int z2, int y, BlockFamily tiles) {
        int[] rise = {0, 0, 1, 2, 3};
        BlockState ridge = tiles.slab().getDefaultState().with(SlabBlock.TYPE, SlabType.DOUBLE);
        for (int z = z1; z <= z2; z++) {
            for (int x = 0; x < WIDTH; x++) {
                int k = Math.min(x, WIDTH - 1 - x);
                if (k >= rise.length) {
                    b.set(x, y + rise[rise.length - 1] + 1, z, ridge);
                } else {
                    b.set(x, y + rise[k], z, stairs(tiles, x < CORRIDOR ? Direction.EAST : Direction.WEST, false));
                }
            }
        }
        int crest = y + rise[rise.length - 1] + 2;
        b.fill(CORRIDOR, crest, z1 + 1, CORRIDOR, crest, z2 - 1, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        b.set(CORRIDOR, crest, z1, facing(ModTempleBlocks.DRAGON_HORN, FRONT));
        b.set(CORRIDOR, crest, z2, facing(ModTempleBlocks.DRAGON_HORN, BACK));
    }

    /** One room off the corridor: {@code side} -1 is the courtyard side, +1 the valley side. */
    private void room(Builder b, int side, int z0, boolean cell) {
        int door = CORRIDOR + side;
        int wall = side < 0 ? WEST + 1 : EAST - 1;
        int inside = side < 0 ? WEST + 2 : EAST - 2;
        b.air(door, 1, z0 + 1, door, 2, z0 + 1);
        if (cell) {
            // A monk's cell: a red-lacquered floor.
            b.fill(Math.min(wall, inside), 0, z0, Math.max(wall, inside), 0, z0 + 2, ModBlockFamilies.TEMPLE_LACQUER_PLANKS.base());
            b.bed(wall, 1, z0, Direction.NORTH, DyeColor.RED);
            b.chest(wall, 1, z0 + 2, side < 0 ? Direction.EAST : Direction.WEST, CELL);
            b.set(inside, 1, z0 + 2, b.candle(Blocks.RED_CANDLE));
        } else {
            b.container(wall, 1, z0, facing(Blocks.BARREL, Direction.UP), CELL);
            b.set(wall, 1, z0 + 2, Blocks.BARREL);
            b.set(inside, 1, z0 + 2, Blocks.COMPOSTER);
        }
        // A lattice window in the outer wall.
        b.fill(side < 0 ? WEST : EAST, 2, z0 + 1, side < 0 ? WEST : EAST, 3, z0 + 1, ModBlockFamilies.TEMPLE_LACQUER_PLANKS.fence());
    }

    // --- The covered walk -----------------------------------------------------------------------------------

    private void walk(Builder b, BlockFamily lacquer) {
        for (int z = HALL_BACK + 1; z < TOWER_FRONT; z++) {
            b.set(CORRIDOR, 0, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            for (int x : new int[]{CORRIDOR - 1, CORRIDOR + 1}) {
                b.set(x, 0, z, Blocks.DEEPSLATE_TILES);
                b.fill(x, 1, z, x, 3, z, lacquer.fence());
                b.set(x, 4, z, stairs(lacquer, x < CORRIDOR ? Direction.EAST : Direction.WEST, false));
            }
            b.set(CORRIDOR, 4, z, lacquer.slab().getDefaultState().with(SlabBlock.TYPE, SlabType.DOUBLE));
        }
        b.set(CORRIDOR, 3, HALL_BACK + 2, hangingPaperLantern());
    }

    // --- The library tower ----------------------------------------------------------------------------------

    private void tower(Builder b, BlockFamily lacquer) {
        BlockFamily tiles = ModBlockFamilies.STORM_DRAGONSCALE_TILES;
        int mid = TOWER_MID;
        IntPredicate posts = along -> along == CORRIDOR || along == mid;
        for (int x = WEST; x <= EAST; x++) {
            for (int z = TOWER_FRONT; z <= TOWER_BACK; z++) {
                b.set(x, 0, z, (x + z) % 2 == 0 ? Blocks.DEEPSLATE_TILES : Blocks.POLISHED_DEEPSLATE);
            }
        }

        // Storey 1, the library: 9 by 9, a door from the walk, lattice windows either side of each face's post.
        frameBox(b, WEST, TOWER_FRONT, EAST, TOWER_BACK, 1, STOREY_2 - 2, posts);
        for (int d : new int[]{-2, 2}) {
            b.fill(CORRIDOR + d, 3, TOWER_FRONT, CORRIDOR + d, 4, TOWER_FRONT, lacquer.fence());
            b.fill(CORRIDOR + d, 3, TOWER_BACK, CORRIDOR + d, 4, TOWER_BACK, lacquer.fence());
            b.fill(WEST, 3, mid + d, WEST, 4, mid + d, lacquer.fence());
            b.fill(EAST, 3, mid + d, EAST, 4, mid + d, lacquer.fence());
        }
        b.air(CORRIDOR, 1, TOWER_FRONT, CORRIDOR, 3, TOWER_FRONT);
        b.set(CORRIDOR, 4, TOWER_FRONT, Blocks.GILDED_BLACKSTONE);
        // The enchanting table in a ring of bookshelves two blocks off, open towards the door, and shelves along
        // the back wall.
        int table = mid - 1;
        b.set(CORRIDOR, 1, table, Blocks.ENCHANTING_TABLE);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                if (ring && !(dz == -2 && Math.abs(dx) <= 1)) {
                    boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                    b.set(CORRIDOR + dx, 1, table + dz, Blocks.BOOKSHELF);
                    b.set(CORRIDOR + dx, 2, table + dz, corner ? b.candle(Blocks.WHITE_CANDLE) : Blocks.BOOKSHELF.getDefaultState());
                }
            }
        }
        b.fill(WEST + 1, 1, TOWER_BACK - 1, EAST - 3, 2, TOWER_BACK - 1, Blocks.BOOKSHELF);
        b.set(CORRIDOR, STOREY_2 - 2, table, hangingPaperLantern());
        // The floor of the reading room, with a ladder up through it on a post in the back corner.
        b.fill(WEST + 1, STOREY_2 - 1, TOWER_FRONT + 1, EAST - 1, STOREY_2 - 1, TOWER_BACK - 1, lacquer.base());
        b.fill(EAST - 2, 1, TOWER_BACK - 1, EAST - 2, STOREY_2 - 2, TOWER_BACK - 1, lacquer.base());
        for (int y = 1; y < STOREY_2; y++) {
            b.set(EAST - 2, y, TOWER_BACK - 2, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, FRONT));
        }
        hipRoof(b, WEST - 1, TOWER_FRONT - 1, EAST + 1, TOWER_BACK + 1, STOREY_2 - 1, new int[]{0, 0}, tiles, true);

        // Storey 2, the reading room: 7 by 7, lecterns back to back, a shelf and a candle.
        frameBox(b, WEST + 1, TOWER_FRONT + 1, EAST - 1, TOWER_BACK - 1, STOREY_2, STOREY_3 - 2, posts);
        for (int d : new int[]{-2, 2}) {
            b.set(CORRIDOR + d, STOREY_2 + 2, TOWER_FRONT + 1, lacquer.fence());
            b.set(CORRIDOR + d, STOREY_2 + 2, TOWER_BACK - 1, lacquer.fence());
            b.set(WEST + 1, STOREY_2 + 2, mid + d, lacquer.fence());
            b.set(EAST - 1, STOREY_2 + 2, mid + d, lacquer.fence());
        }
        b.set(CORRIDOR, STOREY_2, mid - 1, facing(Blocks.LECTERN, FRONT).with(LecternBlock.HAS_BOOK, false));
        b.set(CORRIDOR, STOREY_2, mid + 1, facing(Blocks.LECTERN, BACK).with(LecternBlock.HAS_BOOK, false));
        b.fill(WEST + 2, STOREY_2, mid - 1, WEST + 2, STOREY_2 + 1, mid + 1, Blocks.BOOKSHELF);
        b.set(WEST + 2, STOREY_2 + 2, mid, b.candle(Blocks.WHITE_CANDLE));
        b.set(CORRIDOR, STOREY_3 - 2, mid, hangingPaperLantern());
        b.fill(WEST + 2, STOREY_3 - 1, TOWER_FRONT + 2, EAST - 2, STOREY_3 - 1, TOWER_BACK - 2, lacquer.base());
        hipRoof(b, WEST, TOWER_FRONT, EAST, TOWER_BACK, STOREY_3 - 1, new int[]{0, 0}, tiles, true);

        // Storey 3: 5 by 5 and closed, under the pyramid roof and its horned finial.
        frameBox(b, WEST + 2, TOWER_FRONT + 2, EAST - 2, TOWER_BACK - 2, STOREY_3, STOREY_3 + 2, along -> along == CORRIDOR || along == mid);
        for (int d : new int[]{-1, 1}) {
            b.set(CORRIDOR + d, STOREY_3 + 1, TOWER_FRONT + 2, lacquer.fence());
            b.set(CORRIDOR + d, STOREY_3 + 1, TOWER_BACK - 2, lacquer.fence());
            b.set(WEST + 2, STOREY_3 + 1, mid + d, lacquer.fence());
            b.set(EAST - 2, STOREY_3 + 1, mid + d, lacquer.fence());
        }
        int roofY = STOREY_3 + 3;
        hipRoof(b, WEST + 1, TOWER_FRONT + 1, EAST - 1, TOWER_BACK - 1, roofY, new int[]{0, 0, 1, 2}, tiles, true);
        b.set(CORRIDOR, roofY + 3, mid, Blocks.GOLD_BLOCK);
        b.set(CORRIDOR, roofY + 4, mid, Blocks.POLISHED_BLACKSTONE_WALL);
        b.set(CORRIDOR, roofY + 5, mid, facing(ModTempleBlocks.DRAGON_HORN, Direction.UP));
        // Paper lanterns hung under the lowest eaves' corners.
        for (int x : new int[]{WEST - 1, EAST + 1}) {
            for (int z : new int[]{TOWER_FRONT - 1, TOWER_BACK + 1}) {
                b.set(x, STOREY_2 - 2, z, hangingPaperLantern());
            }
        }
    }

    private static BlockState hangingPaperLantern() {
        return ModTempleBlocks.PAPER_LANTERN.getDefaultState().with(LanternBlock.HANGING, true);
    }
}
