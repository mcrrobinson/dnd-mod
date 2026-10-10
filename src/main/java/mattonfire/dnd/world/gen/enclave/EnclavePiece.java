package mattonfire.dnd.world.gen.enclave;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.ElfEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

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
public abstract class EnclavePiece extends StructurePiece {
    protected static final Identifier TALAN_LOOT = new Identifier(DnDClasses.MOD_ID, "chests/elven_enclave_talan");
    public static final Identifier HEART_LOOT = new Identifier(DnDClasses.MOD_ID, "chests/elven_enclave_heart");

    protected final int ox;
    protected final int oy;
    protected final int oz;
    private final long seed;

    protected EnclavePiece(StructurePieceType type, BlockBox box, int ox, int oy, int oz, long seed) {
        super(type, 0, box);
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.seed = seed;
    }

    protected EnclavePiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.ox = nbt.getInt("OX");
        this.oy = nbt.getInt("OY");
        this.oz = nbt.getInt("OZ");
        this.seed = nbt.getLong("Seed");
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
        nbt.putInt("OX", this.ox);
        nbt.putInt("OY", this.oy);
        nbt.putInt("OZ", this.oz);
        nbt.putLong("Seed", this.seed);
    }

    @Override
    public final void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                               Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        this.build(new Builder(world, chunkBox, Random.create(this.seed)));
    }

    protected abstract void build(Builder b);

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

    // ---- Block state helpers ----

    protected static BlockState stairs(Block block, Direction facing, boolean upsideDown) {
        return block.getDefaultState().with(StairsBlock.FACING, facing)
                .with(StairsBlock.HALF, upsideDown ? BlockHalf.TOP : BlockHalf.BOTTOM);
    }

    protected static BlockState slab(Block block, boolean top) {
        return block.getDefaultState().with(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    protected static BlockState leaves(Block block) {
        return block.getDefaultState().with(LeavesBlock.PERSISTENT, true);
    }

    protected static BlockState lantern(boolean hanging) {
        return Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, hanging);
    }

    protected static BlockState facing(Block block, Direction facing) {
        BlockState state = block.getDefaultState();
        if (state.contains(net.minecraft.state.property.Properties.HORIZONTAL_FACING)) {
            return state.with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, facing);
        }
        if (state.contains(net.minecraft.state.property.Properties.FACING)) {
            return state.with(net.minecraft.state.property.Properties.FACING, facing);
        }
        return state;
    }

    protected static final Block[] FLOWERS = {
            Blocks.LILY_OF_THE_VALLEY, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.ALLIUM,
            Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.BLUE_ORCHID, Blocks.DANDELION
    };

    /** Places blocks, containers and entities for one chunk's slice of a piece, in local coordinates. */
    protected class Builder {
        final StructureWorldAccess world;
        final BlockBox chunkBox;
        final Random random;

        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            this.world = world;
            this.chunkBox = chunkBox;
            this.random = random;
        }

        BlockPos pos(int x, int y, int z) {
            return new BlockPos(EnclavePiece.this.ox + x, EnclavePiece.this.oy + y, EnclavePiece.this.oz + z);
        }

        boolean contains(int x, int y, int z) {
            return this.chunkBox.contains(this.pos(x, y, z));
        }

        boolean chance(float p) {
            return this.random.nextFloat() < p;
        }

        <T> T pick(T[] options) {
            return options[this.random.nextInt(options.length)];
        }

        void set(int x, int y, int z, BlockState state) {
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            this.world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                this.world.scheduleFluidTick(pos, fluid.getFluid(), 0);
            }
            Block block = state.getBlock();
            // Shapes that join up with their neighbours get fixed once the chunk is finished.
            if (block instanceof FenceBlock || block instanceof PaneBlock || block instanceof StairsBlock
                    || block instanceof WallBlock || block instanceof ChainBlock) {
                this.world.getChunk(pos).markBlockForPostProcessing(pos);
            }
        }

        void set(int x, int y, int z, Block block) {
            this.set(x, y, z, block.getDefaultState());
        }

        BlockState get(int x, int y, int z) {
            BlockPos pos = this.pos(x, y, z);
            return this.chunkBox.contains(pos) ? this.world.getBlockState(pos) : Blocks.AIR.getDefaultState();
        }

        void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                        this.set(x, y, z, state);
                    }
                }
            }
        }

        void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
            this.fill(x1, y1, z1, x2, y2, z2, block.getDefaultState());
        }

        void air(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.fill(x1, y1, z1, x2, y2, z2, Blocks.AIR);
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
                            this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
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
                this.world.setBlockState(pos, fill, Block.NOTIFY_LISTENERS);
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

        /** A chest or barrel filled from a loot table when first opened. */
        void container(int x, int y, int z, BlockState state, Identifier lootTable) {
            long lootSeed = this.random.nextLong();
            this.set(x, y, z, state);
            BlockPos pos = this.pos(x, y, z);
            if (this.chunkBox.contains(pos) && this.world.getBlockEntity(pos) instanceof LootableContainerBlockEntity container) {
                container.setLootTable(lootTable, lootSeed);
            }
        }

        /** A beehive dripping with honey, with a few bees living in it. */
        void beehive(int x, int y, int z, Direction facing, int bees) {
            this.set(x, y, z, facing(Blocks.BEEHIVE, facing).with(net.minecraft.block.BeehiveBlock.HONEY_LEVEL, 5));
            BlockPos pos = this.pos(x, y, z);
            int[] ticks = new int[bees];
            for (int i = 0; i < bees; i++) {
                ticks[i] = this.random.nextInt(599);
            }
            if (this.chunkBox.contains(pos) && this.world.getBlockEntity(pos) instanceof BeehiveBlockEntity hive) {
                for (int t : ticks) {
                    NbtCompound bee = new NbtCompound();
                    bee.putString("id", Registries.ENTITY_TYPE.getId(EntityType.BEE).toString());
                    hive.addBee(bee, t, false);
                }
            }
        }

        /** An elf of {@code type} standing at local x/y/z, at home there. */
        void elf(int x, int y, int z, EntityType<? extends ElfEntity> type) {
            float yaw = this.random.nextFloat() * 360.0F;
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            ElfEntity elf = type.create(this.world.toServerWorld());
            if (elf == null) {
                return;
            }
            elf.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
            elf.initialize(this.world, this.world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
            elf.setHome(pos);
            this.world.spawnEntityAndPassengers(elf);
        }
    }
}
