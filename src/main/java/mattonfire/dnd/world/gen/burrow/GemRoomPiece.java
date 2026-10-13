package mattonfire.dnd.world.gen.burrow;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The gem-polishing room south of the Commons: 13 by 15, 5 high, with a calcite and smooth basalt floor.
 * Polishing benches of grindstones and stonecutters line both side walls, end rods for lamps; down the middle,
 * calcite pedestals show off amethyst clusters and buds between panes of tinted glass. The gem chest (a DC 10
 * lock) stands at the far wall. A Tinker works here.
 */
public class GemRoomPiece extends CogsdeepPiece {
    public static final int HALF = 7;
    public static final int NEAR = CommonsPiece.HALF;
    public static final int FAR = 22;
    public static final int TOP = 5;

    public GemRoomPiece(BlockPos center, Direction door, long seed, long burrow) {
        super(CogsdeepStructures.GEM_ROOM, center, door, seed, burrow, -HALF, -1, NEAR, HALF, TOP, FAR);
    }

    public GemRoomPiece(NbtCompound nbt) {
        super(CogsdeepStructures.GEM_ROOM, nbt);
    }

    /** The gem chest, in the world. */
    public BlockPos gemChest() {
        return this.at(0, 0, FAR - 1);
    }

    @Override
    protected void build(Builder b) {
        b.room(-HALF, NEAR, HALF, FAR, TOP, Blocks.CALCITE);
        b.doorway(0, NEAR, true, 3, 3);
        for (int a = -HALF + 1; a < HALF; a++) {
            for (int c = NEAR + 1; c < FAR; c++) {
                if ((a + c) % 2 != 0) {
                    b.set(a, -1, c, Blocks.SMOOTH_BASALT);
                }
            }
        }
        // Polishing benches: grindstone, stonecutter, a smooth stone slab with an end rod lamp.
        for (int c = NEAR + 2; c <= FAR - 2; c += 3) {
            for (int wall : new int[]{-1, 1}) {
                int a = wall * (HALF - 1);
                Direction in = wall < 0 ? Direction.EAST : Direction.WEST;
                b.set(a, 0, c, Blocks.GRINDSTONE.getDefaultState().with(GrindstoneBlock.FACE, WallMountLocation.FLOOR)
                        .with(GrindstoneBlock.FACING, in));
                b.set(a, 0, c + 1, facing(Blocks.STONECUTTER, in));
                b.set(a, 0, c + 2, slab(Blocks.SMOOTH_STONE_SLAB, true));
                b.set(a, 1, c + 2, facing(Blocks.END_ROD, Direction.UP));
            }
        }
        // Pedestals of calcite with amethyst on show, between tinted glass.
        Block[] gems = {Blocks.AMETHYST_CLUSTER, Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.AMETHYST_CLUSTER};
        for (int c = NEAR + 4; c <= FAR - 4; c += 3) {
            for (int a = -2; a <= 2; a += 2) {
                b.set(a, 0, c, Blocks.CALCITE);
                b.set(a, 1, c, b.pick(gems).getDefaultState().with(AmethystClusterBlock.FACING, Direction.UP));
                if (a < 2) {
                    b.set(a + 1, 0, c, Blocks.TINTED_GLASS);
                }
            }
        }
        b.set(-3, 0, FAR - 1, Blocks.AMETHYST_BLOCK);
        b.set(3, 0, FAR - 1, Blocks.AMETHYST_BLOCK);
        b.container(0, 0, FAR - 1, facing(Blocks.CHEST, Direction.NORTH), GEM_LOOT);
        for (int c = NEAR + 3; c < FAR; c += 5) {
            b.hangingLantern(0, TOP - 1, c, 1);
        }
        b.tinker(3, 0, NEAR + 4, 6);
    }
}
