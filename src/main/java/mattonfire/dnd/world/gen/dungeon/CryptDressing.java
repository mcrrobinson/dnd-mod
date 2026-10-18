package mattonfire.dnd.world.gen.dungeon;

import mattonfire.dnd.classes.Registry.CryptBlocks;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.util.math.Direction;

/**
 * The Crypt's props: sarcophagi, lit candles and grave candle clusters, cobwebs, bone piles, skulls, grave
 * moss, funerary reliefs, burial niches and the Lich's sigils. Rooms call these
 * after building (only for {@link DungeonTheme#CRYPT}) with spots they know are clear of their
 * doors, chests, spawn points and traps. Everything is placed only into air ({@link DungeonPiece.Builder#setIfAir}),
 * and every draw comes from the builder's separate {@code dressing} random, so dressing never moves
 * a room's chests, traps or puzzle.
 */
final class CryptDressing {
    /** The share of dressing sarcophagi that hold an ambush. */
    static final float OCCUPIED_CHANCE = 0.35F;

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
     * A {@link CryptBlocks#SARCOPHAGUS} two blocks long, from local (x, z) (its head) running {@code along}
     * to its foot. {@value #OCCUPIED_CHANCE} of them hold an ambush (a zombie climbs out when one is opened).
     * Half the time there are candles or a grave candle cluster at its head (the block before it) or, with
     * {@code candlesAtFoot}, its foot (the block past its end).
     */
    static void sarcophagus(DungeonPiece.Builder b, int x, int z, Direction along, boolean candlesAtFoot) {
        boolean occupied = b.dressing.nextFloat() < OCCUPIED_CHANCE;
        Direction toHead = along.getOpposite();
        b.setIfAir(x, 1, z, CryptBlocks.SARCOPHAGUS.half(toHead, BedPart.HEAD, occupied));
        b.setIfAir(x + along.getOffsetX(), 1, z + along.getOffsetZ(), CryptBlocks.SARCOPHAGUS.half(toHead, BedPart.FOOT, occupied));
        if (b.dressing.nextFloat() < 0.5F) {
            int step = candlesAtFoot ? 2 : -1;
            int cx = x + along.getOffsetX() * step;
            int cz = z + along.getOffsetZ() * step;
            if (b.dressing.nextFloat() < 0.4F) {
                candleCluster(b, cx, 1, cz);
            } else {
                candles(b, cx, 1, cz);
            }
        }
    }

    /** A {@link CryptBlocks#GRAVE_CANDLE_CLUSTER}: a puddle of wax with burning stubs (light 6). */
    static void candleCluster(DungeonPiece.Builder b, int x, int y, int z) {
        b.setIfAir(x, y, z, CryptBlocks.GRAVE_CANDLE_CLUSTER.getDefaultState());
    }

    /**
     * Up to {@code count} patches of Grave Moss carpet on the floor (ring 2 inwards), each a few blocks
     * scattered round a random spot; only into air.
     */
    static void mossPatches(DungeonPiece.Builder b, int width, int depth, int count) {
        for (int i = 0; i < count; i++) {
            int cx = 2 + b.dressing.nextInt(width - 4);
            int cz = 2 + b.dressing.nextInt(depth - 4);
            int size = 2 + b.dressing.nextInt(4);
            for (int j = 0; j < size; j++) {
                int x = Math.max(2, Math.min(width - 3, cx + b.dressing.nextInt(3) - 1));
                int z = Math.max(2, Math.min(depth - 3, cz + b.dressing.nextInt(3) - 1));
                b.setIfAir(x, 1, z, CryptBlocks.GRAVE_MOSS_CARPET.getDefaultState());
            }
        }
    }

    /**
     * Funerary Reliefs set into the inner wall (ring 1) at height {@code y}, facing into the room, at
     * {@code offset} blocks either side of the middle of each wall, each with {@code chance}. They only
     * replace plain crypt wall ({@link #isPlainWall}), so they never cut into a doorway's trim, a mural or a
     * seal; doorways are a cell's middle, framed 2 blocks either side of it, so keep {@code offset} past 2
     * from any doorway.
     */
    static void wallReliefs(DungeonPiece.Builder b, int width, int depth, int y, int[] offsets, float chance) {
        for (Direction wall : Direction.Type.HORIZONTAL) {
            for (int offset : offsets) {
                if (b.dressing.nextFloat() >= chance) {
                    continue;
                }
                int x;
                int z;
                switch (wall) {
                    case NORTH -> {
                        x = width / 2 + offset;
                        z = 1;
                    }
                    case SOUTH -> {
                        x = width / 2 + offset;
                        z = depth - 2;
                    }
                    case WEST -> {
                        x = 1;
                        z = depth / 2 + offset;
                    }
                    default -> {
                        x = width - 2;
                        z = depth / 2 + offset;
                    }
                }
                setIfPlainWall(b, x, y, z, CryptBlocks.FUNERARY_RELIEF.facing(wall.getOpposite()));
            }
        }
    }

    /** Sets the block only over plain crypt wall (see {@link #isPlainWall}). */
    static boolean setIfPlainWall(DungeonPiece.Builder b, int x, int y, int z, BlockState state) {
        BlockState now = b.get(x, y, z);
        if (now == null || !isPlainWall(now)) {
            return false;
        }
        b.set(x, y, z, state);
        return true;
    }

    /** The blocks the Crypt's {@link DungeonTheme.Palette#wall} draws from. */
    static boolean isPlainWall(BlockState state) {
        return state.isOf(Blocks.STONE_BRICKS) || state.isOf(Blocks.MOSSY_STONE_BRICKS)
                || state.isOf(Blocks.CRACKED_STONE_BRICKS) || state.isOf(ModBlockFamilies.OSSUARY_BRICKS.base())
                || state.isOf(CryptBlocks.GRAVE_MOSS);
    }

    /**
     * Burial Niche Walls along a corridor's side walls (local {@code lateral} 2 from its middle line), at
     * head height (y 2), every third block centred along it (most corridors are only 1-4 blocks long, the
     * gap between two rooms' walls), each with {@code chance}, facing in.
     */
    static void corridorNiches(DungeonPiece.Builder b, Direction.Axis axis, int length, float chance) {
        for (int i = ((length - 1) % 3) / 2; i < length; i += 3) {
            for (int side : new int[]{1, 5}) {
                if (b.dressing.nextFloat() >= chance) {
                    continue;
                }
                Direction front = axis == Direction.Axis.X
                        ? (side == 1 ? Direction.SOUTH : Direction.NORTH)
                        : (side == 1 ? Direction.EAST : Direction.WEST);
                int x = axis == Direction.Axis.X ? i : side;
                int z = axis == Direction.Axis.X ? side : i;
                setIfPlainWall(b, x, 2, z, CryptBlocks.BURIAL_NICHE_WALL.facing(front));
            }
        }
    }

    /**
     * Lich Sigil Floor in a ring round local (cx, cz) on the floor (y 0), between {@code inner} and
     * {@code outer} blocks out.
     */
    static void sigilRing(DungeonPiece.Builder b, int cx, int cz, double inner, double outer) {
        int r = (int) Math.ceil(outer);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist >= inner && dist <= outer) {
                    b.set(cx + dx, 0, cz + dz, CryptBlocks.LICH_SIGIL_FLOOR.getDefaultState());
                }
            }
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
