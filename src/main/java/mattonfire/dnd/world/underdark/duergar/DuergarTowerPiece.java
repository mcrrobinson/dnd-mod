package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.world.gen.PieceBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Duergar Hold's Taskmaster's tower, by the lava lake, its door towards local +b: 7 x 7, three floors and a roof,
 * Basalt Forge Brick walls with polished basalt corners, a ladder up one inside corner through every floor to a hatch
 * in the roof.
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
public class DuergarTowerPiece extends UnderdarkPiece {
    /** The shell's half-size: a and b from -3 to 3. */
    public static final int HALF = 3;
    /** The floors' y (the floor blocks): ground, first, top; then the roof. */
    public static final int FIRST = 4;
    public static final int TOP = 9;
    public static final int ROOF = 14;

    public DuergarTowerPiece(BlockPos center, Direction front, long seed) {
        super(DuergarHolds.TOWER, center, front, seed, -HALF, -1, -HALF, HALF, ROOF + 3, HALF);
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

    @Override
    protected void build(Builder b) {
        BlockState bricks = DuergarHolds.bricks();
        BlockState greysteel = DuergarHolds.greysteel();
        BlockState pillar = PieceBlocks.pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y);
        for (int a = -HALF; a <= HALF; a++) {
            for (int c = -HALF; c <= HALF; c++) {
                boolean wall = Math.abs(a) == HALF || Math.abs(c) == HALF;
                boolean corner = Math.abs(a) == HALF && Math.abs(c) == HALF;
                for (int y = -1; y <= ROOF; y++) {
                    if (wall) {
                        b.set(a, y, c, corner ? pillar : y == FIRST || y == TOP ? greysteel : bricks);
                    } else if (y == -1 || y == FIRST || y == TOP) {
                        b.set(a, y, c, y == -1 ? Blocks.DEEPSLATE_TILES.getDefaultState() : bricks);
                    } else if (y == ROOF) {
                        b.set(a, y, c, greysteel);
                    } else {
                        b.clear(a, y, c);
                    }
                }
                b.set(a, ROOF + 1, c, wall && Math.floorMod(a + c, 2) == 0 ? greysteel : Blocks.AIR.getDefaultState());
                b.clear(a, ROOF + 2, c);
                b.clear(a, ROOF + 3, c);
            }
        }
        // The door and the windows.
        b.clear(0, 0, HALF);
        b.clear(0, 1, HALF);
        for (int y : new int[] {FIRST + 2, TOP + 2}) {
            b.set(0, y, -HALF, Blocks.IRON_BARS);
            b.set(-HALF, y, 1, Blocks.IRON_BARS);
            b.set(HALF, y, 0, Blocks.IRON_BARS);
            b.set(1, y, HALF, Blocks.IRON_BARS);
        }
        for (int a = -1; a <= 1; a++) {
            b.set(a, TOP + 3, -HALF, Blocks.IRON_BARS);
        }
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
