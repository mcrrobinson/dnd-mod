package mattonfire.dnd.world.gen.burrow;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The Commons at the foot of the spiral stair, Cogsdeep's hearth: a 15 by 15 hall, {@link #TOP} high, the last
 * turn of the stair winding down round the pillar in the middle. A brick fireplace with a campfire on the north
 * wall (its flue stops under the ceiling, so the shell stays sealed), two bunks and bookshelves on the west, a
 * long table with stools, the Tinker's counter of barrels and a lectern in the south-east, the burrow's chest,
 * and lanterns on chains. Doorways lead east and west to the
 * workshops, north to the clocktower and south to the gem room. A Tinker and a Clockwork Sentinel live here.
 */
public class CommonsPiece extends CogsdeepPiece {
    /** Half the hall's outer width (walls at +-8). */
    public static final int HALF = 8;
    /** The ceiling's height: the hall is 6 high inside. */
    public static final int TOP = 6;

    public CommonsPiece(BlockPos center, Direction door, long seed, long burrow) {
        super(CogsdeepStructures.COMMONS, center, door, seed, burrow, -HALF, -1, -HALF, HALF, TOP, HALF);
    }

    public CommonsPiece(NbtCompound nbt) {
        super(CogsdeepStructures.COMMONS, nbt);
    }

    /** Where the Tinker of the Commons keeps shop, in the world. */
    public BlockPos counter() {
        return this.at(5, 0, 4);
    }

    /** The fireplace's campfire, in the world. */
    public BlockPos fireplace() {
        return this.at(-4, 0, -7);
    }

    @Override
    protected void build(Builder b) {
        b.room(-HALF, -HALF, HALF, HALF, TOP, Blocks.SPRUCE_PLANKS);
        // A ring of polished andesite round the stair's foot, copper inlay at its edge.
        for (int a = -HALF + 1; a <= HALF - 1; a++) {
            for (int c = -HALF + 1; c <= HALF - 1; c++) {
                int ring = Math.max(Math.abs(a), Math.abs(c));
                if (ring == 3) {
                    b.set(a, -1, c, Blocks.POLISHED_ANDESITE);
                } else if (ring == 4 && (a + c) % 2 == 0) {
                    b.set(a, -1, c, Blocks.CUT_COPPER);
                }
            }
        }
        b.doorway(HALF, 0, false, 3, 4);
        b.doorway(-HALF, 0, false, 3, 4);
        b.doorway(0, -HALF, true, 3, 4);
        b.doorway(0, HALF, true, 3, 4);

        // The fireplace: a campfire in a brick alcove on the north wall, its flue rising to the ceiling.
        b.fill(-6, 0, -7, -6, 3, -7, Blocks.BRICKS);
        b.fill(-2, 0, -7, -2, 3, -7, Blocks.BRICKS);
        b.fill(-6, 4, -7, -2, 5, -7, Blocks.BRICKS);
        b.fill(-5, 3, -7, -3, 3, -7, Blocks.BRICKS);
        b.set(-4, 3, -7, Blocks.AIR);
        b.set(-4, 4, -7, Blocks.AIR);
        b.set(-4, 5, -7, Blocks.AIR);
        b.set(-4, 0, -7, Blocks.CAMPFIRE);
        b.set(-5, 0, -7, Blocks.BRICK_SLAB);
        b.set(-3, 0, -7, Blocks.BRICK_SLAB);

        // Bunks and bookshelves on the west wall.
        bed(b, -7, -5);
        bed(b, -7, -3);
        b.fill(-7, 0, 3, -7, 2, 6, Blocks.BOOKSHELF);
        b.set(-7, 3, 4, Blocks.LANTERN);

        // A long table with stools by the east doorway's north side.
        for (int a = 3; a <= 6; a++) {
            b.set(a, 0, -5, slab(Blocks.SPRUCE_SLAB, true));
            b.set(a, 0, -6, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            b.set(a, 0, -4, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
        }
        b.set(4, 1, -5, Blocks.LANTERN);
        b.set(6, 1, -5, Blocks.FLOWER_POT);

        // The Tinker's counter: barrels and a lectern facing into the hall.
        b.set(4, 0, 6, facing(Blocks.BARREL, Direction.UP));
        b.set(5, 0, 6, facing(Blocks.BARREL, Direction.UP));
        b.set(6, 0, 6, facing(Blocks.LECTERN, Direction.NORTH));
        b.set(6, 0, 5, Blocks.CRAFTING_TABLE);
        b.set(6, 0, 4, Blocks.SMITHING_TABLE);
        b.set(4, 1, 6, Blocks.LANTERN);

        b.container(-7, 0, 6, facing(Blocks.CHEST, Direction.EAST), COMMONS_LOOT);

        for (int[] spot : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
            b.hangingLantern(spot[0], TOP - 1, spot[1], 1);
        }

        b.tinker(5, 0, 4, 6);
        b.sentinel(-5, 0, 2, 10);
    }

    /** A bed along the west wall, its head against it. */
    private static void bed(Builder b, int a, int c) {
        BlockState head = Blocks.BROWN_BED.getDefaultState().with(BedBlock.FACING, Direction.WEST).with(BedBlock.PART, BedPart.HEAD);
        b.set(a, 0, c, head);
        b.set(a + 1, 0, c, head.with(BedBlock.PART, BedPart.FOOT));
    }
}
