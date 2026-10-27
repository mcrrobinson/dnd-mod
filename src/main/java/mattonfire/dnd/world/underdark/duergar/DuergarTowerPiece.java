package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Duergar Hold's Taskmaster's tower, by the lava lake, its door towards local +b: 7 x 7 with its corners cut to
 * polished basalt chamfers, three floors and a roof, Basalt Forge Brick walls on a blackstone base with Greysteel
 * bands at each floor. Its foot is battered (a skirt of blackstone two high, stepped in), its top overhangs on
 * corbels as a crown of battlements with pinnacles (braziers on some), and most towers have a balcony off the top
 * floor towards the lake with a gibbet cage hanging under it. A ladder climbs one inside corner through every floor to
 * a hatch in the roof; the door is recessed in a portal.
 * <ul>
 * <li>Ground floor: the guard room (barrels, a brazier).</li>
 * <li>First floor: the <b>strongbox</b> vault, a 2 x 2 closet in one corner. Its doorway is a <b>Locked Door</b>
 * obstacle (Hard, DC 15: a Rogue picks it; anyone can tear it down, loudly), and behind it the strongbox chest
 * ({@code chests/duergar_strongbox}, lock DC 15).</li>
 * <li>Top floor: the Taskmaster's quarters (bed, brazier, windows on every side over the hold and the lava). The
 * Taskmaster lives here.</li>
 * <li>Roof: Greysteel battlements, and half the time an arbalester.</li>
 * </ul>
 */
public class DuergarTowerPiece extends DuergarPiece {
    /** The shell's half-size: a and b from -3 to 3. */
    public static final int HALF = 3;
    /** The floors' y (the floor blocks): ground, first, top; then the roof. */
    public static final int FIRST = 4;
    public static final int TOP = 9;
    public static final int ROOF = 14;

    /** Blocks from the centre to just outside the door. */
    public static final int DOOR = HALF + 2;

    public DuergarTowerPiece(BlockPos center, Direction front, long seed, boolean mirror, int style) {
        super(DuergarHolds.TOWER, center, front, seed, mirror, style, -HALF - 2, -1, -HALF - 1, HALF + 1, ROOF + 3, HALF + 2);
    }

    public DuergarTowerPiece(NbtCompound nbt) {
        super(DuergarHolds.TOWER, nbt);
    }

    /** The vault's Locked Door (its lower block). */
    public BlockPos vaultDoor() {
        return this.at(-1, FIRST + 1, 0);
    }

    /** Where you stand in front of the vault door. */
    public BlockPos atVault() {
        return this.at(-1, FIRST + 1, 2);
    }

    /** The strongbox chest. */
    public BlockPos strongbox() {
        return this.at(-2, FIRST + 1, -2);
    }

    /** The Taskmaster's quarters. */
    public BlockPos quarters() {
        return this.at(0, TOP + 1, 0);
    }

    /** Just inside the door. */
    public BlockPos inside() {
        return this.at(0, 0, 1);
    }

    /** Whether local a/b is inside the tower's shell: 7 x 7 with the corner blocks cut. */
    static boolean in(int a, int c) {
        return Math.abs(a) <= HALF && Math.abs(c) <= HALF && !(Math.abs(a) == HALF && Math.abs(c) == HALF);
    }

