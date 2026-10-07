package mattonfire.dnd.world.gen.fortress;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.entity.MountainDwarfEntity;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
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
 * Base for the dwarven fortress pieces. Like the hobbit pieces, they're designed in local
 * coordinates: x runs across the front, z runs from the front edge (z = 0, where you come in) to
 * the back, and y = 0 is the floor. Block states are written as if the piece faced north (so
 * "back towards the entrance" is {@link Direction#SOUTH}) and get rotated to its real facing.
 *
 * The pieces are carved out of the mountain: each one fills its whole box, walls and all, and
 * props its floor up wherever the rock falls away beneath it.
 *
 * A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed.
 */
public abstract class FortressPiece extends StructurePiece {
    protected static final Direction OUT = Direction.SOUTH;
    protected static final Direction IN = Direction.NORTH;

    protected static final Identifier TREASURY = loot("dwarven_fortress_treasury");
    protected static final Identifier FORGE = loot("dwarven_fortress_forge");
    protected static final Identifier BARRACKS = loot("dwarven_fortress_barracks");
    protected static final Identifier BREWHALL = loot("dwarven_fortress_brewhall");
    protected static final Identifier MINE = loot("dwarven_fortress_mine");

    /** How far down a floor gets propped up over caves and cliffs. */
    private static final int MAX_FOUNDATION = 24;

    private final long seed;

    protected FortressPiece(StructurePieceType type, BlockBox box, Direction facing, long seed) {
        super(type, 0, box);
        this.seed = seed;
        this.setOrientation(facing);
    }

    protected FortressPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.seed = nbt.getLong("Seed");
    }

    private static Identifier loot(String name) {
        return new Identifier(DnDClasses.MOD_ID, "chests/" + name);
    }

    /**
     * A box of the given local size whose front edge is centred on {@code front}: the block at
     * local (width / 2, 0, 0) lands exactly there.
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

    protected int width() {
        return this.getFacing().getAxis() == Direction.Axis.Z ? this.boundingBox.getBlockCountX() : this.boundingBox.getBlockCountZ();
    }

    protected int depth() {
        return this.getFacing().getAxis() == Direction.Axis.Z ? this.boundingBox.getBlockCountZ() : this.boundingBox.getBlockCountX();
    }

    protected int height() {
        return this.boundingBox.getBlockCountY();
    }

    /** The world position of a local floor-level spot (which may lie outside the box). */
    public BlockPos worldPos(int x, int y, int z) {
        return new BlockPos(this.applyXTransform(x, z), this.applyYTransform(y), this.applyZTransform(x, z));
    }

    /** Where the next piece back starts: the floor block just behind the middle of the back wall. */
    public BlockPos backCenter() {
        return this.worldPos(this.width() / 2, 0, this.depth());
    }

    /** The real direction of a step from local (x, z) to (x + dx, z + dz). */
    public Direction worldDirection(int dx, int dz) {
        BlockPos a = this.worldPos(0, 0, 0);
        BlockPos b = this.worldPos(dx, 0, dz);
        return Direction.fromVector(b.getX() - a.getX(), 0, b.getZ() - a.getZ());
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

    // ---- Block state helpers (written facing north) ----

    protected static BlockState stairs(Block block, Direction facing, boolean upsideDown) {
        return block.getDefaultState().with(StairsBlock.FACING, facing)
                .with(StairsBlock.HALF, upsideDown ? BlockHalf.TOP : BlockHalf.BOTTOM);
    }

    protected static BlockState topSlab(Block block) {
        return block.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
    }

    protected static BlockState bottomSlab(Block block) {
        return block.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    protected static BlockState pillar(Block block, Direction.Axis axis) {
        return block.getDefaultState().with(PillarBlock.AXIS, axis);
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

    protected static BlockState lit(BlockState state) {
        return state.contains(net.minecraft.state.property.Properties.LIT)
                ? state.with(net.minecraft.state.property.Properties.LIT, true) : state;
    }

    /** The fortress stonework: mostly deepslate bricks, a few cracked or tiled for character. */
    protected static Block masonry(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.08F ? Blocks.CRACKED_DEEPSLATE_BRICKS : r < 0.16F ? Blocks.DEEPSLATE_TILES : Blocks.DEEPSLATE_BRICKS;
    }

    /**
     * Places blocks, containers and entities for one chunk's slice of a piece. Everything takes
     * local coordinates; anything outside the chunk is skipped, but random draws happen either way.
     */
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
            return FortressPiece.this.offsetPos(x, y, z);
        }

        /** Turns a direction written facing north into the piece's real direction. */
        Direction dir(Direction local) {
            if (local.getAxis() == Direction.Axis.Y) {
                return local;
            }
            return FortressPiece.this.getRotation().rotate(FortressPiece.this.getMirror().apply(local));
        }

        boolean chance(float p) {
            return this.random.nextFloat() < p;
        }

        <T> T pick(T[] options) {
            return options[this.random.nextInt(options.length)];
        }

        void set(int x, int y, int z, BlockState state) {
            FortressPiece.this.addBlock(this.world, state, x, y, z, this.chunkBox);
            Block block = state.getBlock();
            // Shapes that join up with their neighbours get fixed once the chunk is finished.
            if (block instanceof PaneBlock || block instanceof StairsBlock || block instanceof WallBlock
                    || block == Blocks.CHAIN || block instanceof net.minecraft.block.FenceBlock) {
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

        /**
         * The usual room: the whole box walled in fortress masonry, a floor at y = 0 propped up
         * from below, and the inside hollowed out.
         */
        void shell(int width, int height, int depth, BlockState floor) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    this.foundation(x, z);
                    boolean edge = x == 0 || z == 0 || x == width - 1 || z == depth - 1;
                    this.set(x, 0, z, edge ? Blocks.POLISHED_DEEPSLATE.getDefaultState() : floor);
                    for (int y = 1; y < height; y++) {
                        if (edge || y == height - 1) {
                            this.set(x, y, z, y == 1 && edge ? Blocks.POLISHED_BLACKSTONE_BRICKS : masonry(this));
                        } else {
                            this.set(x, y, z, Blocks.AIR);
                        }
                    }
                }
            }
        }

        /** Props the floor up with cobbled deepslate wherever there's air or liquid under it. */
        void foundation(int x, int z) {
            BlockPos.Mutable pos = this.pos(x, -1, z).mutableCopy();
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            for (int i = 0; i < MAX_FOUNDATION && pos.getY() > this.world.getBottomY(); i++) {
                BlockState state = this.world.getBlockState(pos);
                if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()) {
                    return;
                }
                this.world.setBlockState(pos, Blocks.COBBLED_DEEPSLATE.getDefaultState(), 2);
                pos.move(Direction.DOWN);
            }
        }

        /** A doorway through a wall at z, centred on x, in a basalt and gilded blackstone frame. */
        void doorway(int cx, int z, int width, int height) {
            int x1 = cx - width / 2;
            int x2 = x1 + width - 1;
            this.air(x1, 1, z, x2, height, z);
            this.fill(x1 - 1, 1, z, x1 - 1, height, z, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
            this.fill(x2 + 1, 1, z, x2 + 1, height, z, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
            this.fill(x1 - 1, height + 1, z, x2 + 1, height + 1, z, Blocks.GILDED_BLACKSTONE);
            if (width >= 3) {
                this.set(x1, height, z, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true));
                this.set(x2, height, z, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true));
            }
        }

        @Nullable
        BlockState get(int x, int y, int z) {
            BlockPos pos = this.pos(x, y, z);
            return this.chunkBox.contains(pos) ? this.world.getBlockState(pos) : null;
        }

        /** A pillar of polished deepslate on a chiselled base with a gold capital. */
        void column(int x, int z, int top) {
            this.set(x, 1, z, Blocks.CHISELED_DEEPSLATE);
            this.set(x, 2, z, Blocks.GILDED_BLACKSTONE);
            this.fill(x, 3, z, x, top - 2, z, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
            this.set(x, top - 1, z, Blocks.GOLD_BLOCK);
            this.set(x, top, z, Blocks.CHISELED_DEEPSLATE);
        }

        /**
         * A chandelier hanging from the ceiling at y = top: a chain, a gold boss and four arms
         * with candles on top and lanterns beneath.
         */
        void chandelier(int x, int top, int z, int drop) {
            for (int y = top - 1; y > top - drop; y--) {
                this.set(x, y, z, Blocks.CHAIN);
            }
            int y = top - drop;
            this.set(x, y, z, Blocks.GOLD_BLOCK);
            this.set(x, y - 1, z, lantern(true));
            for (Direction d : Direction.Type.HORIZONTAL) {
                int ax = x + d.getOffsetX();
                int az = z + d.getOffsetZ();
                this.set(ax, y, az, bottomSlab(Blocks.POLISHED_BLACKSTONE_SLAB));
                this.set(ax, y + 1, az, this.candleState());
                int ox = x + d.getOffsetX() * 2;
                int oz = z + d.getOffsetZ() * 2;
                this.set(ox, y, oz, bottomSlab(Blocks.POLISHED_BLACKSTONE_SLAB));
                this.set(ox, y - 1, oz, lantern(true));
            }
        }

        BlockState candleState() {
            return Blocks.YELLOW_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 1 + this.random.nextInt(3))
                    .with(CandleBlock.LIT, true);
        }

        /** A wall banner on the wall behind it, hanging from y down to y - 1 (banners are two tall). */
        void banner(int x, int y, int z, Direction facing, DyeColor color) {
            Block banner = Registries.BLOCK.get(new Identifier(color.getName() + "_wall_banner"));
            this.set(x, y, z, banner.getDefaultState().with(WallBannerBlock.FACING, facing));
        }

        /** A brazier: a lit campfire on a gilded plinth. */
        void brazier(int x, int y, int z) {
            this.set(x, y, z, Blocks.GILDED_BLACKSTONE);
            this.set(x, y + 1, z, Blocks.CAMPFIRE);
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

        void barrel(int x, int y, int z, Direction facing, Identifier lootTable) {
            this.container(x, y, z, facing(Blocks.BARREL, facing), lootTable);
        }

        /** An item frame on the face of the block behind it (or lying on the block below, facing up). */
        void frame(int x, int y, int z, Direction facing, ItemStack stack) {
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            ItemFrameEntity frame = new ItemFrameEntity(this.world.toServerWorld(), pos, this.dir(facing));
            frame.setSilent(true);
            frame.setHeldItemStack(stack, false);
            frame.setSilent(false);
            this.world.spawnEntity(frame);
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

        /** A chest minecart on the rail at x/y/z, filled from a loot table. */
        void chestCart(int x, int y, int z, Identifier lootTable) {
            long lootSeed = this.random.nextLong();
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            ChestMinecartEntity cart = new ChestMinecartEntity(this.world.toServerWorld(), pos.getX() + 0.5D, pos.getY() + 0.0625D, pos.getZ() + 0.5D);
            cart.setLootTable(lootTable, lootSeed);
            this.world.spawnEntity(cart);
        }

        void dwarf(int x, int y, int z) {
            this.dwarf(x, y, z, dwarf -> {
            });
        }

        /**
         * A dwarf living here. {@code setup} runs before it's added to the world: worldgen saves
         * entities straight away, so later changes would be lost.
         */
        void dwarf(int x, int y, int z, java.util.function.Consumer<MountainDwarfEntity> setup) {
            float yaw = this.random.nextFloat() * 360.0F;
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            MountainDwarfEntity dwarf = ModEntityTypes.MOUNTAIN_DWARF.create(this.world.toServerWorld());
            if (dwarf == null) {
                return;
            }
            dwarf.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
            dwarf.initialize(this.world, this.world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
            dwarf.setHome(pos);
            setup.accept(dwarf);
            this.world.spawnEntityAndPassengers(dwarf);
        }
    }
}
