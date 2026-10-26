package mattonfire.dnd.world.underdark.drow;

import static mattonfire.dnd.world.gen.PieceBlocks.facing;
import static mattonfire.dnd.world.gen.PieceBlocks.stairs;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.boss.StructureBosses;
import mattonfire.dnd.entity.drow.Drow;
import mattonfire.dnd.world.gen.PieceBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlazedTerracottaBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The Drow Spire: a stalagmite rising from the cavern floor joined at its waist to a great stalactite hanging from the
 * dome, the whole column {@value #MAX_RADIUS} blocks round at its widest. Its front (the door) faces local +b.
 * <ul>
 * <li>Outside it's rough dripstone and deepslate patched with Drowstone Bricks; inside, a shell of Drowstone Bricks.
 * Arrow-slit windows on every level glow with Faerie Fire Lanterns.</li>
 * <li>A stair of Drowstone Brick stairs spirals round the middle (the towers' 16-cell ring, a block a step) from the
 * guard hall to the Sanctum, through a gap in each floor; the main way up is always open.</li>
 * <li><b>Guard hall</b> (ground, 12 high): the door with a curtain and a spider sigil before it, lanterns hanging from
 * the ceiling; 2 warriors and 2 Drow Elites.</li>
 * <li><b>Priestesses' cells</b> (floor {@value #CELLS}): three cells parted by Spidersilk Curtains, sleeping mats, a
 * chest ({@code chests/drow_spire_cells}); the web bridges from the minor towers come in here. 2 priestesses and 2
 * elites.</li>
 * <li><b>Library</b> (floor {@value #LIBRARY}): bookshelves round the wall and a lectern holding "Letters to the Mother"
 * (the quest chain's clue); 3 mages.</li>
 * <li><b>Matron's Sanctum</b> (floor {@value #SANCTUM}): a round room of radius 8, its floor half covered in spidersilk
 * and cobwebs at the edge, a spider-idol throne at the back with legs arching over it, and the Matron Mother
 * ({@link StructureBosses#placeMatron}) before it.</li>
 * <li><b>Treasure loft</b> (above the Sanctum): reached by a side stair along the Sanctum's front wall, whose way
 * through the ceiling is shut by a Hard Arcane Seal (Wizard/Warlock); a chest ({@code chests/drow_spire_loft}).</li>
 * </ul>
 */
public class DrowSpirePiece extends UnderdarkPiece {
    /** The cells' floor: the minor towers' platforms and bridges are at this height. */
    public static final int CELLS = 12;
    public static final int LIBRARY = 19;
    public static final int SANCTUM = 26;
    /** The Sanctum's ceiling and the loft's floor. */
    public static final int LOFT = 34;
    /** The loft's ceiling: solid stalactite above. */
    public static final int LOFT_TOP = 40;
    /** The stalactite runs up into the dome's shell. */
    public static final int TOP = 48;
    public static final double MAX_RADIUS = 9.5D;
    /** How thick the wall is: rooms reach this far less than the outside. */
    public static final double WALL = 1.6D;
    /** Blocks out from the middle to just outside the door. */
    public static final int DOOR_OUT = 11;
    static final int HALF = 12;
    /** The side stair to the loft runs along this row (local b), from a = {@value #LOFT_STAIR_A} down. */
    static final int LOFT_STAIR_B = 6;
    static final int LOFT_STAIR_A = 4;
    static final int LOFT_STEPS = 8;
    /** The arcane seal's group seed is the piece's seed xor this, the same in every chunk. */
    private static final long SEAL_SALT = 0x5EA15EA1L;

    public DrowSpirePiece(BlockPos center, Direction front, long seed) {
        super(DrowSpires.SPIRE, center, front, seed, -HALF, -2, -HALF, HALF, TOP, HALF);
    }

    public DrowSpirePiece(NbtCompound nbt) {
        super(DrowSpires.SPIRE, nbt);
    }

    /** The outside radius at height y (before the rough rock's noise). */
    public static double outer(int y) {
        if (y <= 10) {
            return 8.0D - 1.2D * Math.max(0, y) / 10.0D;
        }
        if (y <= SANCTUM) {
            return 6.8D + 2.7D * (y - 10) / 16.0D;
        }
        if (y <= 35) {
            return MAX_RADIUS;
        }
        return Math.max(2.5D, MAX_RADIUS - 7.0D * (y - 35) / 9.0D);
    }

    /** How far the room reaches at height y. */
    public static double inner(int y) {
        return outer(y) - WALL;
    }

    /** Whether height y is inside a room (not a floor, not the solid stalactite). */
    static boolean roomLevel(int y) {
        return y >= 0 && y < LOFT_TOP && y != CELLS && y != LIBRARY && y != SANCTUM && y != LOFT;
    }

    /** The floor block for floor y (-1, the cells, the library, the Sanctum or the loft). */
    private static BlockState floorAt(int y) {
        return y == LIBRARY || y == SANCTUM ? ModBlockFamilies.POLISHED_FAERZRESS.base().getDefaultState()
                : ModBlockFamilies.DROWSTONE_BRICKS.base().getDefaultState();
    }

    // ---- Where things are, in the world (commands and the survey) ----

    /** Where the Matron Mother stands, before her throne. */
    public BlockPos matronSpot() {
        return this.at(0, SANCTUM + 1, -3);
    }

    /** The middle of the Sanctum's floor, where you stand. */
    public BlockPos sanctumSpot() {
        return this.at(0, SANCTUM + 1, 3);
    }

    public BlockPos hallSpot() {
        return this.at(0, 0, 4);
    }

    public BlockPos cellsSpot() {
        return this.at(0, CELLS + 1, 4);
    }

    public BlockPos librarySpot() {
        return this.at(0, LIBRARY + 1, 3);
    }

    /** Just outside the door. */
    public BlockPos doorOutside() {
        return this.at(0, 0, DOOR_OUT);
    }

    /** The lectern holding "Letters to the Mother". */
    public BlockPos lectern() {
        return this.at(0, LIBRARY + 1, 4);
    }

    /** The loft's chest. */
    public BlockPos loftChest() {
        return this.at(0, LOFT + 1, -3);
    }

    /** Where you stand at the foot of the side stair, in the Sanctum. */
    public BlockPos loftStairFoot() {
        return this.at(LOFT_STAIR_A + 1, SANCTUM + 1, LOFT_STAIR_B - 1);
    }

    /** Where you stand in the loft, at the top of the side stair. */
    public BlockPos loftSpot() {
        return this.at(LOFT_STAIR_A - LOFT_STEPS, LOFT + 1, LOFT_STAIR_B - 1);
    }

    /** The Arcane Seal's blocks, in the ceiling over the side stair. */
    public List<BlockPos> seal() {
        List<BlockPos> seal = new ArrayList<>();
        for (int i = 4; i <= 6; i++) {
            seal.add(this.at(LOFT_STAIR_A - i, LOFT, LOFT_STAIR_B));
        }
        return seal;
    }

    /** The main stair's steps from the guard hall (step 0) to the Sanctum. */
    public List<BlockPos> steps() {
        List<BlockPos> steps = new ArrayList<>();
        for (int k = 0; k <= SANCTUM; k++) {
            int[] cell = DrowTowerPiece.stepCell(k);
            steps.add(this.at(cell[0], k, cell[1]));
        }
        return steps;
    }

    /** The side stair's steps, from the Sanctum's floor to the loft's. */
    public List<BlockPos> loftSteps() {
        List<BlockPos> steps = new ArrayList<>();
        for (int i = 0; i < LOFT_STEPS; i++) {
            steps.add(this.at(LOFT_STAIR_A - i, SANCTUM + 1 + i, LOFT_STAIR_B));
        }
        return steps;
    }

    @Override
    protected void build(Builder b) {
        BlockState bricks = ModBlockFamilies.DROWSTONE_BRICKS.base().getDefaultState();
        this.body(b, bricks);
        this.windows(b);
        this.door(b);
        this.stair(b, bricks);
        this.hall(b);
        this.cells(b);
        this.library(b);
        this.sanctum(b, bricks);
        this.loft(b);
    }

    /** The column: rooms cleared, floors laid, the brick shell and the rough outside. */
    private void body(Builder b, BlockState bricks) {
        for (int y = -1; y <= TOP; y++) {
            double out = outer(y);
            double in = out - WALL;
            boolean room = roomLevel(y);
            boolean floor = y == -1 || y == CELLS || y == LIBRARY || y == SANCTUM || y == LOFT;
            for (int a = -HALF; a <= HALF; a++) {
                for (int c = -HALF; c <= HALF; c++) {
                    double d = Math.sqrt(a * a + c * c);
                    double rough = out + noise(this.seed, a, y, c) * 0.9D - 0.2D;
                    if (d > rough) {
                        continue;
                    }
                    if (d <= in && room) {
                        b.clear(a, y, c);
                    } else if (d <= in && floor) {
                        b.set(a, y, c, floorAt(y));
                    } else if (d <= out - 0.5D && y < LOFT_TOP) {
                        b.set(a, y, c, bricks);
                    } else if (noise(this.seed ^ 0x2F, a, y, c) < 0.22F) {
                        b.set(a, y, c, bricks);
                    } else {
                        b.set(a, y, c, rock(b));
                    }
                }
            }
        }
        // The stalagmite's foot, propped down to the cavern floor.
        for (int a = -HALF; a <= HALF; a++) {
            for (int c = -HALF; c <= HALF; c++) {
                if (Math.sqrt(a * a + c * c) <= outer(0) + 0.4D) {
                    b.propUp(a, -1, c, Blocks.COBBLED_DEEPSLATE.getDefaultState(), 6, false);
                }
            }
        }
    }

    /** Arrow slits two high on every level, a Faerie Fire Lantern on each sill. */
    private void windows(Builder b) {
        int[][] rows = {{4, 45}, {15, 15}, {23, 45}, {29, 0}};
        for (int[] row : rows) {
            int y = row[0];
            for (int k = 0; k < 4; k++) {
                double angle = Math.toRadians(row[1] + 90 * k);
                if (row[1] == 0 && k == 0) {
                    continue; // the loft stair runs along the Sanctum's front wall
                }
                double sin = Math.sin(angle);
                double cos = Math.cos(angle);
                int[] first = null;
                for (double t = inner(y) - 0.5D; t <= outer(y) + 1.5D; t += 0.5D) {
                    int a = (int) Math.round(sin * t);
                    int c = (int) Math.round(cos * t);
                    if (first == null && Math.sqrt(a * a + c * c) > inner(y)) {
                        first = new int[] {a, c};
                    }
                    if (Math.sqrt(a * a + c * c) > inner(y)) {
                        b.clear(a, y, c);
                        b.clear(a, y + 1, c);
                    }
                }
                if (first != null) {
                    b.lantern(first[0], y, first[1], UnderdarkBlocks.FAERIE_LANTERN);
                }
            }
        }
    }

    /** The front door, three wide and three high, a curtain across it and a sigil inlaid before it. */
    private void door(Builder b) {
        for (int c = 3; c <= HALF; c++) {
            for (int a = -1; a <= 1; a++) {
                for (int y = 0; y <= 2; y++) {
                    if (Math.sqrt(a * a + c * c) > inner(y)) {
                        b.clear(a, y, c);
                    }
                }
            }
        }
        int wall = (int) Math.ceil(inner(0));
        for (int a = -1; a <= 1; a++) {
            for (int y = 0; y <= 2; y++) {
                b.set(a, y, wall, UnderdarkBlocks.SPIDERSILK_CURTAIN.getDefaultState());
            }
        }
        Direction[] turns = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int t = 0;
        for (int c = 4; c <= 5; c++) {
            for (int a = -1; a <= 1; a++) {
                b.set(a, -1, c, UnderdarkBlocks.SPIDER_SIGIL_STONE.getDefaultState()
                        .with(GlazedTerracottaBlock.FACING, turns[t++ % turns.length]));
            }
        }
    }

    /** The main stair round the middle, from the hall's floor up through the cells and the library to the Sanctum. */
    private void stair(Builder b, BlockState bricks) {
        Block stair = ModBlockFamilies.DROWSTONE_BRICKS.stairs();
        for (int k = 0; k <= SANCTUM; k++) {
            int[] cell = DrowTowerPiece.stepCell(k);
            int[] from = DrowTowerPiece.RING[k % DrowTowerPiece.RING.length];
            Direction climb = Direction.fromVector(cell[0] - from[0], 0, cell[1] - from[1]);
            b.set(cell[0], k, cell[1], stairs(stair, climb == null ? Direction.EAST : climb, false));
            if (k >= 1) {
                b.set(cell[0], k - 1, cell[1], bricks);
            }
        }
        // Gaps in each floor over the two steps below it.
        for (int f : new int[] {CELLS, LIBRARY, SANCTUM}) {
            for (int k = f - 2; k <= f - 1; k++) {
                int[] cell = DrowTowerPiece.stepCell(k);
                b.clear(cell[0], f, cell[1]);
            }
        }
    }

    /** A spot {@code r} blocks out at {@code degrees} round from the front (local +b), towards +a. */
    private static int[] polar(double degrees, double r) {
        double angle = Math.toRadians(degrees);
        return new int[] {(int) Math.round(Math.sin(angle) * r), (int) Math.round(Math.cos(angle) * r)};
    }

    private void hall(Builder b) {
        for (int[] spot : new int[][] {{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) {
            b.hangingLantern(spot[0], CELLS - 1, spot[1], 2, UnderdarkBlocks.FAERIE_LANTERN);
        }
        b.set(5, 0, -1, facing(Blocks.BARREL, Direction.UP));
        b.set(-5, 0, -1, facing(Blocks.BARREL, Direction.UP));
        b.set(4, 0, -3, facing(Blocks.BARREL, Direction.UP));
        b.resident(4, 0, 1, Drow.DROW_WARRIOR, 5);
        b.resident(-4, 0, 1, Drow.DROW_WARRIOR, 5);
        b.resident(2, 0, 4, Drow.DROW_ELITE, 5);
        b.resident(-2, 0, 4, Drow.DROW_ELITE, 5);
    }

    private void cells(Builder b) {
        int y = CELLS + 1;
        // Curtains part the floor into three cells, each with a bridge door (the bridges come in at 60, 180 and 300).
        for (double deg : new double[] {0, 120, 240}) {
            for (double r = 3.2D; r <= inner(y) + 0.2D; r += 0.5D) {
                int[] cell = polar(deg, r);
                for (int h = 0; h <= 2; h++) {
                    b.set(cell[0], y + h, cell[1], UnderdarkBlocks.SPIDERSILK_CURTAIN.getDefaultState());
                }
            }
        }
        for (double deg : new double[] {30, 150, 210, 330}) {
            int[] mat = polar(deg, 4.5D);
            b.set(mat[0], y, mat[1], UnderdarkBlocks.SPIDERSILK_CARPET);
            int[] mat2 = polar(deg + 12, 4.5D);
            b.set(mat2[0], y, mat2[1], UnderdarkBlocks.SPIDERSILK_CARPET);
        }
        int[] chest = polar(270, 4.6D);
        b.chest(chest[0], y, chest[1], Direction.EAST, DrowSpires.CELLS_LOOT);
        b.hangingLantern(0, LIBRARY - 1, 0, 1, UnderdarkBlocks.FAERIE_LANTERN);
        int[] p1 = polar(30, 3.6D);
        int[] p2 = polar(330, 3.6D);
        b.resident(p1[0], y, p1[1], Drow.DROW_PRIESTESS, 4);
        b.resident(p2[0], y, p2[1], Drow.DROW_PRIESTESS, 4);
        b.resident(4, y, 0, Drow.DROW_ELITE, 4);
        b.resident(-4, y, 0, Drow.DROW_ELITE, 4);
    }

    private void library(Builder b) {
        int y = LIBRARY + 1;
        // Shelves round the wall, three high.
        for (int a = -HALF; a <= HALF; a++) {
            for (int c = -HALF; c <= HALF; c++) {
                double d = Math.sqrt(a * a + c * c);
                for (int h = 0; h <= 2; h++) {
                    double in = inner(y + h);
                    if (d <= in && d > in - 1.0D && Math.max(Math.abs(a), Math.abs(c)) > 2) {
                        b.set(a, y + h, c, Blocks.BOOKSHELF);
                    }
                }
            }
        }
        b.set(0, y, 4, facing(Blocks.LECTERN, Direction.NORTH).with(LecternBlock.HAS_BOOK, true));
        BlockPos lectern = b.pos(0, y, 4);
        if (b.chunkBox.contains(lectern) && b.world.getBlockEntity(lectern) instanceof LecternBlockEntity be) {
            be.setBook(lettersToTheMother());
        }
        b.hangingLantern(0, SANCTUM - 1, 0, 2, UnderdarkBlocks.FAERIE_LANTERN);
        b.resident(4, y, -2, Drow.DROW_MAGE, 4);
        b.resident(-4, y, -2, Drow.DROW_MAGE, 4);
        b.resident(0, y, -4, Drow.DROW_MAGE, 4);
    }

    /**
     * "Letters to the Mother": letters from a drow agent above to the Matron, the clue the "Shadows Below" quest chain
     * reads, and its hint of something worse further down.
     */
    public static ItemStack lettersToTheMother() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString("title", "Letters to the Mother");
        nbt.putString("author", "Your Faithful Eyes Above");
        nbt.putBoolean("resolved", false);
        NbtList pages = new NbtList();
        for (int i = 1; i <= 4; i++) {
            pages.add(NbtString.of(Text.Serializer.toJson(
                    Text.translatable("book.dndclasses.letters_to_the_mother.page" + i))));
        }
        nbt.put("pages", pages);
        nbt.putBoolean(LETTERS_TAG, true);
        return book;
    }

    /** Set on the book, so a quest can recognise it. */
    public static final String LETTERS_TAG = "dndclasses_letters_to_the_mother";

    private void sanctum(Builder b, BlockState bricks) {
        int y = SANCTUM + 1;
        double in = inner(y);
        BlockState polished = ModBlockFamilies.POLISHED_FAERZRESS.base().getDefaultState();
        // The webbed floor: spidersilk over half of it, cobwebs round the edge.
        for (int a = -HALF; a <= HALF; a++) {
            for (int c = -HALF; c <= HALF; c++) {
                double d = Math.sqrt(a * a + c * c);
                if (d > in || Math.max(Math.abs(a), Math.abs(c)) <= 2 || this.onLoftStair(a, c)) {
                    continue;
                }
                float n = noise(this.seed ^ 0x51, a, y, c);
                if (d > in - 1.4D && n < 0.3F && c < 4) {
                    b.set(a, y, c, Blocks.COBWEB);
                } else if (n < 0.45F) {
                    b.set(a, y, c, UnderdarkBlocks.SPIDERSILK_CARPET);
                }
            }
        }
        // The throne at the back, a spider sigil over it, legs arching over from the wall.
        for (int a = -2; a <= 2; a++) {
            for (int c = -7; c <= -5; c++) {
                b.set(a, y, c, Blocks.AIR.getDefaultState());
            }
        }
        b.set(0, y, -6, stairs(ModBlockFamilies.POLISHED_FAERZRESS.stairs(), Direction.NORTH, false));
        b.set(1, y, -6, ModBlockFamilies.DROWSTONE_BRICKS.wall().getDefaultState());
        b.set(-1, y, -6, ModBlockFamilies.DROWSTONE_BRICKS.wall().getDefaultState());
        b.set(0, y, -7, polished);
        b.set(0, y + 1, -7, polished);
        b.set(0, y + 2, -7, UnderdarkBlocks.SPIDER_SIGIL_STONE.getDefaultState()
                .with(GlazedTerracottaBlock.FACING, Direction.SOUTH));
        b.set(-1, y + 2, -7, Blocks.CRYING_OBSIDIAN);
        b.set(1, y + 2, -7, Blocks.CRYING_OBSIDIAN);
        b.set(0, y + 3, -7, bricks);
        BlockState wall = ModBlockFamilies.DROWSTONE_BRICKS.wall().getDefaultState();
        for (int side : new int[] {-1, 1}) {
            for (int foot : new int[] {-6, -4, -2, 0}) {
                double knee = (foot - 7) / 2.0D;
                this.limb(b, wall, side, y + 3, -7, side * 3.5D, y + 6, knee);
                this.limb(b, wall, side * 3.5D, y + 6, knee, side * 6, y, foot);
            }
        }
        for (int[] spot : new int[][] {{4, 4}, {-4, 4}, {4, -4}, {-4, -4}}) {
            b.hangingLantern(spot[0], LOFT - 1, spot[1], 1, UnderdarkBlocks.FAERIE_LANTERN);
        }
        // The side stair to the loft, along the front wall; the Arcane Seal shuts its way through the ceiling.
        Block stair = ModBlockFamilies.DROWSTONE_BRICKS.stairs();
        for (int i = 0; i < LOFT_STEPS; i++) {
            int a = LOFT_STAIR_A - i;
            b.set(a, y + i, LOFT_STAIR_B, stairs(stair, Direction.WEST, false));
            for (int below = y; below < y + i; below++) {
                b.set(a, below, LOFT_STAIR_B, bricks);
            }
            for (int h = y + i + 1; h <= y + i + 2 && h < LOFT; h++) {
                b.clear(a, h, LOFT_STAIR_B);
            }
        }
        long group = this.seed ^ SEAL_SALT;
        for (int i = 4; i <= 6; i++) {
            ObstaclePlacer.place(b.world, b.chunkBox, b.pos(LOFT_STAIR_A - i, LOFT, LOFT_STAIR_B),
                    ObstacleTypes.LESSER_ARCANE_SEAL, Tier.HARD, false, group);
        }
        // The Matron Mother, before her throne, facing the stair.
        BlockPos matron = b.pos(0, y, -3);
        if (b.chunkBox.contains(matron)) {
            StructureBosses.placeMatron(b.world, matron, b.dir(Direction.SOUTH).asRotation());
        }
    }

    /** Whether local a/c is on the side stair's row. */
    private boolean onLoftStair(int a, int c) {
        return c == LOFT_STAIR_B && a <= LOFT_STAIR_A + 1 && a >= LOFT_STAIR_A - LOFT_STEPS;
    }

    /** A straight leg of walls from (a0, y0, c0) to (a1, y1, c1), left out where the throne is. */
    private void limb(Builder b, BlockState wall, double a0, double y0, double c0, double a1, double y1, double c1) {
        int steps = 12;
        for (int i = 0; i <= steps; i++) {
            double f = i / (double) steps;
            int a = (int) Math.round(a0 + (a1 - a0) * f);
            int y = (int) Math.round(y0 + (y1 - y0) * f);
            int c = (int) Math.round(c0 + (c1 - c0) * f);
            if (Math.abs(a) >= 2 && Math.sqrt(a * a + c * c) <= inner(y) + 0.5D && y < LOFT) {
                b.set(a, y, c, wall);
            }
        }
    }

    private void loft(Builder b) {
        int y = LOFT + 1;
        b.chest(0, y, -3, Direction.SOUTH, DrowSpires.LOFT_LOOT);
        b.set(2, y, -3, Blocks.GOLD_BLOCK);
        b.set(-2, y, -3, facing(Blocks.BARREL, Direction.UP));
        b.set(-2, y + 1, -3, facing(Blocks.BARREL, Direction.UP));
        b.set(3, y, -1, UnderdarkBlocks.SPIDERSILK_BLOCK);
        b.hangingLantern(0, LOFT_TOP - 1, 0, 1, UnderdarkBlocks.FAERIE_LANTERN);
        b.set(-3, y, 1, PieceBlocks.pillar(ModBlockFamilies.DROWSTONE_BRICKS, Direction.Axis.Y));
        b.lantern(-3, y + 1, 1, UnderdarkBlocks.FAERIE_LANTERN);
    }
}
