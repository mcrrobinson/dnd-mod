package mattonfire.dnd.world.gen.lair;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.classes.Obstacles.StructureObstacles;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Registry.DwarvenBlocks;
import mattonfire.dnd.classes.Registry.LairBlocks;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.LairDragonEntity;
import mattonfire.dnd.entity.MimicEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.EntityType;
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
 * <p>
 * A {@link Kind#FROST} lair (the Frost Drake's, on Frozen Peaks) has the same shape in snow and
 * ice: a snowy floor with a ring of blue ice, a rim of packed ice and snow, ice spires instead of
 * the standing stones, and a snow-filled nest heaped with gold and diamond blocks.
 * <p>
 * Both are dressed with the lair building blocks: a woven Dragon Nest floor heaped with Treasury Coin Piles and the
 * dragon's shed scales; Fulgurite and Fulgurite Spikes where the lightning struck, and Storm Glass in the standing
 * stones; Rimestone in the frost lair's rim and spires, and Hoarfrost Crystals round the nest and on the spires.
 * Where they go is picked by {@link #noise}, a hash of the position, not the piece's random, so the random sequence
 * (and so the layout of half-generated lairs from older versions) is unchanged.
 * <p>
 * Against the rim, north or south of the nest (the dragons stand east and west), a deepslate cairn holds a
 * second hoard chest behind a 2x2x2 Heavy Boulder (Hard): only a Barbarian heaves it aside, anyone else
 * blows it up with TNT.
 *
 * Built once per chunk it overlaps, so all of its randomness comes from its own seed.
 */
public class LairPiece extends StructurePiece {
    /** Which dragon's lair: the Lightning Chaser's (the original, and the default in old saves) or the Frost Drake's. */
    public enum Kind {
        STORM("chests/dragon_lair"),
        FROST("chests/frost_lair");

        final Identifier hoard;

        Kind(String hoard) {
            this.hoard = new Identifier(DnDClasses.MOD_ID, hoard);
        }

        EntityType<? extends LairDragonEntity> dragon() {
            return this == FROST ? ModEntityTypes.FROST_DRAKE : ModEntityTypes.LIGHTNING_CHASER;
        }
    }

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
    /** The side cache's cairn runs from this far out from the nest... */
    private static final int CAIRN_OUT = 5;
    /** ...to this far, against the rim. */
    private static final int CAIRN_BACK = 9;

    private final BlockPos center;
    private final long seed;
    private final Kind kind;

    public LairPiece(BlockPos center, long seed, Kind kind) {
        super(DragonLairStructures.LAIR, 0, new BlockBox(center.getX() - RADIUS - SKIRT, center.getY() - SKIRT,
                center.getZ() - RADIUS - SKIRT, center.getX() + RADIUS + SKIRT, center.getY() + HEADROOM, center.getZ() + RADIUS + SKIRT));
        this.center = center;
        this.seed = seed;
        this.kind = kind;
    }

    public LairPiece(NbtCompound nbt) {
        super(DragonLairStructures.LAIR, nbt);
        this.center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        this.seed = nbt.getLong("Seed");
        this.kind = nbt.getString("Kind").equals("frost") ? Kind.FROST : Kind.STORM;
    }

    public BlockPos getCenter() {
        return this.center;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putLong("Seed", this.seed);
        if (this.kind == Kind.FROST) {
            nbt.putString("Kind", "frost");
        }
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random chunkRandom, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        Random random = Random.create(this.seed);
        boolean frost = this.kind == Kind.FROST;
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
                        foundation(world, chunkBox, pos.down(drop), random.nextFloat(), frost);
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
                foundation(world, chunkBox, pos.down(), random.nextFloat(), frost);
                if (frost) {
                    set(world, chunkBox, pos, frostFloor(d, r));
                    float drift = random.nextFloat();
                    if (d > RADIUS - 1.5D) {
                        // The rim: a low wall of rimestone and packed ice, drifted with snow.
                        set(world, chunkBox, pos.up(), (r < 0.3F ? ModBlockFamilies.RIMESTONE_BRICKS.base()
                                : r < 0.45F ? ModBlockFamilies.RIMESTONE.base() : r < 0.6F ? Blocks.PACKED_ICE
                                : r < 0.7F ? Blocks.BLUE_ICE : Blocks.SNOW_BLOCK).getDefaultState());
                    } else if (d > 3.6D && drift < 0.18F) {
                        // Snow drifted across the floor.
                        set(world, chunkBox, pos.up(), Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, 1 + (int) (drift * 11.0F)));
                    } else {
                        this.roundTheNest(world, chunkBox, pos, d, true);
                    }
                    continue;
                }
                set(world, chunkBox, pos, floor(d, r));
                if (d > RADIUS - 1.5D) {
                    // The rim: a low wall of weathered brick.
                    set(world, chunkBox, pos.up(), (r < 0.3F ? Blocks.CRACKED_STONE_BRICKS : r < 0.45F ? Blocks.MOSSY_STONE_BRICKS
                            : Blocks.STONE_BRICKS).getDefaultState());
                } else {
                    this.roundTheNest(world, chunkBox, pos, d, false);
                }
            }
        }

        // Standing stones on the rim, each crowned with a lightning rod unless it's fallen.
        double turn = random.nextDouble() * Math.PI * 2.0D;
        for (int i = 0; frost && i < STONES; i++) {
            // Or, in a frost lair, spires of ice.
            double angle = turn + Math.PI * 2.0D * i / STONES;
            int sx = cx + (int) Math.round(Math.cos(angle) * (RADIUS - 1));
            int sz = cz + (int) Math.round(Math.sin(angle) * (RADIUS - 1));
            this.iceSpire(world, chunkBox, new BlockPos(sx, y0, sz), random);
        }
        for (int i = 0; !frost && i < STONES; i++) {
            double angle = turn + Math.PI * 2.0D * i / STONES;
            int sx = cx + (int) Math.round(Math.cos(angle) * (RADIUS - 1));
            int sz = cz + (int) Math.round(Math.sin(angle) * (RADIUS - 1));
            boolean broken = random.nextFloat() < 0.25F;
            int height = broken ? 2 + random.nextInt(2) : 5 + random.nextInt(3);
            for (int y = 1; y <= height; y++) {
                float r = random.nextFloat();
                BlockState stone = y == 1 ? Blocks.CHISELED_STONE_BRICKS.getDefaultState()
                        // A band of storm glass under the copper cap, where the lightning comes down.
                        : y == height && !broken ? LairBlocks.STORM_GLASS.getDefaultState()
                        : r < 0.2F ? Blocks.CRACKED_STONE_BRICKS.getDefaultState()
                        : Blocks.POLISHED_ANDESITE.getDefaultState();
                set(world, chunkBox, new BlockPos(sx, y0 + y, sz), stone);
            }
            this.struckGround(world, chunkBox, new BlockPos(sx, y0, sz));
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
                float n = noise(pos, 1);
                if (d > 2.5D && d <= 3.6D) {
                    // Logs, and woven nest, round the ring.
                    set(world, chunkBox, pos.up(), n < 0.35F ? ModBlockFamilies.DRAGON_NEST.base().getDefaultState()
                            : nestLog(dx, dz, r));
                    if (h > 0) {
                        set(world, chunkBox, pos.up(2), r < 0.5F ? Blocks.BONE_BLOCK.getDefaultState()
                                : n > 0.6F ? ModBlockFamilies.DRAGON_NEST.slab().getDefaultState() : nestLog(dz, dx, r));
                    }
                } else if (d <= 2.5D && (dx != 0 || dz != 0) && r < 0.45F) {
                    // The hoard: gold and gems, heaps of coins and the dragon's shed scales.
                    if (frost ? r >= 0.26F : r >= 0.2F) {
                        set(world, chunkBox, pos.up(), n < 0.5F ? coins(3 + (int) (n * 10.0F)) : scales(frost, 1 + (int) ((n - 0.5F) * 8.0F)));
                    } else {
                        set(world, chunkBox, pos.up(), (frost
                                ? r < 0.07F ? Blocks.DIAMOND_BLOCK : r < 0.2F ? Blocks.GOLD_BLOCK : Blocks.RAW_GOLD_BLOCK
                                : r < 0.08F ? Blocks.RAW_GOLD_BLOCK : Blocks.GOLD_BLOCK).getDefaultState());
                    }
                } else if (d <= 2.5D && (dx != 0 || dz != 0) && n < 0.6F) {
                    // Loose coins round the heaps.
                    set(world, chunkBox, pos.up(), coins(1 + (int) (n * 5.0F)));
                }
            }
        }
        long lootSeed = random.nextLong();
        BlockPos chest = this.center.up();
        set(world, chunkBox, chest, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.Type.HORIZONTAL.random(random)));
        if (chunkBox.contains(chest) && world.getBlockEntity(chest) instanceof LootableContainerBlockEntity container) {
            container.setLootTable(this.kind.hoard, lootSeed);
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
                world.setBlockState(at, Blocks.AIR.getDefaultState(), PieceBlocks.UPDATE);
                world.setBlockState(at.up(), Blocks.AIR.getDefaultState(), PieceBlocks.UPDATE);
                MimicEntity mimic = MimicEntity.disguised(world.toServerWorld(), at, side, this.kind.hoard);
                if (mimic != null) {
                    world.spawnEntity(mimic);
                }
            }
        }

        this.sideCache(world, chunkBox, frost);
    }

    /**
     * The side cache: a cairn {@value #CAIRN_OUT} to {@value #CAIRN_BACK} blocks out from the nest, its 2x2x2
     * doorway plugged by a Heavy Boulder, a chest from the lair's hoard table inside. It draws from its own random
     * (seeded from the lair's), so lairs generated before it was added keep their layout.
     */
    private void sideCache(StructureWorldAccess world, BlockBox chunkBox, boolean frost) {
        Random random = Random.create(this.seed ^ 0x5ADECA11L);
        Direction side = random.nextBoolean() ? Direction.NORTH : Direction.SOUTH;
        long lootSeed = random.nextLong();
        Direction right = side.rotateYClockwise();
        // Dark deepslate (like the rune ring) or ice, so the grey boulder stands out against it
        BlockState wall = (frost ? Blocks.PACKED_ICE : Blocks.DEEPSLATE_BRICKS).getDefaultState();
        BlockState trim = (frost ? Blocks.BLUE_ICE : Blocks.CHISELED_DEEPSLATE).getDefaultState();
        BlockPos base = this.center.offset(side, CAIRN_OUT);
        // Two thick at the front (the boulder's depth), one thick elsewhere, roofed, across -2..1.
        for (int out = 0; out <= CAIRN_BACK - CAIRN_OUT; out++) {
            for (int across = -2; across <= 1; across++) {
                for (int up = 1; up <= 4; up++) {
                    BlockPos pos = base.offset(side, out).offset(right, across).up(up);
                    boolean inside = out >= 2 && out < CAIRN_BACK - CAIRN_OUT && across >= -1 && across <= 0 && up <= 3;
                    set(world, chunkBox, pos, inside ? Blocks.AIR.getDefaultState() : up == 4 ? trim : wall);
                }
                set(world, chunkBox, base.offset(side, out).offset(right, across), wall);
            }
        }
        BlockPos chest = base.offset(side, CAIRN_BACK - CAIRN_OUT - 1).up();
        set(world, chunkBox, chest, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, side.getOpposite()));
        if (chunkBox.contains(chest) && world.getBlockEntity(chest) instanceof LootableContainerBlockEntity container) {
            container.setLootTable(this.kind.hoard, lootSeed);
        }
        BlockPos from = base.offset(right, -1).up();
        ObstaclePlacer.box(world, chunkBox, from, base.offset(side, 1).up(2), ObstacleTypes.HEAVY_BOULDER, Tier.HARD,
                false, StructureObstacles.seedAt(from));
    }

    private void spawnDragon(StructureWorldAccess world, BlockPos at, float yaw) {
        LairDragonEntity dragon = this.kind.dragon().create(world.toServerWorld());
        if (dragon == null) {
            return;
        }
        dragon.refreshPositionAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, yaw, 0.0F);
        dragon.initialize(world, world.getLocalDifficulty(at), SpawnReason.STRUCTURE, null, null);
        dragon.setLair(this.center);
        dragon.setPersistent();
        world.spawnEntityAndPassengers(dragon);
    }

    /** The floor at distance d from the middle: the woven nest, scorched stone and fulgurite, a ring of runes. */
    private static BlockState floor(double d, float r) {
        if (d <= 2.5D) {
            return ModBlockFamilies.DRAGON_NEST.base().getDefaultState();
        }
        if (d > 6.5D && d <= 7.5D) {
            return (r < 0.25F ? Blocks.CHISELED_DEEPSLATE : Blocks.DEEPSLATE_TILES).getDefaultState();
        }
        // Scorch marks and fulgurite where the lightning has come down.
        if (r < 0.06F) {
            return Blocks.BLACKSTONE.getDefaultState();
        }
        if (r < 0.16F) {
            return ModBlockFamilies.FULGURITE.base().getDefaultState();
        }
        return (r < 0.45F ? Blocks.ANDESITE : r < 0.7F ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS).getDefaultState();
    }

    /** A frost lair's floor: the woven nest, snow, ice and rimestone, and a ring of blue ice where the storm lair has its runes. */
    private static BlockState frostFloor(double d, float r) {
        if (d <= 2.5D) {
            return ModBlockFamilies.DRAGON_NEST.base().getDefaultState();
        }
        if (d > 6.5D && d <= 7.5D) {
            return (r < 0.6F ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).getDefaultState();
        }
        return (r < 0.2F ? Blocks.PACKED_ICE : r < 0.3F ? ModBlockFamilies.RIMESTONE.base() : Blocks.SNOW_BLOCK).getDefaultState();
    }

    /**
     * A spire of ice standing on the rim: a packed ice column veined with blue ice, thick at the foot
     * and tipped with blue ice, on a rimestone footing. A quarter of them have snapped off short. Hoarfrost
     * crystals grow on its buttresses and sides.
     */
    private void iceSpire(StructureWorldAccess world, BlockBox chunkBox, BlockPos foot, Random random) {
        boolean broken = random.nextFloat() < 0.25F;
        int height = broken ? 2 + random.nextInt(2) : 6 + random.nextInt(4);
        int buttress = broken ? 1 : 1 + height / 3;
        for (int y = 1; y <= height; y++) {
            float r = random.nextFloat();
            boolean tip = !broken && y > height - 2;
            BlockState ice = (y == 1 ? ModBlockFamilies.RIMESTONE_BRICKS.base()
                    : tip || r < 0.25F ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).getDefaultState();
            BlockPos at = foot.up(y);
            set(world, chunkBox, at, ice);
            for (Direction side : Direction.Type.HORIZONTAL) {
                BlockPos beside = at.offset(side);
                if (y <= buttress) {
                    // Every side draws the same randoms in every chunk
                    boolean place = random.nextFloat() < 0.8F;
                    if (place) {
                        set(world, chunkBox, beside, (y == 1 && noise(beside, 2) < 0.5F
                                ? ModBlockFamilies.RIMESTONE.base() : Blocks.PACKED_ICE).getDefaultState());
                        if (y == buttress && noise(beside, 3) < 0.45F) {
                            set(world, chunkBox, beside.up(), crystal(Direction.UP));
                        }
                    }
                } else if (y < height && noise(beside, 4) < 0.12F) {
                    set(world, chunkBox, beside, crystal(side));
                }
            }
        }
    }

    /**
     * Dressing on the floor round the nest at distance d: shed scales in both lairs, then fulgurite spikes (storm)
     * or hoarfrost crystals (frost) further out. {@code floor} is the floor block; this sets the one above it.
     */
    private void roundTheNest(StructureWorldAccess world, BlockBox chunkBox, BlockPos floor, double d, boolean frost) {
        if (d <= 3.6D) {
            return;
        }
        float n = noise(floor, 5);
        if (d <= 5.5D && n < 0.22F) {
            set(world, chunkBox, floor.up(), scales(frost, 1 + (int) (n * 9.0F)));
        } else if (frost && d <= 6.5D && n > 0.9F) {
            set(world, chunkBox, floor.up(), crystal(Direction.UP));
        } else if (!frost && d > 4.5D && (d <= 6.5D || d > 7.5D) && n > 0.95F) {
            set(world, chunkBox, floor.up(), LairBlocks.FULGURITE_SPIKE.getDefaultState());
        }
    }

    /** Where lightning struck a standing stone: fulgurite in the floor round its foot, and a spike or two. */
    private void struckGround(StructureWorldAccess world, BlockBox chunkBox, BlockPos stone) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos at = stone.add(dx, 0, dz);
                double d = Math.sqrt(Math.pow(at.getX() - this.center.getX(), 2) + Math.pow(at.getZ() - this.center.getZ(), 2));
                if ((dx == 0 && dz == 0) || d > RADIUS - 1.5D) {
                    continue;
                }
                float n = noise(at, 6);
                if (n < 0.75F) {
                    set(world, chunkBox, at, ModBlockFamilies.FULGURITE.base().getDefaultState());
                }
                if (n < 0.2F) {
                    set(world, chunkBox, at.up(), LairBlocks.FULGURITE_SPIKE.getDefaultState());
                }
            }
        }
    }

    /** A Treasury Coin Pile (the dwarves' block, reused for hoards) of 1-8 layers. */
    private static BlockState coins(int layers) {
        return DwarvenBlocks.TREASURY_COIN_PILE.getDefaultState().with(SnowBlock.LAYERS, Math.max(1, Math.min(8, layers)));
    }

    /** The lair's dragon's shed scales, 1-8 layers. */
    private static BlockState scales(boolean frost, int layers) {
        return (frost ? LairBlocks.FROST_SCALE_PILE : LairBlocks.STORM_SCALE_PILE).getDefaultState()
                .with(SnowBlock.LAYERS, Math.max(1, Math.min(8, layers)));
    }

    private static BlockState crystal(Direction facing) {
        return LairBlocks.HOARFROST_CRYSTAL.getDefaultState().with(AmethystClusterBlock.FACING, facing);
    }

    /**
     * A number in [0, 1) from this lair's seed, the position and {@code salt}: the same in every chunk, and it
     * leaves the piece's random sequence alone.
     */
    private float noise(BlockPos pos, int salt) {
        long h = this.seed + salt * 0x9E3779B97F4A7C15L + pos.asLong() * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 40) / (float) (1L << 24);
    }

    private static BlockState nestLog(int dx, int dz, float r) {
        Block log = r < 0.6F ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_LOG;
        // Lying round the ring, more or less.
        Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.Z : Direction.Axis.X;
        return log.getDefaultState().with(PillarBlock.AXIS, axis);
    }

    private static void set(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, state, PieceBlocks.UPDATE);
        }
    }

    /**
     * Fills rock down from {@code top} wherever there's air, snow or liquid, until it meets the
     * mountain. {@code r} picks the odd patch of andesite or gravel in the surface. A frost lair's
     * rock is capped with three blocks of snow (or the odd patch of packed ice).
     */
    private static void foundation(StructureWorldAccess world, BlockBox chunkBox, BlockPos top, float r, boolean frost) {
        if (!chunkBox.contains(top)) {
            return;
        }
        BlockPos.Mutable pos = top.mutableCopy();
        for (int i = 0; i < MAX_FOUNDATION && pos.getY() > world.getBottomY(); i++) {
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()) {
                return;
            }
            Block rock = frost ? (i > 2 ? Blocks.STONE : r < 0.15F ? Blocks.PACKED_ICE : Blocks.SNOW_BLOCK)
                    : i > 0 ? Blocks.STONE : r < 0.15F ? Blocks.ANDESITE : r < 0.22F ? Blocks.GRAVEL : Blocks.STONE;
            world.setBlockState(pos, rock.getDefaultState(), PieceBlocks.UPDATE);
            pos.move(Direction.DOWN);
        }
    }
}
