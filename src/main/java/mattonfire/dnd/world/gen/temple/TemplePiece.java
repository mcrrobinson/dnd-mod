package mattonfire.dnd.world.gen.temple;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.function.Consumer;
import mattonfire.dnd.classes.Registry.BlockFamily;
import mattonfire.dnd.entity.DragonbornEntity;
import mattonfire.dnd.world.gen.FacingPiece;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Base for the Dragon Temple pieces. Like the fortress pieces they're designed in local coordinates: x runs
 * across the front, z runs from the front edge (z = 0, towards the grand stair and the valley) to the back
 * (into the mountain), and y = 0 is the floor. Block states are written as if the piece faced north, so
 * "towards the back" is {@link Direction#NORTH} and "towards the stair" {@link Direction#SOUTH}; vanilla
 * rotates them to the piece's real facing.
 *
 * <p>A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed, and
 * every draw happens whether or not its block lands in the chunk.
 */
public abstract class TemplePiece extends FacingPiece<TemplePiece.Builder> {
    public static final Identifier RELIQUARY = loot("dragon_temple_reliquary");
    public static final Identifier CELL = loot("dragon_temple_cell");

    protected static final Direction BACK = Direction.NORTH;
    protected static final Direction FRONT = Direction.SOUTH;

    /** How far down a floor is propped up over drops and cliffs. */
    private static final int MAX_FOUNDATION = 32;

    protected TemplePiece(StructurePieceType type, BlockBox box, Direction facing, long seed) {
        super(type, box, facing, seed);
    }

    protected TemplePiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
    }

    /**
     * The direction "to the right" of a piece facing {@code facing}, as vanilla lays out local x: east for
     * pieces facing north or south, south for pieces facing east or west.
     */
    public static Direction lateral(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
    }

    @Override
    protected Builder builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
        return new Builder(world, chunkBox, random);
    }

    /** The temple stonework: deepslate tiles, a few cracked, and the odd polished blackstone brick. */
    protected static Block masonry(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.06F ? Blocks.CRACKED_DEEPSLATE_TILES : r < 0.18F ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.DEEPSLATE_TILES;
    }

    /**
     * A hip roof of {@code tiles} over the rectangle x0..x1 by z0..z1, its eaves at {@code y}: rings of stairs
     * rising towards the middle, ring {@code k} (0 at the eaves) {@code rise[k]} above the eaves. Rises that
     * start flat and then climb give the curved, flared look of a temple roof. Rings past the end of
     * {@code rise} are left open (a lower roof skirting an upper storey). Where the rings from front and back
     * meet in a single row it's a ridge of double slabs. With {@code upturned}, the four eave corners sweep up
     * a block. Double slabs, never the tiles' base block: in the sanctum that's an altar plinth.
     */
    protected static void hipRoof(Builder b, int x0, int z0, int x1, int z1, int y, int[] rise, BlockFamily tiles,
                                  boolean upturned) {
        hipRoof(b, x0, z0, x1, z1, y, rise, x -> tiles, upturned);
    }

    /** {@link #hipRoof(Builder, int, int, int, int, int, int[], BlockFamily, boolean)} with each column's tiles. */
    protected static void hipRoof(Builder b, int x0, int z0, int x1, int z1, int y, int[] rise,
                                  java.util.function.IntFunction<BlockFamily> tilesAt, boolean upturned) {
        int midZ = (z1 - z0) / 2;
        int midX = (x1 - x0) / 2;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int kx = Math.min(x - x0, x1 - x);
                int kz = Math.min(z - z0, z1 - z);
                int k = Math.min(kx, kz);
                if (k >= rise.length) {
                    continue;
                }
                int yy = y + rise[k];
                BlockFamily tiles = tilesAt.apply(x);
                BlockState ridge = tiles.slab().getDefaultState().with(SlabBlock.TYPE, SlabType.DOUBLE);
                boolean ridgeRow = (z1 - z0) % 2 == 0 && k == kz && kz == midZ
                        || (x1 - x0) % 2 == 0 && k == kx && kx == midX;
                if (ridgeRow) {
                    b.set(x, yy, z, ridge);
                    continue;
                }
                Direction up = k == kz ? (z - z0 <= z1 - z ? BACK : FRONT) : x - x0 <= x1 - x ? Direction.EAST : Direction.WEST;
                if (upturned && kx == 0 && kz == 0) {
                    // The corner sweeps up: a tile slab under a raised stair.
                    b.set(x, yy, z, tiles.slab().getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
                    b.set(x, yy + 1, z, stairs(tiles, up, false));
                } else {
                    b.set(x, yy, z, stairs(tiles, up, false));
                }
            }
        }
    }

    /** A rough natural-stone block for retaining walls, picked by position so every chunk agrees. */
    protected static Block rubble(long hash) {
        int r = (int) Math.floorMod(hash, 100L);
        return r < 30 ? Blocks.COBBLESTONE : r < 55 ? Blocks.STONE_BRICKS : r < 70 ? Blocks.ANDESITE
                : r < 82 ? Blocks.CRACKED_STONE_BRICKS : r < 92 ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE;
    }

    /** A well-mixed hash of a world position and a seed. */
    protected static long hash(int x, int y, int z, long seed) {
        long h = seed ^ x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
        return h ^ (h >>> 33);
    }

    /** Places blocks, containers and entities for one chunk's slice of a piece, in local coordinates. */
    protected class Builder extends FacingPiece<Builder>.Builder {
        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        /** Props local x/z up with {@code fill} from below y down to solid ground (at most 32 blocks). */
        void foundation(int x, int y, int z, BlockState fill) {
            this.propUp(x, y, z, fill, MAX_FOUNDATION, true);
        }

        BlockState candle(Block candle) {
            return candle.getDefaultState().with(CandleBlock.CANDLES, 1 + this.random.nextInt(4)).with(CandleBlock.LIT, true);
        }

        void dragonborn(int x, int y, int z, EntityType<? extends DragonbornEntity> type) {
            this.dragonborn(x, y, z, type, npc -> {
            });
        }

        /**
         * A Dragonborn of {@code type} living here, at home on this spot. {@code setup} runs before it's
         * added to the world: worldgen saves entities straight away, so later changes would be lost.
         */
        <E extends DragonbornEntity> void dragonborn(int x, int y, int z, EntityType<E> type, Consumer<E> setup) {
            BlockPos home = this.pos(x, y, z);
            this.npc(x, y, z, type, npc -> {
                npc.setHome(home);
                setup.accept(npc);
            });
        }
    }
}
