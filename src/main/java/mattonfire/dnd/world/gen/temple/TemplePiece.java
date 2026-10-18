package mattonfire.dnd.world.gen.temple;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.function.Consumer;
import java.util.function.IntFunction;
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
     * A stepped hip roof of Dragonscale Tiles on top of a {@code width} by {@code depth} building, from {@code y}
     * up: {@code tiers} rings of stairs, each one block in and one up from the last and climbing to the middle,
     * capped with a flat top of double slabs. {@code tilesAt} gives each column's tiles (local x), so a roof
     * can carry stripes of another colour. The hollow under the upper rings is filled with hidden stone.
     */
    protected static void roof(Builder b, int width, int depth, int y, int tiers, IntFunction<BlockFamily> tilesAt) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                int inset = Math.min(Math.min(x, width - 1 - x), Math.min(z, depth - 1 - z));
                BlockFamily tiles = tilesAt.apply(x);
                for (int k = 0; k < tiers && k <= inset; k++) {
                    if (k == inset) {
                        // The ring: stairs rising towards the middle (their full back faces inwards).
                        Direction in = z == k ? BACK : z == depth - 1 - k ? FRONT : x == k ? Direction.EAST : Direction.WEST;
                        b.set(x, y + k, z, stairs(tiles, in, false));
                    } else if (k == tiers - 1) {
                        b.set(x, y + k, z, tiles.slab().getDefaultState().with(SlabBlock.TYPE, SlabType.DOUBLE));
                    } else {
                        b.set(x, y + k, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
                    }
                }
            }
        }
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