    /** Whether local a/b is just outside the shell (beside or diagonal to it): the batter and the crown stand there. */
    static boolean ring(int a, int c) {
        if (in(a, c)) {
            return false;
        }
        for (int da = -1; da <= 1; da++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (in(a + da, c + dc)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The way from a ring cell in towards the shell. */
    private static Direction inward(int a, int c) {
        if (Math.abs(a) > Math.abs(c)) {
            return a > 0 ? Direction.WEST : Direction.EAST;
        }
        return c > 0 ? Direction.NORTH : Direction.SOUTH;
    }

    @Override
    protected void build(Builder b) {
        BlockState bricks = DuergarHolds.bricks();
        BlockState greysteel = DuergarHolds.greysteel();
        BlockState pillar = DuergarMasonry.pillar();
        boolean balcony = b.random.nextInt(4) != 0;
        boolean pinnacles = b.random.nextBoolean();
        int span = HALF + 1;
        for (int a = -span; a <= span; a++) {
            for (int c = -span; c <= span; c++) {
                if (in(a, c)) {
                    boolean wall = Math.abs(a) == HALF || Math.abs(c) == HALF;
                    boolean corner = Math.abs(a) >= HALF - 1 && Math.abs(c) >= HALF - 1 && wall;
                    for (int y = -1; y <= ROOF; y++) {
                        if (wall) {
                            b.set(a, y, c, y <= 1 ? DuergarMasonry.base() : y == FIRST || y == TOP || y == ROOF ? greysteel
                                    : corner ? pillar : bricks);
                        } else if (y == -1 || y == FIRST || y == TOP) {
                            b.set(a, y, c, y == -1 ? Blocks.DEEPSLATE_TILES.getDefaultState() : bricks);
                        } else if (y == ROOF) {
                            b.set(a, y, c, greysteel);
                        } else {
                            b.clear(a, y, c);
                        }
                    }
                    DuergarMasonry.openAir(b, a, ROOF + 1, c);
                } else if (ring(a, c)) {
                    // The battered foot and the overhanging crown.
                    Direction in = inward(a, c);
                    b.set(a, -1, c, DuergarMasonry.base());
                    b.set(a, 0, c, DuergarMasonry.base());
                    b.set(a, 1, c, DuergarMasonry.baseStairs(in, false));
                    b.set(a, ROOF - 2, c, DuergarMasonry.baseStairs(in, true));
                    b.set(a, ROOF - 1, c, bricks);
                    b.set(a, ROOF, c, greysteel);
                    boolean notch = Math.abs(a) == HALF && Math.abs(c) == HALF;
                    if (notch) {
                        b.set(a, ROOF + 1, c, greysteel);
                        b.set(a, ROOF + 2, c, pinnacles ? DuergarHolds.brazier() : DuergarMasonry.brickWall());
                    } else {
                        DuergarMasonry.merlon(b, a, ROOF + 1, c, Math.floorMod(a + c, 2) == 0, greysteel);
                    }
                }
                DuergarMasonry.openAir(b, a, ROOF + 2, c);
                DuergarMasonry.openAir(b, a, ROOF + 3, c);
            }
        }
        // The door, recessed in a portal, and the windows: shuttered on the first floor, slits on the top floor.
        DuergarMasonry.portal(b, 0, 0, 2, HALF);
        b.set(0, FIRST + 2, -HALF, Blocks.IRON_BARS);
        b.set(HALF, FIRST + 2, 0, Blocks.IRON_BARS);
        b.set(HALF, FIRST + 3, 0, Blocks.IRON_BARS);
        b.set(-HALF, FIRST + 2, 1, Blocks.IRON_BARS);
        b.set(1, FIRST + 2, HALF, Blocks.IRON_BARS);
        b.set(HALF + 1, FIRST + 2, -1, DuergarMasonry.shutter(Direction.EAST));
        b.set(HALF + 1, FIRST + 3, -1, DuergarMasonry.shutter(Direction.EAST));
        b.set(HALF + 1, FIRST + 2, 1, DuergarMasonry.shutter(Direction.EAST));
        b.set(HALF + 1, FIRST + 3, 1, DuergarMasonry.shutter(Direction.EAST));
        for (int y = TOP + 1; y <= TOP + 2; y++) {
            b.set(0, y, -HALF, Blocks.IRON_BARS);
            b.set(HALF, y, 0, Blocks.IRON_BARS);
            b.set(1, y, HALF, Blocks.IRON_BARS);
            b.set(-1, y, HALF, Blocks.IRON_BARS);
        }
        b.set(-1, TOP + 2, -HALF, Blocks.IRON_BARS);
        b.set(1, TOP + 2, -HALF, Blocks.IRON_BARS);
        if (balcony) {
            // A balcony off the Taskmaster's floor towards the lake, a gibbet cage hanging under it.
            for (int c = -1; c <= 1; c++) {
                for (int a = -HALF - 2; a <= -HALF - 1; a++) {
                    b.set(a, TOP, c, DuergarMasonry.base());
                    b.set(a, TOP - 1, c, DuergarMasonry.baseStairs(Direction.EAST, true));
                    b.clear(a, TOP + 1, c);
                    b.clear(a, TOP + 2, c);
                }
                b.set(-HALF - 2, TOP + 1, c, DuergarMasonry.brickWall());
            }
            for (int a = -HALF - 2; a <= -HALF - 1; a++) {
                b.set(a, TOP + 1, -2, DuergarMasonry.brickWall());
                b.set(a, TOP + 1, 2, DuergarMasonry.brickWall());
            }
            b.clear(-HALF, TOP + 1, 0);
            b.clear(-HALF, TOP + 2, 0);
            b.set(-HALF - 2, TOP - 2, 1, Blocks.CHAIN.getDefaultState());
            b.set(-HALF - 2, TOP - 3, 1, UnderdarkBlocks.CHAIN_CAGE.getDefaultState());
            b.set(-HALF - 2, TOP + 2, -1, Blocks.SOUL_LANTERN.getDefaultState());
        } else {
            b.set(-HALF, TOP + 1, 0, Blocks.IRON_BARS);
            b.set(-HALF, TOP + 2, 0, Blocks.IRON_BARS);
        }
        // A brazier on a post beside the door.
        b.set(-2, 0, HALF + 2, DuergarHolds.brazier());
        // The ladder up the back corner, through every floor and the roof.
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH);
        for (int y = 0; y <= ROOF; y++) {
            b.set(2, y, -2, ladder);
        }
        // Ground floor: the guard room.
        b.set(-2, 0, -2, PieceBlocks.facing(Blocks.BARREL, Direction.UP));
        b.set(-2, 1, -2, PieceBlocks.facing(Blocks.BARREL, Direction.UP));
        b.set(-2, 0, -1, PieceBlocks.facing(Blocks.BARREL, Direction.UP));
        b.set(-2, 0, 2, DuergarHolds.brazier());
        b.hangingLantern(0, FIRST - 1, 0, 1, Blocks.SOUL_LANTERN);
        // First floor: the vault in the back left corner, behind a Locked Door.
        for (int y = FIRST + 1; y < TOP; y++) {
            b.set(0, y, -2, bricks);
            b.set(0, y, -1, bricks);
            b.set(0, y, 0, bricks);
            b.set(-2, y, 0, bricks);
            b.set(-1, y, 0, bricks);
        }
        b.chest(-2, FIRST + 1, -2, Direction.SOUTH, DuergarHolds.STRONGBOX_LOOT);
        b.set(-1, FIRST + 1, -2, Blocks.GOLD_BLOCK);
        long group = b.random.nextLong();
        for (int y = FIRST + 1; y <= FIRST + 2; y++) {
            ObstaclePlacer.place(b.world, b.chunkBox, b.pos(-1, y, 0), ObstacleTypes.LOCKED_DOOR, Tier.HARD, false, group);
        }
        b.hangingLantern(1, TOP - 1, 1, 1, Blocks.SOUL_LANTERN);
        // Top floor: the Taskmaster's quarters.
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH);
        b.set(-2, TOP + 1, 1, bed.with(BedBlock.PART, BedPart.FOOT));
        b.set(-2, TOP + 1, 2, bed.with(BedBlock.PART, BedPart.HEAD));
        b.set(2, TOP + 1, 2, DuergarHolds.brazier());
        b.set(-2, TOP + 1, -2, PieceBlocks.facing(Blocks.BARREL, Direction.UP));
        b.set(0, TOP + 1, 2, mattonfire.dnd.classes.Registry.ModBlockFamilies.GREYSTEEL_PLATE.slab().getDefaultState()
                .with(net.minecraft.block.SlabBlock.TYPE, net.minecraft.block.enums.SlabType.TOP));
        b.hangingLantern(0, ROOF - 1, -1, 1, Blocks.SOUL_LANTERN);
        DuergarHolds.duergar(b, 0, TOP + 1, 0, DuergarEntities.DUERGAR_TASKMASTER, 3);
        if (b.random.nextBoolean()) {
            DuergarHolds.duergar(b, 0, ROOF + 1, 0, DuergarEntities.DUERGAR_ARBALESTER, 2);
        }
    }
}
