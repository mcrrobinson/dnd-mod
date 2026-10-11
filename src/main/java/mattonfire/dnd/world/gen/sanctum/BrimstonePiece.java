package mattonfire.dnd.world.gen.sanctum;

import java.util.function.Consumer;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.TieflingEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.BlockRotation;
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
 * Base for the Brimstone Sanctum pieces, laid out like the Orc Stronghold's: local {@code a} runs across (east
 * when the gate faces south), local {@code b} runs from the back towards the gate, and y = 0 is the platform's
 * top. Block states are written as if the gate faced south and turned with {@link BlockState#rotate}, so the
 * sanctum can face any way.
 *
 * <p>A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed, and every
 * draw happens whether or not its block lands in the chunk. Each piece carries its sanctum's key (the start
 * chunk, {@link ChunkPos#toLong}).
 */
public abstract class BrimstonePiece extends StructurePiece {
    /** The DC 15 offering chest behind the Pact Hall's altar. */
    public static final Identifier ALTAR_LOOT = loot("brimstone_sanctum_altar");
    public static final Identifier DEN_LOOT = loot("brimstone_sanctum_den");
    public static final Identifier MARKET_LOOT = loot("brimstone_sanctum_market");
    public static final Identifier REFUGE_LOOT = loot("brimstone_sanctum_refuge");

    protected final BlockPos center;
    protected final Direction gate;
    protected final long seed;
    protected final long sanctum;

    protected BrimstonePiece(StructurePieceType type, BlockPos center, Direction gate, long seed, long sanctum,
                             int a1, int y1, int b1, int a2, int y2, int b2) {
        super(type, 0, box(center, gate, a1, y1, b1, a2, y2, b2));
        this.center = center;
        this.gate = gate;
        this.seed = seed;
        this.sanctum = sanctum;
    }

    protected BrimstonePiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        this.gate = Direction.fromHorizontal(nbt.getInt("Gate"));
        this.seed = nbt.getLong("Seed");
        this.sanctum = nbt.getLong("Sanctum");
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

    /** The world position of local (a, y, b) in a sanctum centred on {@code center} with its gate towards {@code gate}. */
    public static BlockPos at(BlockPos center, Direction gate, int a, int y, int b) {
        return center.add(new BlockPos(a, y, b).rotate(rotation(gate)));
    }

    static BlockBox box(BlockPos center, Direction gate, int a1, int y1, int b1, int a2, int y2, int b2) {
        return BlockBox.create(at(center, gate, a1, y1, b1), at(center, gate, a2, y2, b2));
    }

    public BlockPos at(int a, int y, int b) {
        return at(this.center, this.gate, a, y, b);
    }

    public BlockPos center() {
        return this.center;
    }

    public Direction gate() {
        return this.gate;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putInt("Gate", this.gate.getHorizontal());
        nbt.putLong("Seed", this.seed);
        nbt.putLong("Sanctum", this.sanctum);
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

    protected static BlockState pillar(Block block, Direction.Axis axis) {
        return block.getDefaultState().with(PillarBlock.AXIS, axis);
    }

    protected static BlockState soulLantern(boolean hanging) {
        return Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, hanging);
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

    /** The sanctum's stonework: blackstone bricks, a few cracked, the odd gilded or plain blackstone. */
    protected static Block masonry(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.08F ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : r < 0.11F ? Blocks.GILDED_BLACKSTONE
                : r < 0.2F ? Blocks.BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS;
    }

    /** Places blocks, containers and Tieflings for one chunk's slice of a piece, in local coordinates. */
    protected class Builder {
        final StructureWorldAccess world;
        final BlockBox chunkBox;
        final Random random;
        private final BlockRotation rotation;

        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            this.world = world;
            this.chunkBox = chunkBox;
            this.random = random;
            this.rotation = rotation(BrimstonePiece.this.gate);
        }

        BlockPos pos(int a, int y, int b) {
            return BrimstonePiece.this.at(a, y, b);
        }

        boolean chance(float p) {
            return this.random.nextFloat() < p;
        }

        <T> T pick(T[] options) {
            return options[this.random.nextInt(options.length)];
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

        /**
         * Props local a/b up with {@code fill} from below y down to solid ground, through air, lava and fire (at
         * most {@code max} blocks). Returns how many blocks it placed, or -1 if it didn't reach ground.
         */
        int foundation(int a, int y, int b, BlockState fill, int max) {
            BlockPos.Mutable pos = this.pos(a, y - 1, b).mutableCopy();
            if (!this.chunkBox.contains(pos)) {
                return 0;
            }
            for (int i = 0; i < max && pos.getY() > this.world.getBottomY(); i++) {
                BlockState state = this.world.getBlockState(pos);
                if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()) {
                    return i;
                }
                this.world.setBlockState(pos, fill, Block.NOTIFY_LISTENERS);
                pos.move(Direction.DOWN);
            }
            return -1;
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

        /** A chest or barrel filled from a loot table when first opened. */
        void container(int a, int y, int b, BlockState state, Identifier lootTable) {
            long lootSeed = this.random.nextLong();
            this.set(a, y, b, state);
            BlockPos pos = this.pos(a, y, b);
            if (this.chunkBox.contains(pos) && this.world.getBlockEntity(pos) instanceof LootableContainerBlockEntity container) {
                container.setLootTable(lootTable, lootSeed);
            }
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
            float yaw = this.random.nextFloat() * 360.0F;
            BlockPos pos = this.pos(a, y, b);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            E npc = type.create(this.world.toServerWorld());
            if (npc == null) {
                return;
            }
            npc.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
            npc.initialize(this.world, this.world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
            npc.setHome(pos, range);
            setup.accept(npc);
            this.world.spawnEntityAndPassengers(npc);
        }
    }
}
