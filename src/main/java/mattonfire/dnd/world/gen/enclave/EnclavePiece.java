package mattonfire.dnd.world.gen.enclave;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.entity.ElfEntity;
import mattonfire.dnd.world.gen.ModPiece;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Base for the elven enclave pieces. Each piece is built round an origin (its middle, on the ground
 * layer: local y = 0 is the top block of the ground), in world directions: the trees, the well and the
 * gardens look the same from every side, so nothing is rotated.
 *
 * <p>A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed (never
 * the per-chunk random) and every random draw happens whether or not its block lands in the chunk, which
 * keeps every chunk's slice of it in agreement. Shapes that don't need a draw per block use
 * {@link #noise}, a hash of the position.
 *
 * <p>Our trees are built from birch <em>wood</em> (bark on all sides), never logs, so a piece can tell
 * them from the forest's own trees and clear those out of its way ({@link Builder#clearForest}).
 */
public abstract class EnclavePiece extends ModPiece<EnclavePiece.Builder> {
    protected static final Identifier TALAN_LOOT = loot("elven_enclave_talan");
    public static final Identifier HEART_LOOT = loot("elven_enclave_heart");

    protected final int ox;
    protected final int oy;
    protected final int oz;

    protected EnclavePiece(StructurePieceType type, BlockBox box, int ox, int oy, int oz, long seed) {
        super(type, box, seed);
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
    }

    protected EnclavePiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.ox = nbt.getInt("OX");
        this.oy = nbt.getInt("OY");
        this.oz = nbt.getInt("OZ");
    }

    /** A box from local min to local max round the origin. */
    protected static BlockBox box(int ox, int oy, int oz, int x1, int y1, int z1, int x2, int y2, int z2) {
        return new BlockBox(ox + x1, oy + y1, oz + z1, ox + x2, oy + y2, oz + z2);
    }

    /** The origin: the middle of the piece on its ground layer. */
    public BlockPos origin() {
        return new BlockPos(this.ox, this.oy, this.oz);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("OX", this.ox);
        nbt.putInt("OY", this.oy);
        nbt.putInt("OZ", this.oz);
    }

    @Override
    protected Builder builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
        return new Builder(world, chunkBox, random);
    }

    /** A repeatable 0-1 value for a local position (the same in every chunk). */
    protected float noise(int x, int y, int z) {
        long h = this.seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 31)) * 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return (h >>> 40) / (float) (1L << 24);
    }

    /** Logs, non-persistent leaves, vines and bee nests: the forest's own trees, which ours replace. */
    static boolean isForest(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock) {
            return !state.get(LeavesBlock.PERSISTENT);
        }
        if (state.isIn(BlockTags.LOGS)) {
            String path = Registries.BLOCK.getId(block).getPath();
            return path.endsWith("_log") && !path.startsWith("stripped_");
        }
        return block == Blocks.VINE || block == Blocks.BEE_NEST;
    }

    protected static final Block[] FLOWERS = {
            Blocks.LILY_OF_THE_VALLEY, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.ALLIUM,
            Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.BLUE_ORCHID, Blocks.DANDELION
    };

    /** Places blocks, containers and entities for one chunk's slice of a piece, in local coordinates. */
    protected class Builder extends ModPiece<Builder>.Builder {
        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            super(world, chunkBox, random);
        }

        @Override
        public BlockPos pos(int x, int y, int z) {
            return new BlockPos(EnclavePiece.this.ox + x, EnclavePiece.this.oy + y, EnclavePiece.this.oz + z);
        }

        /** Writes the block and lets any liquid in it flow. */
        @Override
        protected void place(int x, int y, int z, BlockPos pos, BlockState state) {
            this.world.setBlockState(pos, state, PieceBlocks.UPDATE);
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                this.world.scheduleFluidTick(pos, fluid.getFluid(), 0);
            }
        }

        /** The block at local x/y/z, or air outside this chunk. */
        @Override
        public BlockState get(int x, int y, int z) {
            BlockPos pos = this.pos(x, y, z);
            return this.chunkBox.contains(pos) ? this.world.getBlockState(pos) : Blocks.AIR.getDefaultState();
        }

        /** Clears the forest's own trees (see {@link #isForest}) out of a local box. */
        void clearForest(int x1, int y1, int z1, int x2, int y2, int z2) {
            BlockPos.Mutable pos = new BlockPos.Mutable();
            int minX = Math.max(EnclavePiece.this.ox + x1, this.chunkBox.getMinX());
            int maxX = Math.min(EnclavePiece.this.ox + x2, this.chunkBox.getMaxX());
            int minZ = Math.max(EnclavePiece.this.oz + z1, this.chunkBox.getMinZ());
            int maxZ = Math.min(EnclavePiece.this.oz + z2, this.chunkBox.getMaxZ());
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = EnclavePiece.this.oy + y1; y <= EnclavePiece.this.oy + y2; y++) {
                        pos.set(x, y, z);
                        if (isForest(this.world.getBlockState(pos))) {
                            this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), PieceBlocks.UPDATE);
                        }
                    }
                }
            }
        }

        /** Fills the gap under local x/y/z with {@code fill} down to solid ground (at most 24 blocks). */
        void anchor(int x, int y, int z, BlockState fill) {
            for (int dy = y - 1; dy >= y - 24; dy--) {
                BlockPos pos = this.pos(x, dy, z);
                if (!this.chunkBox.contains(pos) || pos.getY() <= this.world.getBottomY()) {
                    return;
                }
                BlockState state = this.world.getBlockState(pos);
                if (!state.isAir() && !state.isReplaceable() && state.getFluidState().isEmpty() && !isForest(state)) {
                    return;
                }
                this.world.setBlockState(pos, fill, PieceBlocks.UPDATE);
            }
        }

        /** Sets the ground block at y = 0 and fills any gap beneath it with dirt. */
        void ground(int x, int z, BlockState top) {
            this.set(x, 0, z, top);
            this.anchor(x, 0, z, Blocks.DIRT.getDefaultState());
        }

        /**
         * A level patch of forest floor: grass (or {@code top}) on dirt, anything above it up to
         * {@code clear} cleared away.
         */
        void lawn(int x1, int z1, int x2, int z2, int clear) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    this.ground(x, z, Blocks.GRASS_BLOCK.getDefaultState());
                    this.air(x, 1, z, x, clear, z);
                }
            }
        }

        void flower(int x, int y, int z) {
            this.set(x, y, z, this.pick(FLOWERS));
        }

        /** An elf of {@code type} standing at local x/y/z, at home there. */
        void elf(int x, int y, int z, EntityType<? extends ElfEntity> type) {
            BlockPos home = this.pos(x, y, z);
            this.npc(x, y, z, type, elf -> elf.setHome(home));
        }
    }
}
