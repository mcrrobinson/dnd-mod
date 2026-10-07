package mattonfire.dnd.world.gen.lair;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.LightningChaserEntity;
import mattonfire.dnd.entity.MimicEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
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
 * The lair itself, built round its centre in world coordinates (it's round, so it has no facing).
 * Rings outwards from the middle: the nest and its hoard, a scorched stone floor, a band of
 * chiselled runes, then a low rim with eight standing stones on it, most still holding up a
 * lightning rod. The peak is cut flat above the floor, and wherever it falls away the floor is
 * held up by a cone of rock sloping out into the mountainside.
 *
 * Built once per chunk it overlaps, so all of its randomness comes from its own seed.
 */
public class LairPiece extends StructurePiece {
    private static final Identifier HOARD = new Identifier(DnDClasses.MOD_ID, "chests/dragon_lair");

    /** Radius of the floor, out to the outside of the rim. */
    private static final int RADIUS = 10;
    /** How far out the peak is cut away above the floor, so no rock overhangs the rim. */
    private static final int CLEAR = RADIUS + 3;
    /** How high above the floor the peak is cut away. */
    private static final int HEADROOM = 24;
    /** How far down the floor gets built up over the slopes. */
    private static final int MAX_FOUNDATION = 40;
    /** How far down the rock beneath the rim slopes away... */
    private static final int SKIRT = 18;
    /** ...dropping this many blocks for each block out, give or take a couple. */
    private static final double SLOPE = 1.6D;
    private static final int STONES = 8;
    /** Chance of a mimic lying in wait beside the hoard chest. */
    private static final float MIMIC_CHANCE = 0.5F;

    private final BlockPos center;
    private final long seed;

    public LairPiece(BlockPos center, long seed) {
        super(DragonLairStructures.LAIR, 0, new BlockBox(center.getX() - RADIUS - SKIRT, center.getY() - SKIRT,
                center.getZ() - RADIUS - SKIRT, center.getX() + RADIUS + SKIRT, center.getY() + HEADROOM, center.getZ() + RADIUS + SKIRT));
        this.center = center;
        this.seed = seed;
    }

    public LairPiece(NbtCompound nbt) {
        super(DragonLairStructures.LAIR, nbt);
        this.center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        this.seed = nbt.getLong("Seed");
    }

    public BlockPos getCenter() {
        return this.center;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putLong("Seed", this.seed);
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random chunkRandom, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        Random random = Random.create(this.seed);
        int cx = this.center.getX();
        int y0 = this.center.getY();
        int cz = this.center.getZ();

        // Ground, floor and the open sky above it. Every column draws the same randoms whether
        // or not it's in this chunk, so the pattern matches across chunk borders.
        int reach = RADIUS + SKIRT;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                float r = random.nextFloat();
                int rough = random.nextInt(3);
                BlockPos pos = new BlockPos(cx + dx, y0, cz + dz);
                if (d > RADIUS + 0.5D) {
                    // Rough rock sloping away beneath the rim, and the sky cleared just round it.
                    int drop = (int) Math.ceil((d - RADIUS - 0.5D) * SLOPE) + rough;
                    if (drop <= SKIRT) {
                        foundation(world, chunkBox, pos.down(drop), random.nextFloat());
                    }
                    if (d <= CLEAR + 0.5D) {
                        for (int y = 1 - drop; y <= HEADROOM; y++) {
                            set(world, chunkBox, pos.up(y), Blocks.AIR.getDefaultState());
                        }
                    }
                    continue;
                }
                for (int y = 1; y <= HEADROOM; y++) {
                    set(world, chunkBox, pos.up(y), Blocks.AIR.getDefaultState());
                }
                foundation(world, chunkBox, pos.down(), random.nextFloat());
                set(world, chunkBox, pos, floor(d, r));
                if (d > RADIUS - 1.5D) {
                    // The rim: a low wall of weathered brick.
                    set(world, chunkBox, pos.up(), (r < 0.3F ? Blocks.CRACKED_STONE_BRICKS : r < 0.45F ? Blocks.MOSSY_STONE_BRICKS
                            : Blocks.STONE_BRICKS).getDefaultState());
                }
            }
        }

        // Standing stones on the rim, each crowned with a lightning rod unless it's fallen.
        double turn = random.nextDouble() * Math.PI * 2.0D;
        for (int i = 0; i < STONES; i++) {
            double angle = turn + Math.PI * 2.0D * i / STONES;
            int sx = cx + (int) Math.round(Math.cos(angle) * (RADIUS - 1));
            int sz = cz + (int) Math.round(Math.sin(angle) * (RADIUS - 1));
            boolean broken = random.nextFloat() < 0.25F;
            int height = broken ? 2 + random.nextInt(2) : 5 + random.nextInt(3);
            for (int y = 1; y <= height; y++) {
                float r = random.nextFloat();
                BlockState stone = y == 1 ? Blocks.CHISELED_STONE_BRICKS.getDefaultState()
                        : r < 0.2F ? Blocks.CRACKED_STONE_BRICKS.getDefaultState()
                        : Blocks.POLISHED_ANDESITE.getDefaultState();
                set(world, chunkBox, new BlockPos(sx, y0 + y, sz), stone);
            }
            if (!broken) {
                Block cap = random.nextBoolean() ? Blocks.OXIDIZED_CUT_COPPER : Blocks.WEATHERED_CUT_COPPER;
                set(world, chunkBox, new BlockPos(sx, y0 + height + 1, sz), cap.getDefaultState());
                set(world, chunkBox, new BlockPos(sx, y0 + height + 2, sz), Blocks.LIGHTNING_ROD.getDefaultState());
            }
        }

        // The nest: a ring of tangled logs and old bones, heaped with gold round a chest.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                float r = random.nextFloat();
                int h = random.nextInt(3);
                BlockPos pos = new BlockPos(cx + dx, y0, cz + dz);
                if (d > 2.5D && d <= 3.6D) {
                    set(world, chunkBox, pos.up(), nestLog(dx, dz, r));
                    if (h > 0) {
                        set(world, chunkBox, pos.up(2), r < 0.5F ? Blocks.BONE_BLOCK.getDefaultState() : nestLog(dz, dx, r));
                    }
                } else if (d <= 2.5D && (dx != 0 || dz != 0) && r < 0.45F) {
                    set(world, chunkBox, pos.up(), (r < 0.08F ? Blocks.RAW_GOLD_BLOCK : r < 0.2F ? Blocks.GOLD_BLOCK
                            : r < 0.3F ? Blocks.RAW_COPPER_BLOCK : Blocks.HAY_BLOCK).getDefaultState());
                }
            }
        }
        long lootSeed = random.nextLong();
        BlockPos chest = this.center.up();
        set(world, chunkBox, chest, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.Type.HORIZONTAL.random(random)));
        if (chunkBox.contains(chest) && world.getBlockEntity(chest) instanceof LootableContainerBlockEntity container) {
            container.setLootTable(HOARD, lootSeed);
        }

        // And its keeper, or a pair of them.
        int dragons = random.nextFloat() < 0.3F ? 2 : 1;
        for (int i = 0; i < dragons; i++) {
            BlockPos at = this.center.add(i == 0 ? 5 : -5, 1, 0);
            float yaw = random.nextFloat() * 360.0F;
            if (chunkBox.contains(at)) {
                spawnDragon(world, at, yaw);
            }
        }

        // Sometimes a second chest sits beside the hoard. It isn't a chest.
        if (random.nextFloat() < MIMIC_CHANCE) {
            Direction side = Direction.Type.HORIZONTAL.random(random);
            BlockPos at = chest.offset(side, 2);
            if (chunkBox.contains(at)) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                world.setBlockState(at.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                MimicEntity mimic = MimicEntity.disguised(world.toServerWorld(), at, side, HOARD);
                if (mimic != null) {
                    world.spawnEntity(mimic);
                }
            }
        }
    }

    private void spawnDragon(StructureWorldAccess world, BlockPos at, float yaw) {
        LightningChaserEntity dragon = ModEntityTypes.LIGHTNING_CHASER.create(world.toServerWorld());
        if (dragon == null) {
            return;
        }
        dragon.refreshPositionAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, yaw, 0.0F);
        dragon.initialize(world, world.getLocalDifficulty(at), SpawnReason.STRUCTURE, null, null);
        dragon.setLair(this.center);
        dragon.setPersistent();
        world.spawnEntityAndPassengers(dragon);
    }

    /** The floor at distance d from the middle: straw in the nest, scorched stone, a ring of runes. */
    private static BlockState floor(double d, float r) {
        if (d <= 2.5D) {
            return (r < 0.5F ? Blocks.COARSE_DIRT : Blocks.PACKED_MUD).getDefaultState();
        }
        if (d > 6.5D && d <= 7.5D) {
            return (r < 0.25F ? Blocks.CHISELED_DEEPSLATE : Blocks.DEEPSLATE_TILES).getDefaultState();
        }
        // Scorch marks where the lightning has come down.
        if (r < 0.1F) {
            return Blocks.BLACKSTONE.getDefaultState();
        }
        if (r < 0.16F) {
            return Blocks.SMOOTH_BASALT.getDefaultState();
        }
        return (r < 0.45F ? Blocks.ANDESITE : r < 0.7F ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS).getDefaultState();
    }

    private static BlockState nestLog(int dx, int dz, float r) {
        Block log = r < 0.6F ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_LOG;
        // Lying round the ring, more or less.
        Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.Z : Direction.Axis.X;
        return log.getDefaultState().with(PillarBlock.AXIS, axis);
    }

    private static void set(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, state, 2);
        }
    }

    /**
     * Fills rock down from {@code top} wherever there's air, snow or liquid, until it meets the
     * mountain. {@code r} picks the odd patch of andesite or gravel in the surface.
     */
    private static void foundation(StructureWorldAccess world, BlockBox chunkBox, BlockPos top, float r) {
        if (!chunkBox.contains(top)) {
            return;
        }
        BlockPos.Mutable pos = top.mutableCopy();
        for (int i = 0; i < MAX_FOUNDATION && pos.getY() > world.getBottomY(); i++) {
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()) {
                return;
            }
            Block rock = i > 0 ? Blocks.STONE : r < 0.15F ? Blocks.ANDESITE : r < 0.22F ? Blocks.GRAVEL : Blocks.STONE;
            world.setBlockState(pos, rock.getDefaultState(), 2);
            pos.move(Direction.DOWN);
        }
    }
}
