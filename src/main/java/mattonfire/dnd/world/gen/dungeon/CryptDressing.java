package mattonfire.dnd.world.gen.dungeon;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.Direction;

/**
 * The Crypt's props: sarcophagi, lit candles, cobwebs, bone piles and skulls. Rooms call these
 * after building (only for {@link DungeonTheme#CRYPT}) with spots they know are clear of their
 * doors, chests, spawn points and traps. Everything is placed only into air ({@link DungeonPiece.Builder#setIfAir}),
 * and every draw comes from the builder's separate {@code dressing} random, so dressing never moves
 * a room's chests, traps or puzzle.
 */
final class CryptDressing {
    private CryptDressing() {
    }

    static boolean applies(DungeonPiece piece) {
        return piece.theme() == DungeonTheme.CRYPT;
    }

    /** Cobwebs in the room's four top corners, each with {@code chance}. */
    static void cornerCobwebs(DungeonPiece.Builder b, int width, int depth, int top, float chance) {
        for (int[] c : corners(width, depth)) {
            if (b.dressing.nextFloat() < chance) {
                b.setIfAir(c[0], top, c[1], Blocks.COBWEB.getDefaultState());
            }
        }
    }

    /** The four floor corners just inside the walls (ring 2): (2, 2), (w-3, 2), (2, d-3), (w-3, d-3). */
    static int[][] corners(int width, int depth) {
        return new int[][]{{2, 2}, {width - 3, 2}, {2, depth - 3}, {width - 3, depth - 3}};
    }

    /** 1-4 lit candles. */
    static void candles(DungeonPiece.Builder b, int x, int y, int z) {
        int count = 1 + b.dressing.nextInt(4);
        b.setIfAir(x, y, z, Blocks.CANDLE.getDefaultState().with(CandleBlock.CANDLES, count).with(CandleBlock.LIT, true));
    }

    /** A skeleton skull on the floor at a random angle. */
    static void skull(DungeonPiece.Builder b, int x, int y, int z) {
        b.setIfAir(x, y, z, Blocks.SKELETON_SKULL.getDefaultState().with(SkullBlock.ROTATION, b.dressing.nextInt(16)));
    }

    /** A heap of bones: a bone block on its side, sometimes a second on top, and a skull. */
    static void bonePile(DungeonPiece.Builder b, int x, int z) {
        Direction.Axis axis = b.dressing.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        BlockState bone = Blocks.BONE_BLOCK.getDefaultState().with(PillarBlock.AXIS, axis);
        if (!b.setIfAir(x, 1, z, bone)) {
            return;
        }
        if (b.dressing.nextFloat() < 0.35F) {
            b.setIfAir(x, 2, z, Blocks.BONE_BLOCK.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y));
            if (b.dressing.nextBoolean()) {
                skull(b, x, 3, z);
            }
        } else if (b.dressing.nextFloat() < 0.6F) {
            skull(b, x, 2, z);
        }
    }

    /**
     * A stone coffin two blocks long, from local (x, z) running {@code along}: a polished deepslate
     * body under a deepslate-tile lid, now and then with candles at its head (the block before it) or,
     * with {@code candlesAtFoot}, its foot (the block past its end).
     */
    static void sarcophagus(DungeonPiece.Builder b, int x, int z, Direction along, boolean candlesAtFoot) {
        BlockState body = Blocks.POLISHED_DEEPSLATE.getDefaultState();
        BlockState lid = Blocks.DEEPSLATE_TILE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int i = 0; i < 2; i++) {
            int bx = x + along.getOffsetX() * i;
            int bz = z + along.getOffsetZ() * i;
            if (b.setIfAir(bx, 1, bz, body)) {
                b.setIfAir(bx, 2, bz, lid);
            }
        }
        if (b.dressing.nextFloat() < 0.5F) {
            int step = candlesAtFoot ? 2 : -1;
            candles(b, x + along.getOffsetX() * step, 1, z + along.getOffsetZ() * step);
        }
    }

    /**
     * Two sarcophagi along each wall of a room, either side of the middle (where doors are), at
     * {@code from}-{@code from+1} blocks from each end, each with {@code chance}. Candles go on the
     * corner side, away from the doorway.
     */
    static void wallSarcophagi(DungeonPiece.Builder b, int width, int depth, int from, float chance) {
        for (Direction wall : Direction.Type.HORIZONTAL) {
            boolean alongX = wall.getAxis() == Direction.Axis.Z;
            int length = alongX ? width : depth;
            int fixed = switch (wall) {
                case NORTH, WEST -> 2;
                case SOUTH -> depth - 3;
                default -> width - 3;
            };
            for (int start : new int[]{from, length - 2 - from}) {
                if (b.dressing.nextFloat() >= chance) {
                    continue;
                }
                Direction along = alongX ? Direction.EAST : Direction.SOUTH;
                int x = alongX ? start : fixed;
                int z = alongX ? fixed : start;
                sarcophagus(b, x, z, along, start != from);
            }
        }
    }
}
