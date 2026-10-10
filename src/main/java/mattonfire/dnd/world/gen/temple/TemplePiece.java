package mattonfire.dnd.world.gen.temple;

import java.util.function.Consumer;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.DragonbornEntity;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
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
import org.jetbrains.annotations.Nullable;

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
public abstract class TemplePiece extends StructurePiece {
    public static final Identifier RELIQUARY = loot("dragon_temple_reliquary");
    public static final Identifier CELL = loot("dragon_temple_cell");

    protected static final Direction BACK = Direction.NORTH;
    protected static final Direction FRONT = Direction.SOUTH;

    /** How far down a floor is propped up over drops and cliffs. */
    private static final int MAX_FOUNDATION = 32;

    private final long seed;

    protected TemplePiece(StructurePieceType type, BlockBox box, Direction facing, long seed) {
        super(type, 0, box);
        this.seed = seed;
        this.setOrientation(facing);
    }

    protected TemplePiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.seed = nbt.getLong("Seed");
    }

    private static Identifier loot(String name) {
        return new Identifier(DnDClasses.MOD_ID, "chests/" + name);
    }

    /**
     * The direction "to the right" of a piece facing {@code facing}, as vanilla lays out local x: east for
     * pieces facing north or south, south for pieces facing east or west.
     */
    public static Direction lateral(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
    }

    /**
     * A box of the given local size whose front edge is centred on {@code front}: the block at local
     * (width / 2, 0, 0) lands exactly there.
     */
    protected static BlockBox boxFrom(BlockPos front, Direction facing, int width, int height, int depth) {
        boolean alongZ = facing.getAxis() == Direction.Axis.Z;
        int sizeX = alongZ ? width : depth;
        int sizeZ = alongZ ? depth : width;
        int lx;
        int lz;
        switch (facing) {
            case NORTH -> {
                lx = width / 2;
                lz = sizeZ - 1;
            }
            case SOUTH -> {
                lx = width / 2;
                lz = 0;
            }
            case WEST -> {
                lx = sizeX - 1;
                lz = width / 2;
            }
            default -> {
                lx = 0;
                lz = width / 2;
            }
        }
        int minX = front.getX() - lx;
        int minZ = front.getZ() - lz;
        return new BlockBox(minX, front.getY(), minZ, minX + sizeX - 1, front.getY() + height - 1, minZ + sizeZ - 1);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putLong("Seed", this.seed);
    }

    @Override
    public final void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                               Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        this.build(new Builder(world, chunkBox, Random.create(this.seed)));
    }

    protected abstract void build(Builder b);

    /** The world position of a local spot (which may lie outside the box). */
    public BlockPos worldPos(int x, int y, int z) {
        return this.offsetPos(x, y, z);
    }

    // ---- Block state helpers (written facing north) ----

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

    protected static BlockState lantern(boolean hanging) {
        return Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, hanging);
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

    /** The temple stonework: deepslate tiles, a few cracked, and the odd polished blackstone brick. */
    protected static Block masonry(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.06F ? Blocks.CRACKED_DEEPSLATE_TILES : r < 0.18F ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.DEEPSLATE_TILES;
    }

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
            return TemplePiece.this.offsetPos(x, y, z);
        }

        /** Turns a direction written facing north into the piece's real direction. */
        Direction dir(Direction local) {
            if (local.getAxis() == Direction.Axis.Y) {
                return local;
            }
            return TemplePiece.this.getRotation().rotate(TemplePiece.this.getMirror().apply(local));
        }

        boolean chance(float p) {
            return this.random.nextFloat() < p;
        }

        <T> T pick(T[] options) {
            return options[this.random.nextInt(options.length)];
        }

        void set(int x, int y, int z, BlockState state) {
            TemplePiece.this.addBlock(this.world, state, x, y, z, this.chunkBox);
            Block block = state.getBlock();
            // Shapes that join up with their neighbours get fixed once the chunk is finished.
            if (block instanceof PaneBlock || block instanceof StairsBlock || block instanceof WallBlock
                    || block instanceof FenceBlock || block == Blocks.CHAIN) {
                BlockPos pos = this.pos(x, y, z);
                if (this.chunkBox.contains(pos)) {
                    this.world.getChunk(pos).markBlockForPostProcessing(pos);
                }
            }
        }

        void set(int x, int y, int z, Block block) {
            this.set(x, y, z, block.getDefaultState());
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

        @Nullable
        BlockState get(int x, int y, int z) {
            BlockPos pos = this.pos(x, y, z);
            return this.chunkBox.contains(pos) ? this.world.getBlockState(pos) : null;
        }

        /** Props local x/z up with {@code fill} from below y down to solid ground (at most 32 blocks). */
        void foundation(int x, int y, int z, BlockState fill) {
            BlockPos.Mutable pos = this.pos(x, y - 1, z).mutableCopy();
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            for (int i = 0; i < MAX_FOUNDATION && pos.getY() > this.world.getBottomY(); i++) {
                BlockState state = this.world.getBlockState(pos);
                if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()
                        && !(state.getBlock() instanceof net.minecraft.block.LeavesBlock)) {
                    return;
                }
                this.world.setBlockState(pos, fill, Block.NOTIFY_LISTENERS);
                pos.move(Direction.DOWN);
            }
        }

        BlockState candle(Block candle) {
            return candle.getDefaultState().with(CandleBlock.CANDLES, 1 + this.random.nextInt(4)).with(CandleBlock.LIT, true);
        }

        /** A wall banner on the wall behind it. */
        void banner(int x, int y, int z, Direction facing, DyeColor color) {
            Block banner = Registries.BLOCK.get(new Identifier(color.getName() + "_wall_banner"));
            this.set(x, y, z, banner.getDefaultState().with(WallBannerBlock.FACING, facing));
        }

        /** A bed with its foot at x/y/z and its head one block towards {@code facing} (local). */
        void bed(int x, int y, int z, Direction facing, DyeColor color) {
            Block bed = Registries.BLOCK.get(new Identifier(color.getName() + "_bed"));
            BlockState state = bed.getDefaultState().with(BedBlock.FACING, facing);
            this.set(x, y, z, state.with(BedBlock.PART, BedPart.FOOT));
            this.set(x + facing.getOffsetX(), y, z - facing.getOffsetZ(), state.with(BedBlock.PART, BedPart.HEAD));
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

        void chest(int x, int y, int z, Direction facing, Identifier lootTable) {
            this.container(x, y, z, facing(Blocks.CHEST, facing), lootTable);
        }

        /** An armour stand wearing the given pieces (head, chest, legs, feet; empty for none). */
        void armorStand(int x, int y, int z, Direction facing, ItemStack... armor) {
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            ArmorStandEntity stand = new ArmorStandEntity(this.world.toServerWorld(), pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            stand.setYaw(this.dir(facing).asRotation());
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < armor.length && i < slots.length; i++) {
                stand.equipStack(slots[i], armor[i]);
            }
            this.world.spawnEntity(stand);
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
            float yaw = this.random.nextFloat() * 360.0F;
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            E npc = type.create(this.world.toServerWorld());
            if (npc == null) {
                return;
            }
            npc.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
            npc.initialize(this.world, this.world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
            npc.setHome(pos);
            setup.accept(npc);
            this.world.spawnEntityAndPassengers(npc);
        }
    }
}
