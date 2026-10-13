package mattonfire.dnd.world.gen;

import java.util.function.Consumer;
import mattonfire.dnd.entity.MimicEntity;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.MobEntity;
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
 * Base for the mod's hand-built structure pieces. A piece is built once per chunk it overlaps, so all of its
 * randomness comes from its own seed (saved as {@code Seed}): {@link #generate} hands {@link #build} a fresh
 * {@link Builder} on that seed for every chunk, and the builder skips whatever lies outside the chunk but makes
 * its random draws either way, so every chunk sees the same piece.
 *
 * Subclasses give the builder its local coordinates ({@link Builder#pos}): {@link FacingPiece} for pieces turned
 * the vanilla way, {@link OrientedPiece} for pieces turned round a centre, or their own.
 *
 * @param <B> the subclass's own builder, with its structure's helpers
 */
public abstract class ModPiece<B extends ModPiece<B>.Builder> extends StructurePiece {
    protected final long seed;

    protected ModPiece(StructurePieceType type, BlockBox box, long seed) {
        super(type, 0, box);
        this.seed = seed;
    }

    protected ModPiece(StructurePieceType type, NbtCompound nbt) {
        super(type, nbt);
        this.seed = nbt.getLong("Seed");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putLong("Seed", this.seed);
    }

    @Override
    public final void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                               Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        B b = this.builder(world, chunkBox, Random.create(this.seed));
        this.build(b);
        this.finish(b);
    }

    /** A builder for one chunk's slice of this piece. */
    protected abstract B builder(StructureWorldAccess world, BlockBox chunkBox, Random random);

    protected abstract void build(B b);

    /** Runs after {@link #build}, for anything that needs the whole build done first. */
    protected void finish(B b) {
    }

    /**
     * Places blocks, containers and entities for one chunk's slice of a piece. Everything takes local
     * coordinates; anything outside the chunk is skipped, but random draws happen either way.
     */
    public abstract class Builder {
        public final StructureWorldAccess world;
        public final BlockBox chunkBox;
        public final Random random;

        protected Builder(StructureWorldAccess world, BlockBox chunkBox, Random random) {
            this.world = world;
            this.chunkBox = chunkBox;
            this.random = random;
        }

        /** The world position of local x/y/z. */
        public abstract BlockPos pos(int x, int y, int z);

        /** Turns a local direction into the real one (unchanged unless the piece is turned). */
        public Direction dir(Direction local) {
            return local;
        }

        public boolean inChunk(int x, int y, int z) {
            return this.chunkBox.contains(this.pos(x, y, z));
        }

        public boolean chance(float p) {
            return this.random.nextFloat() < p;
        }

        public <T> T pick(T[] options) {
            return options[this.random.nextInt(options.length)];
        }

        public void set(int x, int y, int z, BlockState state) {
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            this.place(x, y, z, pos, state);
            // Shapes that join up with their neighbours get fixed once the chunk is finished.
            if (this.joinsNeighbours(state.getBlock())) {
                this.world.getChunk(pos).markBlockForPostProcessing(pos);
            }
        }

        /** Writes {@code state} at local x/y/z, world {@code pos} (in this chunk). */
        protected void place(int x, int y, int z, BlockPos pos, BlockState state) {
            this.world.setBlockState(pos, state, PieceBlocks.UPDATE);
        }

        /** Whether a block placed by {@link #set} is marked for post-processing. */
        protected boolean joinsNeighbours(Block block) {
            return PieceBlocks.joinsNeighbours(block);
        }

        public void set(int x, int y, int z, Block block) {
            this.set(x, y, z, block.getDefaultState());
        }

        public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                        this.set(x, y, z, state);
                    }
                }
            }
        }

        public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
            this.fill(x1, y1, z1, x2, y2, z2, block.getDefaultState());
        }

        public void air(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.fill(x1, y1, z1, x2, y2, z2, Blocks.AIR);
        }

        /** The block at local x/y/z, or null outside this chunk. */
        @Nullable
        public BlockState get(int x, int y, int z) {
            BlockPos pos = this.pos(x, y, z);
            return this.chunkBox.contains(pos) ? this.world.getBlockState(pos) : null;
        }

        /**
         * Props local x/z up with {@code fill} from below y down to solid ground, through air, liquids and
         * replaceable blocks (and leaves, if {@code throughLeaves}), at most {@code max} blocks. Returns how many
         * blocks it placed when it reached ground, -1 if it didn't, or 0 if the column isn't in this chunk.
         */
        public int propUp(int x, int y, int z, BlockState fill, int max, boolean throughLeaves) {
            BlockPos.Mutable pos = this.pos(x, y - 1, z).mutableCopy();
            if (!this.chunkBox.contains(pos)) {
                return 0;
            }
            for (int i = 0; i < max && pos.getY() > this.world.getBottomY(); i++) {
                BlockState state = this.world.getBlockState(pos);
                if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()
                        && !(throughLeaves && state.getBlock() instanceof LeavesBlock)) {
                    return i;
                }
                this.world.setBlockState(pos, fill, PieceBlocks.UPDATE);
                pos.move(Direction.DOWN);
            }
            return -1;
        }

        /** A chest or barrel filled from a loot table when first opened. */
        public void container(int x, int y, int z, BlockState state, Identifier lootTable) {
            long lootSeed = this.random.nextLong();
            this.set(x, y, z, state);
            BlockPos pos = this.pos(x, y, z);
            if (this.chunkBox.contains(pos) && this.world.getBlockEntity(pos) instanceof LootableContainerBlockEntity container) {
                container.setLootTable(lootTable, lootSeed);
            }
        }

        public void chest(int x, int y, int z, Direction facing, Identifier lootTable) {
            this.container(x, y, z, PieceBlocks.facing(Blocks.CHEST, facing), lootTable);
        }

        public void barrel(int x, int y, int z, Direction facing, Identifier lootTable) {
            this.container(x, y, z, PieceBlocks.facing(Blocks.BARREL, facing), lootTable);
        }

        /** A mimic sitting where a chest would be, holding that chest's loot. */
        public void mimic(int x, int y, int z, Direction facing, Identifier lootTable) {
            this.set(x, y, z, Blocks.AIR);
            BlockPos pos = this.pos(x, y, z);
            if (!this.chunkBox.contains(pos)) {
                return;
            }
            MimicEntity mimic = MimicEntity.disguised(this.world.toServerWorld(), pos, this.dir(facing), lootTable);
            if (mimic != null) {
                this.world.spawnEntity(mimic);
            }
        }

        /** A wall banner on the wall behind it (banners are two tall: it hangs from y down to y - 1). */
        public void banner(int x, int y, int z, Direction facing, DyeColor color) {
            Block banner = Registries.BLOCK.get(new Identifier(color.getName() + "_wall_banner"));
            this.set(x, y, z, banner.getDefaultState().with(WallBannerBlock.FACING, facing));
        }

        /** A bed with its foot at x/y/z and its head one block towards {@code facing} (local). */
        public void bed(int x, int y, int z, Direction facing, DyeColor color) {
            Block bed = Registries.BLOCK.get(new Identifier(color.getName() + "_bed"));
            BlockState state = bed.getDefaultState().with(BedBlock.FACING, facing);
            this.set(x, y, z, state.with(BedBlock.PART, BedPart.FOOT));
            this.set(x + facing.getOffsetX(), y, z - facing.getOffsetZ(), state.with(BedBlock.PART, BedPart.HEAD));
        }

        /** A beehive dripping with honey, with a few bees living in it. */
        public void beehive(int x, int y, int z, Direction facing, int bees) {
            this.set(x, y, z, PieceBlocks.facing(Blocks.BEEHIVE, facing).with(BeehiveBlock.HONEY_LEVEL, 5));
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
        public void frame(int x, int y, int z, Direction facing, ItemStack stack) {
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
        public void armorStand(int x, int y, int z, Direction facing, ItemStack... armor) {
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

        /**
         * A mob of {@code type} living here, facing a random way. {@code setup} (its home and so on) runs before
         * it's added to the world: worldgen saves entities straight away, so later changes would be lost.
         */
        public <E extends MobEntity> void npc(int x, int y, int z, EntityType<E> type, Consumer<? super E> setup) {
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
            setup.accept(npc);
            this.world.spawnEntityAndPassengers(npc);
        }
    }
}
