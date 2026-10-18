package mattonfire.dnd.world.gen.sanctum;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.function.Consumer;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.TieflingEntity;
import mattonfire.dnd.world.gen.OrientedPiece;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Base for the Brimstone Sanctum pieces, laid out like the Orc Stronghold's: local {@code a} runs across (east
 * when the gate faces south), local {@code b} runs from the back towards the gate, and y = 0 is the platform's
 * top. Block states are written as if the gate faced south and turned with {@link BlockState#rotate}, so the
 * sanctum can face any way.
 *
 * <p>A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed, and every
 * draw happens whether or not its block lands in the chunk. Each piece carries its sanctum's key (the start
 * chunk, {@link ChunkPos#toLong}).
 */
public abstract class BrimstonePiece extends OrientedPiece<BrimstonePiece.Builder> {
    /** The DC 15 offering chest behind the Pact Hall's altar. */
    public static final Identifier ALTAR_LOOT = loot("brimstone_sanctum_altar");
    public static final Identifier DEN_LOOT = loot("brimstone_sanctum_den");
    public static final Identifier MARKET_LOOT = loot("brimstone_sanctum_market");
    public static final Identifier REFUGE_LOOT = loot("brimstone_sanctum_refuge");

    protected final long sanctum;

    protected BrimstonePiece(StructurePieceType type, BlockPos center, Direction gate, long seed, long sanctum,
                             int a1, int y1, int b1, int a2, int y2, int b2) {
        super(type, "Gate", center, gate, seed, a1, y1, b1, a2, y2, b2);
        this.sanctum = sanctum;
    }

    protected BrimstonePiece(StructurePieceType type, NbtCompound nbt) {
        super(type, "Gate", nbt);
        this.sanctum = nbt.getLong("Sanctum");
    }

    public Direction gate() {
        return this.front;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putLong("Sanctum", this.sanctum);
    }

    @Override
    protected Builder builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
        return new Builder(world, chunkBox, random);
    }

    /**
     * The sanctum's stonework: brimstone bricks, with the odd cracked or plain polished blackstone brick and
     * gilded blackstone.
     */
    protected static Block masonry(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.06F ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : r < 0.09F ? Blocks.GILDED_BLACKSTONE
                : r < 0.2F ? Blocks.POLISHED_BLACKSTONE_BRICKS : ModBlockFamilies.BRIMSTONE_BRICKS.base();
    }

    /** Places blocks, containers and Tieflings for one chunk's slice of a piece, in local coordinates. */
    protected class Builder extends OrientedPiece<Builder>.Builder {
        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        /**
         * Props local a/b up with {@code fill} from below y down to solid ground, through air, lava and fire (at
         * most {@code max} blocks). Returns how many blocks it placed, or -1 if it didn't reach ground.
         */
        int foundation(int a, int y, int b, BlockState fill, int max) {
            return this.propUp(a, y, b, fill, max, false);
        }

        /**
         * How many blocks of air, lava or other replaceable blocks lie under local a/y/b (from y - 1 down)
         * before solid ground, or -1 if there's no ground within {@code max} or the column isn't in this chunk.
         */
        int inChunkGap(int a, int y, int b, int max) {
            BlockPos.Mutable pos = this.pos(a, y - 1, b).mutableCopy();
            if (!this.chunkBox.contains(pos)) {
                return -1;
            }
            for (int i = 0; i < max && pos.getY() > this.world.getBottomY(); i++) {
                BlockState state = this.world.getBlockState(pos);
                if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()) {
                    return i;
                }
                pos.move(Direction.DOWN);
            }
            return -1;
        }

        /** A hanging chandelier: a chain from {@code top} down to a ring of four soul lanterns round a centre one. */
        void chandelier(int a, int top, int b, int chain) {
            for (int y = top; y > top - chain; y--) {
                this.set(a, y, b, Blocks.CHAIN);
            }
            int y = top - chain;
            this.set(a, y, b, Blocks.POLISHED_BLACKSTONE_BRICKS);
            this.set(a, y - 1, b, soulLantern(true));
            for (Direction d : Direction.Type.HORIZONTAL) {
                this.set(a + d.getOffsetX(), y, b + d.getOffsetZ(), slab(Blocks.POLISHED_BLACKSTONE_SLAB, true));
                this.set(a + d.getOffsetX(), y - 1, b + d.getOffsetZ(), soulLantern(true));
            }
        }

        <E extends TieflingEntity> void tiefling(int a, int y, int b, EntityType<E> type, int range) {
            this.tiefling(a, y, b, type, range, npc -> {
            });
        }

        /**
         * A Tiefling of {@code type} living here, keeping within {@code range} of this spot. {@code setup} runs
         * before it's added to the world: worldgen saves entities straight away, so later changes would be lost.
         */
        <E extends TieflingEntity> void tiefling(int a, int y, int b, EntityType<E> type, int range, Consumer<E> setup) {
            BlockPos home = this.pos(a, y, b);
            this.npc(a, y, b, type, npc -> {
                npc.setHome(home, range);
                setup.accept(npc);
            });
        }
    }
}
