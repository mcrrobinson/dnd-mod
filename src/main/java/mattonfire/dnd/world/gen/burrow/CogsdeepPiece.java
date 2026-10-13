package mattonfire.dnd.world.gen.burrow;

import java.util.function.Consumer;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
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
 * Base for the Cogsdeep pieces, laid out like the Brimstone Sanctum's: local {@code a} runs across (east when
 * the observatory door faces south), local {@code b} runs from the back towards the door, and {@code (0, 0)} is
 * the axis of the spiral stair. Each piece has its own reference height ({@link #center}'s y): the burrow's
 * floor for the rooms down below (y = 0 is where you stand, the floor blocks are at y = -1), the ground for the
 * observatory. Block states are written as if the door faced south and turned with {@link BlockState#rotate}.
 *
 * <p>A piece is built once per chunk it overlaps, so all of its randomness comes from its own seed, and every
 * draw happens whether or not its block lands in the chunk. Each piece carries its burrow's key (the start
 * chunk, {@link ChunkPos#toLong}).
 */
public abstract class CogsdeepPiece extends StructurePiece {
    /** The chests and barrels of the two workshops. Not locked. */
    public static final Identifier WORKSHOP_LOOT = loot("cogsdeep_workshop");
    /** The gem-polishing room's chest (DC 10). */
    public static final Identifier GEM_LOOT = loot("cogsdeep_gems");
    /** The strongbox on the clocktower gallery (DC 15). */
    public static final Identifier VAULT_LOOT = loot("cogsdeep_vault");
    /** The chest in the Commons and the observatory's. Not locked. */
    public static final Identifier COMMONS_LOOT = loot("cogsdeep_commons");

    protected final BlockPos center;
    protected final Direction door;
    protected final long seed;
    protected final long burrow;

    protected CogsdeepPiece(StructurePieceType type, BlockPos center, Direction door, long seed, long burrow,
                            int a1, int y1, int b1, int a2, int y2, int b2) {
        super(type, 0, box(center, door, a1, y1, b1, a2, y2, b2));
        this.center = center;
        this.door = door;
        this.seed = seed;
        this.burrow = burrow;
    }

    protected CogsdeepPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        this.door = Direction.fromHorizontal(nbt.getInt("Door"));
        this.seed = nbt.getLong("Seed");
        this.burrow = nbt.getLong("Burrow");
    }

    private static Identifier loot(String name) {
        return new Identifier(DnDClasses.MOD_ID, "chests/" + name);
    }

    /** The rotation that turns "the door faces south" into the real door direction. */
    public static BlockRotation rotation(Direction door) {
        for (BlockRotation rotation : BlockRotation.values()) {
            if (rotation.rotate(Direction.SOUTH) == door) {
                return rotation;
            }
        }
        return BlockRotation.NONE;
    }

    /** The world position of local (a, y, b) round {@code center} with the observatory door towards {@code door}. */
    public static BlockPos at(BlockPos center, Direction door, int a, int y, int b) {
        return center.add(new BlockPos(a, y, b).rotate(rotation(door)));
    }

    static BlockBox box(BlockPos center, Direction door, int a1, int y1, int b1, int a2, int y2, int b2) {
        return BlockBox.create(at(center, door, a1, y1, b1), at(center, door, a2, y2, b2));
    }

    public BlockPos at(int a, int y, int b) {
        return at(this.center, this.door, a, y, b);
    }

    /** The piece's reference point: the stair axis at its floor (burrow rooms) or at the ground (observatory). */
    public BlockPos center() {
        return this.center;
    }

    public Direction door() {
        return this.door;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putInt("Door", this.door.getHorizontal());
        nbt.putLong("Seed", this.seed);
        nbt.putLong("Burrow", this.burrow);
    }

    @Override
    public final void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                               Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        this.build(new Builder(world, chunkBox, Random.create(this.seed)));
    }

    protected abstract void build(Builder b);

    // ---- Block state helpers (written as if the door faced south) ----

    protected static BlockState stairs(Block block, Direction facing, boolean upsideDown) {
        return block.getDefaultState().with(StairsBlock.FACING, facing)
                .with(StairsBlock.HALF, upsideDown ? BlockHalf.TOP : BlockHalf.BOTTOM);
    }

    protected static BlockState slab(Block block, boolean top) {
        return block.getDefaultState().with(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    protected static BlockState lantern(boolean hanging) {
        return Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, hanging);
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

    /** The burrow's stonework: stone bricks with some cracked, mossy and tuff blocks. */
    protected static Block masonry(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.07F ? Blocks.CRACKED_STONE_BRICKS : r < 0.13F ? Blocks.MOSSY_STONE_BRICKS
                : r < 0.17F ? Blocks.TUFF : Blocks.STONE_BRICKS;
    }

    /** Copper going green: mostly cut copper, some of it already exposed, weathered or oxidised. */
    protected static Block cutCopper(Builder b) {
        float r = b.random.nextFloat();
        return r < 0.45F ? Blocks.CUT_COPPER : r < 0.7F ? Blocks.EXPOSED_CUT_COPPER
                : r < 0.9F ? Blocks.WEATHERED_CUT_COPPER : Blocks.OXIDIZED_CUT_COPPER;
    }

    /** Places blocks, containers and folk for one chunk's slice of a piece, in local coordinates. */
    protected class Builder {
        final StructureWorldAccess world;
        final BlockBox chunkBox;
        final Random random;
        private final BlockRotation rotation;

        Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            this.world = world;
            this.chunkBox = chunkBox;
            this.random = random;
            this.rotation = rotation(CogsdeepPiece.this.door);
        }

        BlockPos pos(int a, int y, int b) {
            return CogsdeepPiece.this.at(a, y, b);
        }

        boolean chance(float p) {
            return this.random.nextFloat() < p;
        }

        <T> T pick(T[] options) {
            return options[this.random.nextInt(options.length)];
        }

        Direction dir(Direction local) {
            return this.rotation.rotate(local);
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

        BlockState get(int a, int y, int b) {
            return this.world.getBlockState(this.pos(a, y, b));
        }

        boolean inChunk(int a, int y, int b) {
            return this.chunkBox.contains(this.pos(a, y, b));
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
         * A room's shell: walls, floor and ceiling of masonry from (a1, -1, b1) to (a2, top, b2), and air inside.
         * The shell keeps out the water and gravel round it, so it's always built whole.
         */
        void room(int a1, int b1, int a2, int b2, int top, Block floor) {
            for (int y = -1; y <= top; y++) {
                for (int a = a1; a <= a2; a++) {
                    for (int b = b1; b <= b2; b++) {
                        boolean shell = y == -1 || y == top || a == a1 || a == a2 || b == b1 || b == b2;
                        if (!shell) {
                            this.set(a, y, b, Blocks.AIR);
                        } else if (y == -1 && a != a1 && a != a2 && b != b1 && b != b2) {
                            this.set(a, y, b, floor);
                        } else {
                            this.set(a, y, b, masonry(this));
                        }
                    }
                }
            }
        }

        /** A doorway {@code width} wide (odd) and {@code height} high through the wall at a/b, along the a or b axis. */
        void doorway(int a, int b, boolean alongA, int width, int height) {
            int half = width / 2;
            for (int i = -half; i <= half; i++) {
                for (int y = 0; y < height; y++) {
                    this.set(alongA ? a + i : a, y, alongA ? b : b + i, Blocks.AIR);
                }
            }
        }

        /**
         * Props local a/b up with {@code fill} from below y down to solid ground, through air, water and plants
         * (at most {@code max} blocks).
         */
        void foundation(int a, int y, int b, BlockState fill, int max) {
            BlockPos.Mutable pos = this.pos(a, y - 1, b).mutableCopy();
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            for (int i = 0; i < max && pos.getY() > this.world.getBottomY(); i++) {
                BlockState state = this.world.getBlockState(pos);
                if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()) {
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

        /** A lantern hanging on a chain {@code chain} long from the ceiling block above {@code top}. */
        void hangingLantern(int a, int top, int b, int chain) {
            for (int y = top; y > top - chain; y--) {
                this.set(a, y, b, Blocks.CHAIN);
            }
            this.set(a, top - chain, b, lantern(true));
        }

        /** An item frame on the face of the block behind it (or lying on the block below, facing up). */
        void frame(int a, int y, int b, Direction facing, ItemStack stack) {
            BlockPos pos = this.pos(a, y, b);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            ItemFrameEntity frame = new ItemFrameEntity(this.world.toServerWorld(), pos,
                    facing.getAxis().isVertical() ? facing : this.dir(facing));
            frame.setSilent(true);
            frame.setHeldItemStack(stack, false);
            frame.setSilent(false);
            this.world.spawnEntity(frame);
        }

        /**
         * A settlement NPC of {@code type} living here, keeping near this spot ({@code setup} sets its home and runs
         * before it's added: worldgen saves entities straight away, so later changes would be lost).
         */
        <E extends PathAwareEntity> void npc(int a, int y, int b, EntityType<E> type, Consumer<E> setup) {
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
            setup.accept(npc);
            this.world.spawnEntityAndPassengers(npc);
        }

        /** A Gnome Tinker keeping within {@code range} of this spot. */
        void tinker(int a, int y, int b, int range) {
            BlockPos home = this.pos(a, y, b);
            this.npc(a, y, b, ModEntityTypes.GNOME_TINKER, npc -> npc.setHome(home, range));
        }

        /** A Clockwork Sentinel keeping within {@code range} of this spot. */
        void sentinel(int a, int y, int b, int range) {
            BlockPos home = this.pos(a, y, b);
            this.npc(a, y, b, ModEntityTypes.CLOCKWORK_SENTINEL, npc -> npc.setHome(home, range));
        }
    }
}
