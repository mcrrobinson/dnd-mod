package mattonfire.dnd.world.gen.stronghold;

import java.util.function.Consumer;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.OrcEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.DyeColor;
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
 * Base for the Orc Stronghold pieces. Everything is laid out in the hold's own frame round its centre: local
 * {@code a} runs across (east when the gate faces south), local {@code b} runs from the back towards the gate,
 * and y = 0 is the levelled floor. Block states are written as if the gate faced south and turned with
 * {@link BlockState#rotate}, so the whole hold can face any way.
 *
 * <p>A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed, and every
 * draw happens whether or not its block lands in the chunk. Each piece also carries its hold's key (the start
 * chunk, {@link ChunkPos#toLong}) so the orcs it places know which hold they belong to.
 */
public abstract class StrongholdPiece extends StructurePiece {
    public static final Identifier YURT_LOOT = loot("orc_stronghold_yurt");
    public static final Identifier ARMORY_LOOT = loot("orc_stronghold_armory");
    public static final Identifier HOARD_LOOT = loot("orc_stronghold_hoard");

    protected final BlockPos center;
    protected final Direction gate;
    protected final long seed;
    protected final long hold;

    protected StrongholdPiece(StructurePieceType type, BlockPos center, Direction gate, long seed, long hold,
                              int a1, int y1, int b1, int a2, int y2, int b2) {
        super(type, 0, box(center, gate, a1, y1, b1, a2, y2, b2));
        this.center = center;
        this.gate = gate;
        this.seed = seed;
        this.hold = hold;
    }

    protected StrongholdPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        this.gate = Direction.fromHorizontal(nbt.getInt("Gate"));
        this.seed = nbt.getLong("Seed");
        this.hold = nbt.getLong("Hold");
    }

    private static Identifier loot(String name) {
        return new Identifier(DnDClasses.MOD_ID, "chests/" + name);
    }

    /** The rotation that turns "the gate faces south" into the real gate direction. */
    public static BlockRotation rotation(Direction gate) {
        for (BlockRotation rotation : BlockRotation.values()) {
            if (rotation.rotate(Direction.SOUTH) == gate) {
                return rotation;
            }
        }
        return BlockRotation.NONE;
    }

    /** The world position of local (a, y, b) in a hold centred on {@code center} with its gate towards {@code gate}. */
    public static BlockPos at(BlockPos center, Direction gate, int a, int y, int b) {
        return center.add(new BlockPos(a, y, b).rotate(rotation(gate)));
    }

    static BlockBox box(BlockPos center, Direction gate, int a1, int y1, int b1, int a2, int y2, int b2) {
        return BlockBox.create(at(center, gate, a1, y1, b1), at(center, gate, a2, y2, b2));
    }

    public BlockPos at(int a, int y, int b) {
        return at(this.center, this.gate, a, y, b);
    }

    public long hold() {
        return this.hold;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putInt("Gate", this.gate.getHorizontal());
        nbt.putLong("Seed", this.seed);
        nbt.putLong("Hold", this.hold);
    }

    @Override
    public final void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                               Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        this.build(new Builder(world, chunkBox, Random.create(this.seed)));
    }

    protected abstract void build(Builder b);

    // ---- Block state helpers (written as if the gate faced south) ----

    protected static BlockState stairs(Block block, Direction facing, boolean upsideDown) {
        return block.getDefaultState().with(StairsBlock.FACING, facing)
                .with(StairsBlock.HALF, upsideDown ? BlockHalf.TOP : BlockHalf.BOTTOM);
    }

    protected static BlockState slab(Block block, boolean top) {
        return block.getDefaultState().with(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    protected static BlockState log(Block block, Direction.Axis axis) {
        return block.getDefaultState().with(PillarBlock.AXIS, axis);
    }

    protected static BlockState facing(Block block, Direction facing) {
        BlockState state = block.getDefaultState();
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return state.with(Properties.HORIZONTAL_FACING, facing);
        }
        if (state.contains(Properties.FACING)) {
            return state.with(Properties.FACING, facing);
        }
        return state;
    }

    /** A pointed dripstone stake, tip up. */
    protected static BlockState stake() {
        return Blocks.POINTED_DRIPSTONE.getDefaultState()
                .with(net.minecraft.block.PointedDripstoneBlock.VERTICAL_DIRECTION, Direction.UP)
                .with(net.minecraft.block.PointedDripstoneBlock.THICKNESS, net.minecraft.block.enums.Thickness.TIP);
    }

    /** A floor-standing skull or head, turned {@code rotation} (0-15). */
    protected static BlockState skull(Random random) {
        float r = random.nextFloat();
        Block skull = r < 0.6F ? Blocks.SKELETON_SKULL : r < 0.85F ? Blocks.ZOMBIE_HEAD : r < 0.97F ? Blocks.CREEPER_HEAD
                : Blocks.WITHER_SKELETON_SKULL;
        return skull.getDefaultState().with(SkullBlock.ROTATION, random.nextInt(16));
    }

    /** Places blocks, containers and orcs for one chunk's slice of a piece, in local coordinates. */
    protected class Builder {
        final StructureWorldAccess world;
        final BlockBox chunkBox;
        final Random random;
        private final BlockRotation rotation;

        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            this.world = world;
            this.chunkBox = chunkBox;
            this.random = random;
            this.rotation = rotation(StrongholdPiece.this.gate);
        }

        BlockPos pos(int a, int y, int b) {
            return StrongholdPiece.this.at(a, y, b);
        }

        boolean chance(float p) {
            return this.random.nextFloat() < p;
        }

        <T> T pick(T[] options) {
            return options[this.random.nextInt(options.length)];
        }

        boolean inChunk(int a, int y, int b) {
            return this.chunkBox.contains(this.pos(a, y, b));
        }

        void set(int a, int y, int b, BlockState state) {
            BlockPos pos = this.pos(a, y, b);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            BlockState rotated = state.rotate(this.rotation);
            this.world.setBlockState(pos, rotated, Block.NOTIFY_LISTENERS);
            Block block = rotated.getBlock();
            // Shapes that join up with their neighbours get fixed once the chunk is finished.
            if (block instanceof PaneBlock || block instanceof StairsBlock || block instanceof WallBlock
                    || block instanceof FenceBlock || block instanceof ChainBlock) {
                this.world.getChunk(pos).markBlockForPostProcessing(pos);
            }
        }

        void set(int a, int y, int b, Block block) {
            this.set(a, y, b, block.getDefaultState());
        }

        void fill(int a1, int y1, int b1, int a2, int y2, int b2, BlockState state) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int a = Math.min(a1, a2); a <= Math.max(a1, a2); a++) {
                    for (int b = Math.min(b1, b2); b <= Math.max(b1, b2); b++) {
                        this.set(a, y, b, state);
                    }
                }
            }
        }

        void fill(int a1, int y1, int b1, int a2, int y2, int b2, Block block) {
            this.fill(a1, y1, b1, a2, y2, b2, block.getDefaultState());
        }

        void air(int a1, int y1, int b1, int a2, int y2, int b2) {
            this.fill(a1, y1, b1, a2, y2, b2, Blocks.AIR);
        }

        /** Props local a/b up with {@code fill} from below y down to solid ground (at most {@code max} blocks). */
        void foundation(int a, int y, int b, BlockState fill, int max) {
            BlockPos.Mutable pos = this.pos(a, y - 1, b).mutableCopy();
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            for (int i = 0; i < max && pos.getY() > this.world.getBottomY(); i++) {
                BlockState state = this.world.getBlockState(pos);
                if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()
                        && !(state.getBlock() instanceof LeavesBlock)) {
                    return;
                }
                this.world.setBlockState(pos, fill, Block.NOTIFY_LISTENERS);
                pos.move(Direction.DOWN);
            }
        }

        /** A chest or barrel filled from a loot table when first opened. */
        void container(int a, int y, int b, BlockState state, Identifier lootTable) {
            long lootSeed = this.random.nextLong();
            this.set(a, y, b, state);
            BlockPos pos = this.pos(a, y, b);
            if (this.chunkBox.contains(pos) && this.world.getBlockEntity(pos) instanceof LootableContainerBlockEntity container) {
                container.setLootTable(lootTable, lootSeed);
            }
        }

        /** A red war banner with a black skull and border (a wall banner on the block behind it). */
        void warBanner(int a, int y, int b, Direction facing) {
            this.set(a, y, b, Blocks.RED_WALL_BANNER.getDefaultState().with(net.minecraft.block.WallBannerBlock.FACING, facing));
            BlockPos pos = this.pos(a, y, b);
            if (this.chunkBox.contains(pos) && this.world.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
                NbtList patterns = new NbtList();
                for (String pattern : new String[]{"bo", "sku"}) {
                    NbtCompound entry = new NbtCompound();
                    entry.putString("Pattern", pattern);
                    entry.putInt("Color", DyeColor.BLACK.getId());
                    patterns.add(entry);
                }
                NbtCompound nbt = new NbtCompound();
                nbt.put("Patterns", patterns);
                banner.readNbt(nbt);
                banner.markDirty();
            }
        }

        <E extends OrcEntity> void orc(int a, int y, int b, EntityType<E> type, BlockPos home, int range) {
            this.orc(a, y, b, type, home, range, orc -> {
            });
        }

        /**
         * An orc of {@code type} of this hold, keeping within {@code range} of {@code home}. {@code setup} runs
         * before it's added to the world: worldgen saves entities straight away, so later changes would be lost.
         */
        <E extends OrcEntity> void orc(int a, int y, int b, EntityType<E> type, BlockPos home, int range, Consumer<E> setup) {
            float yaw = this.random.nextFloat() * 360.0F;
            BlockPos pos = this.pos(a, y, b);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            E orc = type.create(this.world.toServerWorld());
            if (orc == null) {
                return;
            }
            orc.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
            orc.initialize(this.world, this.world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
            orc.setHome(home, range);
            orc.setHold(StrongholdPiece.this.hold);
            setup.accept(orc);
            this.world.spawnEntityAndPassengers(orc);
        }
    }
}
