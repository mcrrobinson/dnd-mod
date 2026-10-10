package mattonfire.dnd.world.gen.village;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.HobbitEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ItemFrameEntity;
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

/**
 * Base for the hobbit village pieces. Pieces are designed in local coordinates: x runs across the
 * front, z runs from the front edge (z = 0, where the entrance is) to the back, and y = 0 is the
 * ground layer. Block states are written as if the piece faced north, so "out of the front" is
 * {@link Direction#SOUTH}, and get rotated to the piece's real facing.
 *
 * A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed
 * (never the per-chunk random), which keeps every chunk's slice of it in agreement.
 */
public abstract class HobbitPiece extends StructurePiece {
    protected static final Direction OUT = Direction.SOUTH;
    protected static final Direction IN = Direction.NORTH;

    protected static final Identifier PANTRY = loot("hobbit_pantry");
    protected static final Identifier LARDER = loot("hobbit_larder");
    protected static final Identifier HARVEST = loot("hobbit_harvest");
    protected static final Identifier ALE = loot("hobbit_ale");

    private final long seed;

    protected HobbitPiece(StructurePieceType type, BlockBox box, Direction facing, long seed) {
        super(type, 0, box);
        this.seed = seed;
        this.setOrientation(facing);
    }

    protected HobbitPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.seed = nbt.getLong("Seed");
    }

    private static Identifier loot(String name) {
        return new Identifier(DnDClasses.MOD_ID, "chests/" + name);
    }

    /**
     * The ground layer (local y = 0) sits one below the box: terrain adaptation levels the land
     * so its top block is just under a piece's box, which is then exactly our ground layer.
     */
    @Override
    protected int applyYTransform(int y) {
        return super.applyYTransform(y) - 1;
    }

    /** A box of the given local size (width across the front, depth front to back), centred on x/z. */
    protected static BlockBox centeredBox(int x, int y, int z, Direction facing, int width, int height, int depth) {
        int sizeX = facing.getAxis() == Direction.Axis.Z ? width : depth;
        int sizeZ = facing.getAxis() == Direction.Axis.Z ? depth : width;
        int minX = x - sizeX / 2;
        int minZ = z - sizeZ / 2;
        return new BlockBox(minX, y, minZ, minX + sizeX - 1, y + height - 1, minZ + sizeZ - 1);
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

    /** Local x of the entrance on the front edge; paths lead to the block just in front of it. */
    protected int entranceX() {
        return this.width() / 2;
    }

    public BlockPos entrance() {
        int x = this.entranceX();
        return new BlockPos(this.applyXTransform(x, -1), this.applyYTransform(0), this.applyZTransform(x, -1));
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

    protected static BlockState log(Block block, Direction.Axis axis) {
        return block.getDefaultState().with(PillarBlock.AXIS, axis);
    }

    protected static BlockState leaves(Block block) {
        return block.getDefaultState().with(LeavesBlock.PERSISTENT, true);
    }

    protected static BlockState lantern(boolean hanging) {
        return Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, hanging);
    }

    protected static BlockState ripe(Block crop) {
        return ((CropBlock) crop).withAge(((CropBlock) crop).getMaxAge());
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

    private static final Block[] FLOWERS = {
            Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.ALLIUM, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET,
            Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.PINK_TULIP, Blocks.WHITE_TULIP, Blocks.LILY_OF_THE_VALLEY
    };
    private static final Block[] TALL_FLOWERS = {Blocks.ROSE_BUSH, Blocks.PEONY, Blocks.LILAC, Blocks.SUNFLOWER};
    private static final Block[] POTTED = {
            Blocks.POTTED_RED_TULIP, Blocks.POTTED_FERN, Blocks.POTTED_FLOWERING_AZALEA_BUSH, Blocks.POTTED_AZALEA_BUSH,
            Blocks.POTTED_POPPY, Blocks.POTTED_CORNFLOWER, Blocks.POTTED_LILY_OF_THE_VALLEY, Blocks.POTTED_DANDELION
    };
    private static final Block[] CANDLES = {Blocks.CANDLE, Blocks.WHITE_CANDLE, Blocks.YELLOW_CANDLE, Blocks.ORANGE_CANDLE};

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
            return HobbitPiece.this.offsetPos(x, y, z);
        }

        boolean contains(int x, int y, int z) {
            return this.chunkBox.contains(this.pos(x, y, z));
        }

        /** Turns a direction written facing north into the piece's real direction. */
        Direction dir(Direction local) {
            if (local.getAxis() == Direction.Axis.Y) {
                return local;
            }
            return HobbitPiece.this.getRotation().rotate(HobbitPiece.this.getMirror().apply(local));
        }

        boolean chance(float p) {
            return this.random.nextFloat() < p;
        }

        <T> T pick(T[] options) {
            return options[this.random.nextInt(options.length)];
        }

        void set(int x, int y, int z, BlockState state) {
            HobbitPiece.this.addBlock(this.world, state, x, y, z, this.chunkBox);
            Block block = state.getBlock();
            // Shapes that join up with their neighbours get fixed once the chunk is finished.
            if (block instanceof PaneBlock || block instanceof StairsBlock || block instanceof WallBlock) {
                BlockPos pos = this.pos(x, y, z);
                if (this.chunkBox.contains(pos)) {
                    this.world.getChunk(pos).markBlockForPostProcessing(pos);
                }
            }
        }

        void set(int x, int y, int z, Block block) {
            this.set(x, y, z, block.getDefaultState());
        }

        BlockState get(int x, int y, int z) {
            return HobbitPiece.this.getBlockAt(this.world, x, y, z, this.chunkBox);
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

        /** Sets the ground block at y = 0 and fills any gap beneath it with dirt. */
        void ground(int x, int z, BlockState top) {
            this.set(x, 0, z, top);
            HobbitPiece.this.fillDownwards(this.world, Blocks.DIRT.getDefaultState(), x, -1, z, this.chunkBox);
        }

        void ground(int x, int z, Block top) {
            this.ground(x, z, top.getDefaultState());
        }

        /** Grass and dirt ground over a whole rectangle, with the air above it cleared. */
        void lawn(int x1, int z1, int x2, int z2, int clearHeight) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    this.ground(x, z, Blocks.GRASS_BLOCK);
                    this.air(x, 1, z, x, clearHeight, z);
                }
            }
        }

        void flower(int x, int y, int z) {
            if (this.chance(0.15F)) {
                this.tall(x, y, z, this.pick(TALL_FLOWERS));
            } else {
                this.set(x, y, z, this.pick(FLOWERS));
            }
        }

        void tall(int x, int y, int z, Block block) {
            this.set(x, y, z, block.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
            this.set(x, y + 1, z, block.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
        }

        void pot(int x, int y, int z) {
            this.set(x, y, z, this.pick(POTTED));
        }

        void candles(int x, int y, int z) {
            this.set(x, y, z, this.pick(CANDLES).getDefaultState()
                    .with(CandleBlock.CANDLES, 1 + this.random.nextInt(3)).with(CandleBlock.LIT, true));
        }

        /**
         * A round window centred on x/y/z: a plus of glass with its corners rounded off by stairs.
         * {@code alongX} for a wall running along x (constant z), otherwise along z.
         */
        void roundWindow(int x, int y, int z, boolean alongX) {
            int dx = alongX ? 1 : 0;
            int dz = alongX ? 0 : 1;
            Direction low = alongX ? Direction.WEST : Direction.SOUTH;
            Direction high = alongX ? Direction.EAST : Direction.NORTH;
            this.set(x, y, z, Blocks.GLASS_PANE);
            this.set(x - dx, y, z - dz, Blocks.GLASS_PANE);
            this.set(x + dx, y, z + dz, Blocks.GLASS_PANE);
            this.set(x, y - 1, z, Blocks.GLASS_PANE);
            this.set(x, y + 1, z, Blocks.GLASS_PANE);
            this.set(x - dx, y + 1, z - dz, stairs(Blocks.SPRUCE_STAIRS, low, true));
            this.set(x + dx, y + 1, z + dz, stairs(Blocks.SPRUCE_STAIRS, high, true));
            this.set(x - dx, y - 1, z - dz, stairs(Blocks.SPRUCE_STAIRS, low, false));
            this.set(x + dx, y - 1, z + dz, stairs(Blocks.SPRUCE_STAIRS, high, false));
        }

        /** A round door: a coloured 3x3 disc with the real door in its middle, in a wooden ring. */
        void roundDoor(int x, int y, int z, Block door, Block planks) {
            this.fill(x - 1, y, z, x + 1, y + 2, z, planks);
            this.door(x, y, z, door, IN);
            this.fill(x - 2, y, z, x - 2, y + 2, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y));
            this.fill(x + 2, y, z, x + 2, y + 2, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y));
            this.fill(x - 1, y + 3, z, x + 1, y + 3, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            this.set(x - 2, y + 3, z, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, true));
            this.set(x + 2, y + 3, z, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, true));
            // A knob on the right-hand panel.
            this.set(x + 1, y + 1, z - 1, Blocks.POLISHED_BLACKSTONE_BUTTON.getDefaultState()
                    .with(net.minecraft.block.WallMountedBlock.FACING, OUT));
        }

        void door(int x, int y, int z, Block door, Direction facing) {
            BlockState state = door.getDefaultState().with(DoorBlock.FACING, facing).with(DoorBlock.HINGE, DoorHinge.LEFT);
            this.set(x, y, z, state.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            this.set(x, y + 1, z, state.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }

        void gate(int x, int y, int z, Block gate, Direction facing) {
            this.set(x, y, z, gate.getDefaultState().with(FenceGateBlock.FACING, facing));
        }

        /** A bed with its foot at x/y/z and its head one block towards {@code facing}. */
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

        void barrel(int x, int y, int z, Direction facing, Identifier lootTable) {
            this.container(x, y, z, facing(Blocks.BARREL, facing), lootTable);
        }

        void chest(int x, int y, int z, Direction facing, Identifier lootTable) {
            this.container(x, y, z, facing(Blocks.CHEST, facing), lootTable);
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

        /** A plate of food on the table top below. */
        void plate(int x, int y, int z) {
            this.frame(x, y, z, Direction.UP, HobbitFood.plate(this.random));
        }

        /** Something tasty, or at least festive, for a table top. */
        void tableFood(int x, int y, int z) {
            int roll = this.random.nextInt(10);
            if (roll < 5) {
                this.plate(x, y, z);
            } else if (roll < 7) {
                this.set(x, y, z, Blocks.CAKE);
            } else if (roll < 8) {
                this.set(x, y, z, this.pick(new Block[]{Blocks.CANDLE_CAKE, Blocks.RED_CANDLE_CAKE, Blocks.YELLOW_CANDLE_CAKE})
                        .getDefaultState().with(CandleBlock.LIT, true));
            } else if (roll < 9) {
                this.candles(x, y, z);
            } else {
                this.pot(x, y, z);
            }
        }

        void hobbit(int x, int y, int z) {
            this.hobbit(x, y, z, ModEntityTypes.HOBBIT);
        }

        void hobbit(int x, int y, int z, EntityType<? extends HobbitEntity> type) {
            this.hobbit(x, y, z, type, hobbit -> {
            });
        }

        /** Places a hobbit and lets {@code setup} change it before it's added (e.g. the village elder). */
        void hobbit(int x, int y, int z, EntityType<? extends HobbitEntity> type,
                    java.util.function.Consumer<HobbitEntity> setup) {
            float yaw = this.random.nextFloat() * 360.0F;
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            HobbitEntity hobbit = type.create(this.world.toServerWorld());
            if (hobbit == null) {
                return;
            }
            hobbit.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
            hobbit.initialize(this.world, this.world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
            hobbit.setHome(pos);
            setup.accept(hobbit);
            this.world.spawnEntityAndPassengers(hobbit);
        }
    }
}
