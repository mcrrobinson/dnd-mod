package mattonfire.dnd.world.gen;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mattonfire.dnd.classes.Blocks.RubbleBlock;
import mattonfire.dnd.classes.Blocks.dwarven.CoinPileBlock;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.BlockFamily;
import mattonfire.dnd.classes.Registry.DesolateBlocks;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.RuinBlocks;
import mattonfire.dnd.world.gen.burrow.ObservatoryPiece;
import net.minecraft.block.AttachedStemBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CakeBlock;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.DeadBushBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.DyedCarpetBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.Material;
import net.minecraft.block.MushroomPlantBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.data.family.BlockFamilies;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.TreeConfiguredFeatures;

/**
 * Wears one chunk's slice of a desolate settlement down to its {@link DesolationLevel}, after its pieces are
 * placed ({@link Desolation#end}). Every decision comes from {@link Desolation#roll} on the block position and the
 * start, so chunks agree where they meet. In order:
 * <ol>
 *   <li>Every block, top down: containers and beehives emptied, gold looted, then the level's changes (ground,
 *   plants, leaves, lights, glass, carpets, doors, cracked and mossy bricks, cobwebs). Snow on a block that's
 *   gone goes with it.</li>
 *   <li>Fallen doorways (before the rest, so the doors are still there to find): the door and the lintel over it
 *   come down in a heap on the threshold.</li>
 *   <li>Collapses, planned per piece from the start alone so they carry across chunk borders: breaches knocked
 *   through its walls and roof, and holes down through its roof and floors. Some of what they knock out lies on
 *   the ground below as slabs, blocks or rubble.</li>
 *   <li>Support ({@link #supports}): what the breaches leave without support comes down, planned per piece from
 *   its breaches, and fragments left floating in this chunk fall ({@link #fragments}).</li>
 *   <li>Overgrowth: flowers and tall grass, vines down the walls, moss on floors, moss and soil on the roofs.</li>
 *   <li>Trees grown up inside.</li>
 * </ol>
 * It covers the structure's box and {@link #ABOVE} blocks above it, fading out over the outer {@link #FADE}.
 */
final class Withering {
    /** Blocks over which the withering fades in from the edge of the structure's box. */
    static final int FADE = 6;
    /**
     * Blocks above the structure's box that are withered too: pieces build above their own boxes (the hobbit
     * party tree, the grass on top of the grounds).
     */
    static final int ABOVE = 24;
    static final float DEAD_BUSH = 0.3F;
    /** How far a breach looks down for the ground its rubble falls on. */
    static final int RUBBLE_FALL = 24;
    /** A piece this many blocks square gets the level's number of breaches and holes; others by their area. */
    static final int PIECE_SIZE = 12;
    /** The most a big piece multiplies them by, and the least a small one does. */
    static final float MAX_PIECE_FACTOR = 12.0F;
    static final float MIN_PIECE_FACTOR = 0.4F;
    /**
     * A piece this many times {@link #PIECE_SIZE}'s area or more (a goblin camp is one piece) has half its breaches
     * anywhere inside it rather than on its sides, where its buildings are.
     */
    static final float BIG_PIECE = 2.0F;
    /** A fallen doorway's lintel blocks that come down, in the first and second rows over the door. */
    static final float LINTEL_FALLS = 0.8F;
    static final float LINTEL_TOP_FALLS = 0.5F;
    /** How many floors (and roofs) a hole goes down through. */
    static final int HOLE_FLOORS = 3;
    /** The least share of the blocks a {@link Ruinable} piece knocks out that are thrown onto the ground. */
    static final float THROWN_RUBBLE = 0.5F;
    /** How far a thrown block can fall to the ground (down a slope from the top of a wall). */
    static final int THROWN_FALL = 40;
    /** How far round a {@link Ruinable} piece's breach its bricks are cracked, and how many of them. */
    static final float RIM = 1.5F;
    static final float RIM_CRACKED = 0.6F;

    private static final Block[] FLOWERS = {Blocks.GRASS, Blocks.GRASS, Blocks.GRASS, Blocks.FERN, Blocks.DANDELION,
            Blocks.POPPY, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.ALLIUM};
    private static final List<RegistryKey<ConfiguredFeature<?, ?>>> TREES = List.of(TreeConfiguredFeatures.OAK,
            TreeConfiguredFeatures.BIRCH, TreeConfiguredFeatures.OAK);
    private static Map<Block, Block> slabs;

    private final StructureWorldAccess world;
    private final StructureStart start;
    private final BlockBox box;
    private final BlockBox chunkBox;
    private final ChunkGenerator generator;
    private final DesolationLevel level;
    private final long salt;
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;
    /** Containers emptied, blocks changed, collapses (breaches, holes and doorways), blocks fallen for want of support. */
    private final int[] counts = new int[4];

    Withering(StructureWorldAccess world, StructureStart start, BlockBox chunkBox, ChunkGenerator generator, DesolationLevel level) {
        this.world = world;
        this.start = start;
        this.box = start.getBoundingBox();
        this.chunkBox = chunkBox;
        this.generator = generator;
        this.level = level;
        this.salt = start.getPos().toLong();
        this.minX = Math.max(this.box.getMinX(), chunkBox.getMinX());
        this.maxX = Math.min(this.box.getMaxX(), chunkBox.getMaxX());
        this.minZ = Math.max(this.box.getMinZ(), chunkBox.getMinZ());
        this.maxZ = Math.min(this.box.getMaxZ(), chunkBox.getMaxZ());
        this.minY = Math.max(this.box.getMinY(), chunkBox.getMinY());
        this.maxY = Math.min(this.box.getMaxY() + ABOVE, chunkBox.getMaxY());
    }

    /** Returns containers emptied, blocks changed and breaches made. */
    int[] run() {
        this.doorways();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = this.minX; x <= this.maxX; x++) {
            for (int z = this.minZ; z <= this.maxZ; z++) {
                if (this.faded(x, z)) {
                    continue;
                }
                // Top down, so a cobweb sees the ceiling above it as it ends up.
                for (int y = this.maxY; y >= this.minY; y--) {
                    pos.set(x, y, z);
                    this.witherBlock(pos);
                }
            }
        }
        this.collapses();
        this.supports();
        this.fragments();
        if (this.level.flowers > 0.0F || this.level.vines > 0.0F || this.level.roofs > 0.0F || this.level.floorMoss > 0.0F) {
            this.overgrow();
        }
        this.trees();
        return this.counts;
    }

    private float roll(int x, int y, int z, int k) {
        return Desolation.roll(this.salt, x, y, z, k);
    }

    /** Whether column x/z is left alone, near the edge of the structure's box. */
    private boolean faded(int x, int z) {
        int edge = Math.min(Math.min(x - this.box.getMinX(), this.box.getMaxX() - x), Math.min(z - this.box.getMinZ(), this.box.getMaxZ() - z));
        return this.roll(x, 0, z, 0) >= (edge + 1) / (float) (FADE + 1);
    }

    private void set(BlockPos pos, BlockState state) {
        this.world.setBlockState(pos, state, PieceBlocks.UPDATE);
        this.counts[1]++;
        BlockPos up = pos.up();
        if (!state.isSideSolidFullSquare(this.world, pos, Direction.UP) && this.chunkBox.contains(up)
                && this.world.getBlockState(up).isOf(Blocks.SNOW)) {
            // Snow that lay on leaves or a roof that's gone.
            this.world.setBlockState(up, Blocks.AIR.getDefaultState(), PieceBlocks.UPDATE);
        }
    }

    private void witherBlock(BlockPos pos) {
        BlockState state = this.world.getBlockState(pos);
        BlockEntity blockEntity = this.world.getBlockEntity(pos);
        if (blockEntity instanceof BeehiveBlockEntity) {
            // A fresh hive: no bees, no honey.
            this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), PieceBlocks.UPDATE);
            this.world.setBlockState(pos, state.with(Properties.HONEY_LEVEL, 0), PieceBlocks.UPDATE);
            this.counts[0]++;
            return;
        }
        if (blockEntity instanceof Clearable) {
            if (blockEntity instanceof LootableContainerBlockEntity container) {
                container.setLootTable(null, 0L);
            }
            Clearable.clear(blockEntity);
            this.counts[0]++;
        }
        BlockState withered = this.withered(pos, state);
        if (withered != state) {
            this.set(pos, withered);
            if (state.getBlock() instanceof PaneBlock) {
                // Panes beside a broken one lose their arm towards it.
                for (Direction side : Direction.Type.HORIZONTAL) {
                    BlockPos next = pos.offset(side);
                    if (this.chunkBox.contains(next)) {
                        this.world.getChunk(next).markBlockForPostProcessing(next);
                    }
                }
            }
        }
    }

    /** Whether {@code state} is part of a building rather than the land: what a breach knocks out. */
    static boolean built(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.isReplaceable()) {
            return false;
        }
        return !(state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.SAND)
                || state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.TERRACOTTA)
                || state.isIn(BlockTags.SNOW) || state.isIn(BlockTags.ICE) || state.isIn(BlockTags.COAL_ORES)
                || state.isIn(BlockTags.IRON_ORES) || state.isIn(BlockTags.COPPER_ORES) || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.LAPIS_ORES) || state.isIn(BlockTags.DIAMOND_ORES)
                || state.isIn(BlockTags.EMERALD_ORES) || state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.BEDROCK)
                || state.isOf(Blocks.SANDSTONE) || state.isOf(Blocks.RED_SANDSTONE) || state.isOf(Blocks.CLAY)
                || state.isOf(Blocks.CALCITE) || state.isOf(Blocks.DRIPSTONE_BLOCK) || state.isOf(Blocks.POINTED_DRIPSTONE)
                || state.isOf(Blocks.AMETHYST_BLOCK) || state.isOf(Blocks.BUDDING_AMETHYST) || state.isOf(Blocks.OBSIDIAN));
    }

    /** What {@code state} at {@code pos} becomes ({@code state} itself to leave it). */
    private BlockState withered(BlockPos pos, BlockState state) {
        DesolationLevel l = this.level;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        float roll = this.roll(x, y, z, 1);
        Block block = state.getBlock();
        BlockState empty = state.getFluidState().isStill() ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
        if (state.isAir()) {
            return roll < l.cobwebs && this.world.getBlockState(pos.up()).isOpaqueFullCube(this.world, pos.up())
                    ? Blocks.COBWEB.getDefaultState() : state;
        }
        // Looted.
        if (block == Blocks.GOLD_BLOCK || block instanceof CoinPileBlock || block instanceof CakeBlock || block instanceof CandleCakeBlock) {
            return Blocks.AIR.getDefaultState();
        }
        // The ground.
        if (block == Blocks.GRASS_BLOCK) {
            if (Desolation.patches(this.salt, x, z, 6, 2) < l.bareGround) {
                return l == DesolationLevel.RAIDED && roll < 0.2F ? Blocks.DIRT_PATH.getDefaultState()
                        : roll < 0.55F ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.DIRT.getDefaultState();
            }
            return state;
        }
        if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.DIRT_PATH || block == Blocks.FARMLAND) {
            BlockState above = this.world.getBlockState(pos.up());
            boolean open = above.isAir() || above.isReplaceable() && above.getFluidState().isEmpty();
            if (open && l.regrowth > 0.0F && Desolation.patches(this.salt, x, z, 5, 3) < l.regrowth) {
                return Blocks.GRASS_BLOCK.getDefaultState();
            }
            if (block == Blocks.FARMLAND) {
                return (l == DesolationLevel.ABANDONED ? Blocks.DIRT : Blocks.COARSE_DIRT).getDefaultState();
            }
            return state;
        }
        if (block == Blocks.MOSS_BLOCK && l == DesolationLevel.FORSAKEN) {
            return Blocks.COARSE_DIRT.getDefaultState();
        }
        // Plants.
        if (block instanceof CropBlock || block instanceof StemBlock || block instanceof AttachedStemBlock || block instanceof SweetBerryBushBlock) {
            if (l == DesolationLevel.OVERGROWN) {
                return roll < 0.7F ? Blocks.GRASS.getDefaultState() : this.flower(x, y, z);
            }
            return roll < DEAD_BUSH ? Blocks.DEAD_BUSH.getDefaultState() : Blocks.AIR.getDefaultState();
        }
        if (block instanceof PlantBlock && !(block instanceof DeadBushBlock) && !(block instanceof MushroomPlantBlock)) {
            // A tall plant decides by its lower half, so both halves agree.
            boolean lower = !(block instanceof TallPlantBlock) || state.get(TallPlantBlock.HALF) == DoubleBlockHalf.LOWER;
            int base = lower ? y : y - 1;
            if (this.roll(x, base, z, 4) >= l.wildDie) {
                return state;
            }
            return lower && roll < DEAD_BUSH && state.getFluidState().isEmpty() ? Blocks.DEAD_BUSH.getDefaultState() : empty;
        }
        if (block instanceof LeavesBlock) {
            return roll < l.leavesKept ? state : empty;
        }
        if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) {
            return Blocks.AIR.getDefaultState();
        }
        if (l == DesolationLevel.FORSAKEN && (block == Blocks.VINE || block == Blocks.GLOW_LICHEN || block == Blocks.MOSS_CARPET
                || block == Blocks.SUGAR_CANE || block == Blocks.HANGING_ROOTS || block == Blocks.SPORE_BLOSSOM)) {
            return empty;
        }
        if (block instanceof FlowerPotBlock pot && pot.getContent() != Blocks.AIR && l != DesolationLevel.OVERGROWN) {
            return roll < 0.5F ? Blocks.POTTED_DEAD_BUSH.getDefaultState() : Blocks.FLOWER_POT.getDefaultState();
        }
        // Lights.
        if (block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH) {
            return roll < l.torchesKept ? state : Blocks.AIR.getDefaultState();
        }
        if (block instanceof LanternBlock) {
            return roll < l.lanternsKept ? state : empty;
        }
        if (block == Blocks.JACK_O_LANTERN) {
            return Blocks.CARVED_PUMPKIN.getDefaultState().with(CarvedPumpkinBlock.FACING, state.get(CarvedPumpkinBlock.FACING));
        }
        if (state.contains(Properties.LIT) && state.get(Properties.LIT)) {
            // Campfires may still smoulder after a raid; candles, furnaces and lamps are out.
            return block instanceof CampfireBlock && roll < l.firesLit ? state : state.with(Properties.LIT, false);
        }
        if (block == Blocks.GLASS || block instanceof StainedGlassBlock || block instanceof PaneBlock && block != Blocks.IRON_BARS) {
            return roll < l.glassBroken ? empty : state;
        }
        if (block instanceof CarpetBlock && block != Blocks.MOSS_CARPET) {
            if (roll < l.carpetsGone) {
                return Blocks.AIR.getDefaultState();
            }
            if (block instanceof DyedCarpetBlock dyed && roll < l.carpetsGone + l.carpetsTattered) {
                return DesolateBlocks.tatteredCarpet(dyed.getDyeColor()).getDefaultState();
            }
            return state;
        }
        if (block instanceof DoorBlock) {
            int base = state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? y : y - 1;
            return this.roll(x, base, z, 9) < l.doorsBroken ? empty : state;
        }
        float wear = this.roll(x, y, z, 10);
        if (wear < l.cracked + l.mossy) {
            Block worn = wear < l.cracked ? cracked(block) : mossy(block);
            if (worn != null) {
                return worn.getDefaultState();
            }
        }
        return state;
    }

    private BlockState flower(int x, int y, int z) {
        return FLOWERS[(int) (this.roll(x, y, z, 5) * FLOWERS.length)].getDefaultState();
    }

    private static Block cracked(Block block) {
        if (block == Blocks.STONE_BRICKS) {
            return Blocks.CRACKED_STONE_BRICKS;
        }
        if (block == Blocks.DEEPSLATE_BRICKS) {
            return Blocks.CRACKED_DEEPSLATE_BRICKS;
        }
        if (block == Blocks.DEEPSLATE_TILES) {
            return Blocks.CRACKED_DEEPSLATE_TILES;
        }
        if (block == Blocks.POLISHED_BLACKSTONE_BRICKS) {
            return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS;
        }
        if (block == Blocks.NETHER_BRICKS) {
            return Blocks.CRACKED_NETHER_BRICKS;
        }
        if (block == ModBlockFamilies.RUNIC_DEEPSLATE_BRICKS.base()) {
            return ModBlockFamilies.CRACKED_RUNIC_DEEPSLATE_BRICKS.base();
        }
        return null;
    }

    private static Block mossy(Block block) {
        if (block == Blocks.STONE_BRICKS) {
            return Blocks.MOSSY_STONE_BRICKS;
        }
        if (block == Blocks.COBBLESTONE) {
            return Blocks.MOSSY_COBBLESTONE;
        }
        if (block == ModBlockFamilies.RUNIC_DEEPSLATE_BRICKS.base()) {
            return ModBlockFamilies.MOSSY_RUNIC_DEEPSLATE_BRICKS.base();
        }
        if (block == ModBlockFamilies.THATCH.base()) {
            return ModBlockFamilies.MOSSY_THATCH.base();
        }
        return null;
    }

    // --- Collapses ---------------------------------------------------------------------------------------------

    /** Whether {@code pos} is a built block a collapse can knock out: no block entity, not unbreakable. */
    private boolean knockable(BlockPos pos, BlockState state) {
        return built(state) && this.world.getBlockEntity(pos) == null && state.getHardness(this.world, pos) >= 0.0F;
    }

    /** {@code mean} as a whole number: its whole part, plus one with the chance of its fraction. */
    private static int count(float mean, float roll) {
        return (int) mean + (roll < mean - (int) mean ? 1 : 0);
    }

    /**
     * Fallen doorways: each door in this chunk falls in with the level's chance, taking the lintel over it (the
     * two rows above, along the wall) and leaving a heap on the threshold.
     */
    private void doorways() {
        if (this.level.doorways <= 0.0F) {
            return;
        }
        BlockPos.Mutable pos = new BlockPos.Mutable();
        List<BlockPos> doors = new ArrayList<>();
        for (int x = this.minX; x <= this.maxX; x++) {
            for (int z = this.minZ; z <= this.maxZ; z++) {
                if (this.faded(x, z)) {
                    continue;
                }
                for (int y = this.minY; y <= this.maxY; y++) {
                    BlockState state = this.world.getBlockState(pos.set(x, y, z));
                    if (state.getBlock() instanceof DoorBlock && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                            && this.roll(x, y, z, 70) < this.level.doorways) {
                        doors.add(pos.toImmutable());
                    }
                }
            }
        }
        for (BlockPos door : doors) {
            this.counts[2]++;
            Direction along = this.world.getBlockState(door).get(DoorBlock.FACING).rotateYClockwise();
            BlockState lintel = null;
            this.set(door, Blocks.AIR.getDefaultState());
            if (this.world.getBlockState(door.up()).getBlock() instanceof DoorBlock) {
                this.set(door.up(), Blocks.AIR.getDefaultState());
            }
            for (int dy = 2; dy <= 3; dy++) {
                for (int side = -1; side <= 1; side++) {
                    BlockPos at = door.up(dy).offset(along, side);
                    if (!this.chunkBox.contains(at)) {
                        continue;
                    }
                    BlockState state = this.world.getBlockState(at);
                    float chance = dy == 2 ? LINTEL_FALLS : LINTEL_TOP_FALLS;
                    if (this.knockable(at, state) && this.roll(at.getX(), at.getY(), at.getZ(), 71) < chance) {
                        this.set(at, Blocks.AIR.getDefaultState());
                        lintel = lintel == null ? state : lintel;
                        if (side != 0) {
                            this.fall(at, state);
                        }
                    }
                }
            }
            // The heap where the door was.
            BlockState heap = lintel == null ? null : this.debris(lintel, this.roll(door.getX(), door.getY(), door.getZ(), 72), door, true);
            if (heap != null && this.world.getBlockState(door.down()).isSideSolidFullSquare(this.world, door.down(), Direction.UP)) {
                this.set(door, heap);
            }
        }
    }

    /**
     * Breaches and holes, planned per piece from the start's pieces and the start's salt, so every chunk plans the
     * same ones and carves its own part of each.
     */
    private void collapses() {
        int index = 0;
        for (StructurePiece piece : this.start.getChildren()) {
            BlockBox b = piece.getBoundingBox();
            index++;
            if (piece instanceof ObservatoryPiece) {
                // Its dome caves in on its own plan, and a hole or breach would leave bits of the rim floating.
                continue;
            }
            if (piece instanceof Ruinable ruinable && ruinable.ruin(new PieceRuin(index))) {
                continue;
            }
            float factor = MathHelper.clamp(b.getBlockCountX() * b.getBlockCountZ() / (float) (PIECE_SIZE * PIECE_SIZE),
                    MIN_PIECE_FACTOR, MAX_PIECE_FACTOR);
            int breaches = count(this.level.breaches * factor, this.roll(index, 0, 0, 60));
            for (int j = 0; j < breaches; j++) {
                this.plannedBreach(b, index, j, factor >= BIG_PIECE && this.roll(index, j, 0, 69) < 0.5F);
            }
            int holes = count(this.level.floorHoles * factor, this.roll(index, 0, 0, 65));
            for (int j = 0; j < holes; j++) {
                this.hole(b, index, j);
            }
        }
    }

    /**
     * Breach {@code j} of piece {@code index}: on one of its four walls (its box's sides) or its roof, or anywhere
     * inside it if {@code inside}.
     */
    private void plannedBreach(BlockBox b, int index, int j, boolean inside) {
        int face = inside ? 5 : (int) (this.roll(index, j, 1, 61) * 5);
        float t = this.roll(index, j, 2, 62);
        float h = this.roll(index, j, 3, 63);
        int x = b.getMinX() + (int) (t * b.getBlockCountX());
        int z = b.getMinZ() + (int) (t * b.getBlockCountZ());
        int y = b.getMinY() + 1 + (int) (h * Math.max(1, b.getBlockCountY() - 2));
        switch (face) {
            case 0 -> z = b.getMinZ() + 1;
            case 1 -> z = b.getMaxZ() - 1;
            case 2 -> x = b.getMinX() + 1;
            case 3 -> x = b.getMaxX() - 1;
            case 5 -> z = b.getMinZ() + (int) (this.roll(index, j, 4, 62) * b.getBlockCountZ());
            default -> {
                x = b.getMinX() + (int) (t * b.getBlockCountX());
                z = b.getMinZ() + (int) (this.roll(index, j, 4, 62) * b.getBlockCountZ());
                y = b.getMaxY() - (int) (h * 3);
            }
        }
        float radius = this.level.breachMin + (this.level.breachMax - this.level.breachMin) * this.roll(index, j, 5, 64);
        this.planned.add(new Breach(index, x, y, z, radius, face));
        int r = (int) Math.ceil(radius) + 1;
        if (x + r < this.chunkBox.getMinX() || x - r > this.chunkBox.getMaxX() || z + r < this.chunkBox.getMinZ()
                || z - r > this.chunkBox.getMaxZ()) {
            return;
        }
        this.breach(new BlockPos(x, y, z), radius);
    }

    /**
     * Knocks a ragged hole of about {@code radius} through the built blocks round {@code centre} (the part in this
     * chunk), and drops rubble.
     */
    private void breach(BlockPos centre, float radius) {
        this.counts[2]++;
        List<BlockPos> knocked = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        int r = (int) Math.ceil(radius * 1.25F);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dy = r; dy >= -r; dy--) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (!this.chunkBox.contains(pos)) {
                        continue;
                    }
                    // Ragged: each block's own reach, between 0.75 and 1.25 of the radius.
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d > radius * (0.75F + 0.5F * this.roll(pos.getX(), pos.getY(), pos.getZ(), 13))) {
                        continue;
                    }
                    BlockState state = this.world.getBlockState(pos);
                    if (!this.knockable(pos, state)) {
                        continue;
                    }
                    this.set(pos, Blocks.AIR.getDefaultState());
                    knocked.add(pos.toImmutable());
                    states.add(state);
                }
            }
        }
        this.rubble(knocked, states);
    }

    /**
     * Hole {@code j} of piece {@code index}: a ragged shaft about 1 to 2.5 blocks across down through the piece's
     * roof and floors. In each column it takes the top block of each of the first {@link #HOLE_FLOORS} floors
     * (built blocks with open space over them), and half the time the one under it too.
     */
    private void hole(BlockBox b, int index, int j) {
        int cx = b.getMinX() + 1 + (int) (this.roll(index, j, 6, 66) * Math.max(1, b.getBlockCountX() - 2));
        int cz = b.getMinZ() + 1 + (int) (this.roll(index, j, 7, 66) * Math.max(1, b.getBlockCountZ() - 2));
        float radius = 1.0F + 1.5F * this.roll(index, j, 8, 66);
        int r = (int) Math.ceil(radius * 1.3F);
        if (cx + r < this.chunkBox.getMinX() || cx - r > this.chunkBox.getMaxX() || cz + r < this.chunkBox.getMinZ()
                || cz - r > this.chunkBox.getMaxZ()) {
            return;
        }
        this.counts[2]++;
        List<BlockPos> knocked = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int top = Math.min(b.getMaxY() + 2, this.chunkBox.getMaxY() - 1);
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                if (!this.chunkBox.contains(x, top, z) || d > radius * (0.7F + 0.6F * this.roll(x, 0, z, 67))) {
                    continue;
                }
                int floors = 0;
                for (int y = top; y >= b.getMinY() && floors < HOLE_FLOORS; y--) {
                    BlockState state = this.world.getBlockState(pos.set(x, y, z));
                    BlockState above = this.world.getBlockState(pos.up());
                    if (!above.isAir() || !this.knockable(pos, state)) {
                        continue;
                    }
                    floors++;
                    this.set(pos, Blocks.AIR.getDefaultState());
                    knocked.add(pos.toImmutable());
                    states.add(state);
                    pos.move(Direction.DOWN);
                    BlockState under = this.world.getBlockState(pos);
                    if (this.roll(x, y, z, 68) < 0.5F && this.knockable(pos, under)) {
                        this.set(pos, Blocks.AIR.getDefaultState());
                        knocked.add(pos.toImmutable());
                        states.add(under);
                    }
                    y = pos.getY();
                }
            }
        }
        this.rubble(knocked, states);
    }

    /** Some of the {@code knocked} blocks (the level's rubble chance) lie on the ground below where they were. */
    private void rubble(List<BlockPos> knocked, List<BlockState> states) {
        for (int i = 0; i < knocked.size(); i++) {
            BlockPos from = knocked.get(i);
            if (this.roll(from.getX(), from.getY(), from.getZ(), 15) < this.level.rubble) {
                this.fall(from, states.get(i));
            }
        }
    }

    /** Drops a piece of {@code state} from {@code from} onto the ground below it, scattered up to a block sideways. */
    private void fall(BlockPos from, BlockState state) {
        int dx = (int) (this.roll(from.getX(), from.getY(), from.getZ(), 16) * 3) - 1;
        int dz = (int) (this.roll(from.getX(), from.getY(), from.getZ(), 17) * 3) - 1;
        BlockPos pos = this.ground(from.getX() + dx, from.getY(), from.getZ() + dz, RUBBLE_FALL);
        if (pos != null) {
            this.land(pos, this.debris(state, this.roll(from.getX(), from.getY(), from.getZ(), 18), from, false), 1);
        }
    }

    /**
     * Throws a block knocked out at {@code from} (the level's rubble chance, at least {@link #THROWN_RUBBLE})
     * {@code out}, 1 to {@code reach} blocks and up to a block sideways, onto the ground there.
     */
    private void thrown(BlockPos from, BlockState state, Direction out, int reach) {
        int x = from.getX();
        int y = from.getY();
        int z = from.getZ();
        if (this.roll(x, y, z, 15) >= Math.max(this.level.rubble, THROWN_RUBBLE)) {
            return;
        }
        int dist = 1 + (int) (this.roll(x, y, z, 16) * reach);
        int side = (int) (this.roll(x, y, z, 17) * 3) - 1;
        Direction across = out.rotateYClockwise();
        BlockPos pos = this.ground(x + out.getOffsetX() * dist + across.getOffsetX() * side, y,
                z + out.getOffsetZ() * dist + across.getOffsetZ() * side, THROWN_FALL);
        if (pos != null) {
            this.land(pos, this.debris(state, this.roll(x, y, z, 18), from, false), 1);
        }
    }

    /**
     * Where something dropped in column x/z at height y comes to rest: the open block above the first solid one
     * below it, at most {@code fall} down. Null if that's outside this chunk or y itself isn't open.
     */
    private BlockPos ground(int x, int y, int z, int fall) {
        BlockPos.Mutable pos = new BlockPos.Mutable(x, y, z);
        if (!this.chunkBox.contains(pos) || !open(this.world.getBlockState(pos))) {
            return null;
        }
        for (int i = 0; i < fall && pos.getY() > this.chunkBox.getMinY() + 1; i++) {
            BlockState below = this.world.getBlockState(pos.down());
            if (!below.isAir() && !below.isReplaceable()) {
                break;
            }
            pos.move(Direction.DOWN);
        }
        return pos.toImmutable();
    }

    private static boolean open(BlockState state) {
        return state.isAir() || state.isReplaceable() && state.getFluidState().isEmpty();
    }

    /**
     * Lays {@code piece} at {@code pos} if it has firm ground under it (not leaves). On a heap of Collapsed Rubble
     * it adds {@code layers} to the heap instead. Returns whether anything was laid.
     */
    private boolean land(BlockPos pos, BlockState piece, int layers) {
        if (piece == null) {
            return false;
        }
        BlockState here = this.world.getBlockState(pos);
        if (here.isOf(RuinBlocks.COLLAPSED_RUBBLE)) {
            if (here.get(RubbleBlock.LAYERS) >= RubbleBlock.MAX_LAYERS) {
                return false;
            }
            this.set(pos, here.with(RubbleBlock.LAYERS, Math.min(RubbleBlock.MAX_LAYERS, here.get(RubbleBlock.LAYERS) + layers)));
            return true;
        }
        BlockState below = this.world.getBlockState(pos.down());
        if (!open(here) || below.isIn(BlockTags.LEAVES) || !below.isSideSolidFullSquare(this.world, pos.down(), Direction.UP)) {
            return false;
        }
        if (here.getBlock() instanceof TallPlantBlock && this.chunkBox.contains(pos.up())
                && this.world.getBlockState(pos.up()).getBlock() instanceof TallPlantBlock) {
            this.world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), PieceBlocks.UPDATE);
        }
        this.set(pos, piece);
        return true;
    }

    /**
     * What a fallen block of {@code state} lies on the ground as: a slab of its material, a few layers of Collapsed
     * Rubble for stone, or the block itself; null for nothing. {@code heap} piles the rubble higher.
     */
    private BlockState debris(BlockState state, float roll, BlockPos from, boolean heap) {
        Block slab = slab(state.getBlock());
        if (slab != null && roll < 0.6F) {
            return slab.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (state.getMaterial() == Material.STONE && roll < 0.85F) {
            int layers = 1 + (int) (roll * 10) % 3 + (heap ? 2 : 0);
            return RuinBlocks.COLLAPSED_RUBBLE.getDefaultState().with(RubbleBlock.LAYERS, layers);
        }
        if (state.isFullCube(this.world, from)) {
            return state.getBlock().getDefaultState();
        }
        return null;
    }

    /** The slab of {@code block}'s family (vanilla or the mod's), from its base block or its stairs. */
    private static Block slab(Block block) {
        if (slabs == null) {
            Map<Block, Block> map = new HashMap<>();
            BlockFamilies.getFamilies().forEach(family -> {
                Block slab = family.getVariant(net.minecraft.data.family.BlockFamily.Variant.SLAB);
                if (slab != null) {
                    map.put(family.getBaseBlock(), slab);
                    family.getVariants().values().forEach(variant -> map.putIfAbsent(variant, slab));
                }
            });
            for (BlockFamily family : BlockFamily.all()) {
                if (family.has(BlockFamily.Shape.SLAB)) {
                    for (Block member : family.blocks()) {
                        map.putIfAbsent(member, family.slab());
                    }
                }
            }
            slabs = map;
        }
        return slabs.get(block);
    }

    // --- Pieces with their own ruin -----------------------------------------------------------------------------

    /** The {@link Ruinable.Ruin} for piece {@code index} of the start, carving in this chunk. */
    private final class PieceRuin implements Ruinable.Ruin {
        private final long pieceSalt;

        PieceRuin(int index) {
            this.pieceSalt = Withering.this.salt + index * 0x632BE59BD9B4E019L;
        }

        @Override
        public DesolationLevel level() {
            return Withering.this.level;
        }

        @Override
        public float roll(int a, int b, int c, int k) {
            return Desolation.roll(this.pieceSalt, a, b, c, k);
        }

        @Override
        public float patches(int a, int b, int size, int k) {
            return Desolation.patches(this.pieceSalt, a, b, size, k);
        }

        @Override
        public boolean near(int x, int z, int r) {
            BlockBox c = Withering.this.chunkBox;
            return x + r >= c.getMinX() && x - r <= c.getMaxX() && z + r >= c.getMinZ() && z - r <= c.getMaxZ();
        }

        @Override
        public void breach(BlockPos from, BlockPos to, float radius, Direction out, int reach) {
            int r = (int) Math.ceil(radius * 1.25F) + 2;
            int x1 = Math.min(from.getX(), to.getX()) - r;
            int x2 = Math.max(from.getX(), to.getX()) + r;
            int z1 = Math.min(from.getZ(), to.getZ()) - r;
            int z2 = Math.max(from.getZ(), to.getZ()) + r;
            BlockBox c = Withering.this.chunkBox;
            if (x2 < c.getMinX() || x1 > c.getMaxX() || z2 < c.getMinZ() || z1 > c.getMaxZ()) {
                return;
            }
            Withering.this.counts[2]++;
            double sx = to.getX() - from.getX();
            double sy = to.getY() - from.getY();
            double sz = to.getZ() - from.getZ();
            double length2 = sx * sx + sy * sy + sz * sz;
            BlockPos.Mutable pos = new BlockPos.Mutable();
            // Top down, so what's thrown from high up lands on what's left below.
            for (int y = Math.max(from.getY(), to.getY()) + r; y >= Math.min(from.getY(), to.getY()) - r; y--) {
                for (int x = Math.max(x1, c.getMinX()); x <= Math.min(x2, c.getMaxX()); x++) {
                    for (int z = Math.max(z1, c.getMinZ()); z <= Math.min(z2, c.getMaxZ()); z++) {
                        double px = x - from.getX();
                        double py = y - from.getY();
                        double pz = z - from.getZ();
                        double t = length2 == 0 ? 0 : MathHelper.clamp((px * sx + py * sy + pz * sz) / length2, 0, 1);
                        double dx = px - t * sx;
                        double dy = py - t * sy;
                        double dz = pz - t * sz;
                        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        double edge = radius * (0.75F + 0.5F * Withering.this.roll(x, y, z, 13));
                        if (d > edge + RIM) {
                            continue;
                        }
                        pos.set(x, y, z);
                        BlockState state = Withering.this.world.getBlockState(pos);
                        if (!Withering.this.knockable(pos, state)) {
                            continue;
                        }
                        if (d > edge) {
                            // The rim round the hole, cracked by the blows.
                            Block cracked = cracked(state.getBlock());
                            if (cracked != null && Withering.this.roll(x, y, z, 14) < RIM_CRACKED) {
                                Withering.this.set(pos, cracked.getDefaultState());
                            }
                            continue;
                        }
                        Withering.this.set(pos, Blocks.AIR.getDefaultState());
                        Withering.this.thrown(pos.toImmutable(), state, out, reach);
                    }
                }
            }
        }

        @Override
        public void crumble(int x, int z, int top, int bottom, int cover, Direction out, int reach) {
            if (!Withering.this.chunkBox.contains(x, top, z) || top < bottom) {
                return;
            }
            StructureWorldAccess world = Withering.this.world;
            BlockPos.Mutable pos = new BlockPos.Mutable(x, top + 1, z);
            BlockState above = world.getBlockState(pos);
            if (!above.isAir()) {
                if (!open(above)) {
                    return;
                }
                Withering.this.set(pos, Blocks.AIR.getDefaultState());
            }
            for (int y = top; y >= bottom; y--) {
                pos.setY(y);
                BlockState state = world.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }
                boolean heaped = y > cover && state.getFluidState().isEmpty() && world.getBlockEntity(pos) == null
                        && state.getHardness(world, pos) >= 0.0F;
                if (!heaped && !Withering.this.knockable(pos, state)) {
                    continue;
                }
                Withering.this.set(pos, Blocks.AIR.getDefaultState());
                if (!heaped) {
                    Withering.this.thrown(pos.toImmutable(), state, out, reach);
                }
            }
        }

        @Override
        public void topple(BlockPos foot, Direction out, BlockState[] fallen) {
            Withering.this.counts[2]++;
            for (int i = 0; i < fallen.length; i++) {
                int x = foot.getX() + out.getOffsetX() * i;
                int z = foot.getZ() + out.getOffsetZ() * i;
                BlockPos pos = Withering.this.ground(x, foot.getY(), z, THROWN_FALL);
                if (pos == null) {
                    continue;
                }
                float roll = this.roll(x, i, z, 90);
                BlockState piece = fallen[i];
                if (roll < 0.15F) {
                    continue;
                }
                if (roll < 0.4F) {
                    piece = RuinBlocks.COLLAPSED_RUBBLE.getDefaultState().with(RubbleBlock.LAYERS, 2 + (int) (roll * 10) % 3);
                } else if (piece.contains(Properties.AXIS)) {
                    piece = piece.with(Properties.AXIS, out.getAxis());
                }
                Withering.this.land(pos, piece, 2);
            }
        }

        @Override
        public void scree(int x, int y, int z, float amount, BlockState[] materials) {
            if (amount <= 0.0F || x < Withering.this.box.getMinX() || x > Withering.this.box.getMaxX()
                    || z < Withering.this.box.getMinZ() || z > Withering.this.box.getMaxZ()) {
                return;
            }
            BlockPos pos = Withering.this.ground(x, y, z, THROWN_FALL + 8);
            if (pos == null) {
                return;
            }
            float roll = this.roll(x, 0, z, 80);
            BlockState material = materials[(int) (this.roll(x, 1, z, 81) * materials.length)];
            if (roll < 0.22F * amount) {
                if (Withering.this.land(pos, material.getBlock().getDefaultState(), 2)
                        && this.roll(x, 2, z, 82) < 0.35F * amount) {
                    // A second block, or a broken bit of one, on top.
                    BlockState top = this.roll(x, 3, z, 82) < 0.5F ? material.getBlock().getDefaultState()
                            : Withering.this.debris(material, this.roll(x, 4, z, 82), pos, false);
                    Withering.this.land(pos.up(), top, 1);
                }
            } else if (roll < 0.62F * amount) {
                int layers = Math.min(RubbleBlock.MAX_LAYERS, 1 + (int) (this.roll(x, 5, z, 83) * (1 + 5 * Math.min(amount, 1.2F))));
                Withering.this.land(pos, RuinBlocks.COLLAPSED_RUBBLE.getDefaultState().with(RubbleBlock.LAYERS, layers), layers);
            } else if (roll < 0.82F * amount) {
                Block slab = slab(material.getBlock());
                if (slab != null) {
                    Withering.this.land(pos, slab.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM), 1);
                }
            }
        }

        @Override
        public void tidy(BlockBox area) {
            BlockBox c = Withering.this.chunkBox;
            StructureWorldAccess world = Withering.this.world;
            BlockPos.Mutable pos = new BlockPos.Mutable();
            // One in from the chunk's edges, so every block a support check looks at is in this chunk.
            for (int x = Math.max(area.getMinX(), c.getMinX() + 1); x <= Math.min(area.getMaxX(), c.getMaxX() - 1); x++) {
                for (int z = Math.max(area.getMinZ(), c.getMinZ() + 1); z <= Math.min(area.getMaxZ(), c.getMaxZ() - 1); z++) {
                    for (int y = area.getMaxY(); y >= area.getMinY(); y--) {
                        BlockState state = world.getBlockState(pos.set(x, y, z));
                        if (state.isAir() || !state.getFluidState().isEmpty() || state.isFullCube(world, pos)
                                || state.canPlaceAt(world, pos)) {
                            continue;
                        }
                        Withering.this.set(pos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    // --- Overgrowth --------------------------------------------------------------------------------------------

    private void overgrow() {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = this.minX; x <= this.maxX; x++) {
            for (int z = this.minZ; z <= this.maxZ; z++) {
                if (this.faded(x, z)) {
                    continue;
                }
                boolean top = true;
                for (int y = this.maxY; y >= this.minY; y--) {
                    pos.set(x, y, z);
                    BlockState state = this.world.getBlockState(pos);
                    if (state.isAir()) {
                        this.overgrowAir(pos);
                        continue;
                    }
                    if (top) {
                        top = false;
                        this.overgrowTop(pos, state);
                    }
                }
            }
        }
    }

    /** The highest block of a column: flowers on grass, moss or soil on a roof. */
    private void overgrowTop(BlockPos pos, BlockState state) {
        BlockPos up = pos.up();
        if (!this.chunkBox.contains(up) || !this.world.getBlockState(up).isAir()) {
            return;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        float roll = this.roll(x, y, z, 20);
        if (state.isOf(Blocks.GRASS_BLOCK)) {
            if (roll < this.level.flowers) {
                this.set(up, this.flower(x, y, z));
            }
            return;
        }
        if (!built(state) || roll >= this.level.roofs) {
            return;
        }
        float kind = this.roll(x, y, z, 21);
        if (kind < 0.35F && state.isOpaqueFullCube(this.world, pos)) {
            // Soil blown up onto the roof, grassed over.
            this.set(pos, Blocks.GRASS_BLOCK.getDefaultState());
            if (kind < 0.12F) {
                this.set(up, this.flower(x, y, z));
            }
        } else if (state.isSideSolidFullSquare(this.world, pos, Direction.UP)) {
            this.set(up, Blocks.MOSS_CARPET.getDefaultState());
        }
    }

    /** Vines down the walls beside it, or moss carpet on the floor under it. */
    private void overgrowAir(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        BlockState above = this.chunkBox.contains(pos.up()) ? this.world.getBlockState(pos.up()) : Blocks.AIR.getDefaultState();
        BlockState vine = Blocks.VINE.getDefaultState();
        boolean any = false;
        for (Direction side : Direction.Type.HORIZONTAL) {
            BlockPos wall = pos.offset(side);
            if (!this.chunkBox.contains(wall)) {
                continue;
            }
            BlockState wallState = this.world.getBlockState(wall);
            if (!built(wallState) || !wallState.isSideSolidFullSquare(this.world, wall, side.getOpposite())) {
                continue;
            }
            boolean hanging = above.isOf(Blocks.VINE) && above.get(VineBlock.getFacingProperty(side));
            float roll = this.roll(x, y, z, 22 + side.getHorizontal());
            if (hanging ? roll < 0.75F : roll < this.level.vines) {
                vine = vine.with(VineBlock.getFacingProperty(side), true);
                any = true;
            }
        }
        if (any) {
            this.set(pos, vine);
            return;
        }
        BlockPos down = pos.down();
        if (this.level.floorMoss > 0.0F && this.chunkBox.contains(down) && this.roll(x, y, z, 26) < this.level.floorMoss) {
            BlockState floor = this.world.getBlockState(down);
            if (built(floor) && floor.isSideSolidFullSquare(this.world, down, Direction.UP)) {
                this.set(pos, Blocks.MOSS_CARPET.getDefaultState());
            }
        }
    }

    // --- Trees -------------------------------------------------------------------------------------------------

    /** Trees grown up inside: on open ground, at least 3 blocks inside the chunk so their crowns stay in it. */
    private void trees() {
        int cx = this.chunkBox.getMinX();
        int cz = this.chunkBox.getMinZ();
        float perChunk = this.level.trees;
        int count = (int) perChunk + (this.roll(cx, 1, cz, 30) < perChunk - (int) perChunk ? 1 : 0);
        for (int i = 0; i < count; i++) {
            int x = cx + 3 + (int) (this.roll(cx, i, cz, 31) * 10);
            int z = cz + 3 + (int) (this.roll(cx, i, cz, 32) * 10);
            if (x < this.minX || x > this.maxX || z < this.minZ || z > this.maxZ || this.faded(x, z)) {
                continue;
            }
            BlockPos.Mutable pos = new BlockPos.Mutable(x, this.maxY, z);
            while (pos.getY() > this.minY && this.world.getBlockState(pos).isAir()) {
                pos.move(Direction.DOWN);
            }
            BlockState ground = this.world.getBlockState(pos);
            if (!ground.isOf(Blocks.GRASS_BLOCK) && !ground.isOf(Blocks.DIRT) && !ground.isOf(Blocks.COARSE_DIRT)) {
                continue;
            }
            RegistryKey<ConfiguredFeature<?, ?>> tree = TREES.get((int) (this.roll(x, i, z, 33) * TREES.size()));
            BlockPos base = pos.up().toImmutable();
            Random random = Random.create((long) (this.roll(x, base.getY(), z, 34) * Long.MAX_VALUE));
            this.world.getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE).getEntry(tree)
                    .ifPresent(entry -> entry.value().generate(this.world, this.generator, random, base));
        }
    }

    // --- Support -----------------------------------------------------------------------------------------------
    //
    // What the breaches leave without support comes down. Two passes, both deterministic per chunk:
    // supports() decides from the planned breaches alone (every chunk plans all of them) how much of each piece
    // loses its support and applies that column by column; fragments() then drops whatever is left floating in
    // this chunk, judged only from this chunk's own (finished) blocks.

    /**
     * A piece's share of its wall band (the columns within {@link #WALL_BAND} of its box's sides) under its wall
     * breaches at or over which its roof caves in (the chance grows from here to {@link #HEAP_DAMAGE}).
     */
    static final float CAVE_DAMAGE = 0.25F;
    /** The share at or over which the whole building comes down in a heap, its walls left 1 or 2 blocks high. */
    static final float HEAP_DAMAGE = 0.55F;
    static final int WALL_BAND = 2;
    /** How far past a wall breach (in its radii) the roof may still fall in, less likely further out. */
    static final float SAG_REACH = 1.0F;
    /** The least share of a heaped building's blocks that lies on the ground as debris. */
    static final float HEAP_RUBBLE = 0.7F;
    /** How far above a piece's box its roof is looked for. */
    static final int ROOF_SCAN = 3;

    /** A breach planned for piece {@code piece} (counted from 1): centre, radius and face (0-3 walls, 4 roof, 5 inside). */
    private record Breach(int piece, int x, int y, int z, float radius, int face) {
        double distance(int px, int pz) {
            return Math.sqrt((double) (px - this.x) * (px - this.x) + (double) (pz - this.z) * (pz - this.z));
        }
    }

    /** Every piece's planned breaches, whether they reach this chunk or not ({@link #plannedBreach}). */
    private final List<Breach> planned = new ArrayList<>();

    /** Open space a falling block passes through or lands in: air or replaceable, liquids included (unlike {@link #open}). */
    private static boolean clear(BlockState state) {
        return state.isAir() || state.isReplaceable();
    }

    /** Whether natural (not stripped) logs are a tree's, which a building's collapse leaves alone. */
    private static boolean naturalLog(BlockState state) {
        return state.isIn(BlockTags.LOGS) && !Registries.BLOCK.getId(state.getBlock()).getPath().startsWith("stripped_");
    }

    /**
     * Whether {@code pos} can come down for want of support: built (or a log, if {@code logs}), breakable, and
     * holding no block entity but a banner or a head.
     */
    private boolean loose(BlockPos pos, BlockState state, boolean logs) {
        if (!(built(state) && !naturalLog(state) || logs && state.isIn(BlockTags.LOGS)) || state.getHardness(this.world, pos) < 0.0F) {
            return false;
        }
        BlockEntity blockEntity = this.world.getBlockEntity(pos);
        return blockEntity == null || blockEntity instanceof BannerBlockEntity || blockEntity instanceof SkullBlockEntity;
    }

    /**
     * Each piece in this chunk with wall breaches: how much of its wall band they cover (from the plan, so every
     * chunk agrees) sets its fate. Under {@link #CAVE_DAMAGE} only what's over and next to each breach comes down;
     * from there up its roof falls in in patches, more of it the more the walls are breached; from
     * {@link #HEAP_DAMAGE} up it's a heap. Pieces of {@link #BIG_PIECE} or more (a whole goblin camp) only ever
     * lose what's over and next to their breaches.
     */
    private void supports() {
        int index = 0;
        for (StructurePiece piece : this.start.getChildren()) {
            index++;
            BlockBox b = piece.getBoundingBox();
            if (b.getMaxX() < this.chunkBox.getMinX() || b.getMinX() > this.chunkBox.getMaxX()
                    || b.getMaxZ() < this.chunkBox.getMinZ() || b.getMinZ() > this.chunkBox.getMaxZ()) {
                continue;
            }
            List<Breach> breaches = new ArrayList<>();
            for (Breach breach : this.planned) {
                // Its own wall breaches, and any other piece's that reach into it (a goblin camp's or the orc
                // grounds' breaches land on the buildings inside them).
                if (breach.face() != 4 && (breach.piece() == index || reaches(breach, b))) {
                    breaches.add(breach);
                }
            }
            if (breaches.isEmpty()) {
                continue;
            }
            float area = b.getBlockCountX() * b.getBlockCountZ() / (float) (PIECE_SIZE * PIECE_SIZE);
            float damage = area >= BIG_PIECE ? 0.0F : wallDamage(b, breaches);
            boolean heap = damage >= HEAP_DAMAGE;
            float caved = MathHelper.clamp((damage - CAVE_DAMAGE) / (HEAP_DAMAGE - CAVE_DAMAGE), 0.0F, 1.0F);
            List<BlockPos> fallen = new ArrayList<>();
            List<BlockState> states = new ArrayList<>();
            for (int x = Math.max(b.getMinX(), this.chunkBox.getMinX()); x <= Math.min(b.getMaxX(), this.chunkBox.getMaxX()); x++) {
                for (int z = Math.max(b.getMinZ(), this.chunkBox.getMinZ()); z <= Math.min(b.getMaxZ(), this.chunkBox.getMaxZ()); z++) {
                    this.unsupported(b, breaches, x, z, caved, heap, fallen, states);
                }
            }
            if (!fallen.isEmpty()) {
                DnDClasses.LOGGER.debug("[Desolation] piece {} {} {}: wall damage {}, {}, {} blocks fell", index,
                        piece.getClass().getSimpleName(), b.getCenter(), String.format("%.2f", damage),
                        heap ? "heaped" : caved > 0.0F ? "caved " + String.format("%.2f", caved) : "local", fallen.size());
            }
            float chance = heap ? Math.max(this.level.rubble, HEAP_RUBBLE) : this.level.rubble;
            for (int i = 0; i < fallen.size(); i++) {
                BlockPos from = fallen.get(i);
                if (this.roll(from.getX(), from.getY(), from.getZ(), 19) < chance) {
                    this.settle(from, states.get(i), heap);
                }
            }
        }
    }

    /** Whether {@code breach} (another piece's) reaches into {@code b}, between its floor and its top. */
    private static boolean reaches(Breach breach, BlockBox b) {
        int r = (int) Math.ceil(breach.radius());
        return breach.x() + r >= b.getMinX() && breach.x() - r <= b.getMaxX() && breach.z() + r >= b.getMinZ()
                && breach.z() - r <= b.getMaxZ() && breach.y() + r >= b.getMinY() && breach.y() - r <= b.getMaxY();
    }

    /** The share of {@code b}'s wall band (columns within {@link #WALL_BAND} of its sides) under a wall breach. */
    private static float wallDamage(BlockBox b, List<Breach> breaches) {
        int covered = 0;
        int total = 0;
        for (int x = b.getMinX(); x <= b.getMaxX(); x++) {
            for (int z = b.getMinZ(); z <= b.getMaxZ(); z++) {
                int edge = Math.min(Math.min(x - b.getMinX(), b.getMaxX() - x), Math.min(z - b.getMinZ(), b.getMaxZ() - z));
                if (edge >= WALL_BAND) {
                    continue;
                }
                total++;
                for (Breach breach : breaches) {
                    if (breach.face() != 4 && breach.distance(x, z) <= breach.radius()) {
                        covered++;
                        break;
                    }
                }
            }
        }
        return total == 0 ? 0.0F : covered / (float) total;
    }

    /**
     * Column x/z of piece {@code b}: its top run of built blocks (a roof, or a wall and the roof on it) comes down,
     * into {@code fallen}, if
     * <ul>
     *   <li>it's over a breach (within the breach's ragged radius): everything above the breach's centre;</li>
     *   <li>the piece is a heap: everything above 1 or 2 blocks over its floor;</li>
     *   <li>it's a ceiling (open space under it) and either near a breach (likelier the nearer, out to
     *   {@link #SAG_REACH} radii past it) or in a patch that caves in ({@code caved} of the roof does).</li>
     * </ul>
     * A column whose highest solid block isn't built (the ground over a dug-out room, a tree) is held up.
     */
    private void unsupported(BlockBox b, List<Breach> breaches, int x, int z, float caved, boolean heap,
                             List<BlockPos> fallen, List<BlockState> states) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int bottom = Math.max(b.getMinY(), this.chunkBox.getMinY());
        int hi = Math.min(b.getMaxY() + ROOF_SCAN, this.chunkBox.getMaxY());
        BlockState state = null;
        for (; hi >= bottom; hi--) {
            state = this.world.getBlockState(pos.set(x, hi, z));
            if (!clear(state) && !state.isIn(BlockTags.LEAVES)) {
                break;
            }
        }
        if (hi < bottom || !this.loose(pos, state, heap)) {
            return;
        }
        int lo = hi;
        while (lo - 1 >= bottom && this.loose(pos.set(x, lo - 1, z), this.world.getBlockState(pos), heap)) {
            lo--;
        }
        boolean ceiling = lo - 1 >= bottom && clear(this.world.getBlockState(pos.set(x, lo - 1, z)));
        int shadow = Integer.MAX_VALUE;
        float sag = 0.0F;
        for (Breach breach : breaches) {
            double d = breach.distance(x, z);
            float reach = breach.radius() * (0.75F + 0.5F * this.roll(x, breach.y(), z, 80));
            if (d <= reach) {
                shadow = Math.min(shadow, breach.y());
            } else {
                sag = Math.max(sag, 1.0F - (float) (d - reach) / (breach.radius() * SAG_REACH));
            }
        }
        int from;
        if (heap) {
            from = Math.max(lo, b.getMinY() + 2 + (this.roll(x, 0, z, 81) < 0.5F ? 1 : 0));
        } else if (shadow != Integer.MAX_VALUE) {
            from = Math.max(lo, shadow + 1);
        } else if (ceiling && (this.roll(x, 0, z, 82) < sag || caved > 0.0F && Desolation.patches(this.salt, x, z, 4, 83) < caved)) {
            from = lo;
        } else {
            return;
        }
        for (int y = from; y <= hi; y++) {
            state = this.world.getBlockState(pos.set(x, y, z));
            if (this.loose(pos, state, heap)) {
                this.set(pos, Blocks.AIR.getDefaultState());
                fallen.add(pos.toImmutable());
                states.add(state);
                this.counts[3]++;
            }
        }
    }

    /**
     * Fragments: built blocks (and logs) in this chunk that touch nothing holding them up fall. Each group of
     * touching blocks (edges and corners count, so stepped roofs hold together) is held up if any of it is ground,
     * stone, a container or other block entity, or lies on the edge of this chunk's slice of the structure, where
     * it may carry on into the next chunk. Only this chunk's blocks are read, and they're final by now.
     */
    private void fragments() {
        int sx = this.maxX - this.minX + 1;
        int sy = this.maxY - this.minY + 1;
        int sz = this.maxZ - this.minZ + 1;
        if (sx <= 0 || sy <= 0 || sz <= 0) {
            return;
        }
        boolean[] seen = new boolean[sx * sy * sz];
        BlockPos.Mutable pos = new BlockPos.Mutable();
        IntArrayList queue = new IntArrayList();
        IntArrayList group = new IntArrayList();
        List<BlockPos> fallen = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        for (int i = 0; i < seen.length; i++) {
            if (seen[i]) {
                continue;
            }
            seen[i] = true;
            if (!this.member(this.at(i, pos))) {
                continue;
            }
            boolean held = false;
            queue.clear();
            group.clear();
            queue.add(i);
            while (!queue.isEmpty()) {
                int c = queue.removeInt(queue.size() - 1);
                group.add(c);
                this.at(c, pos);
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                if (!held && (x == this.minX || x == this.maxX || y == this.minY || y == this.maxY || z == this.minZ || z == this.maxZ
                        || !this.loose(pos, this.world.getBlockState(pos), true))) {
                    held = true;
                }
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            int nz = z + dz;
                            if (nx < this.minX || nx > this.maxX || ny < this.minY || ny > this.maxY || nz < this.minZ || nz > this.maxZ) {
                                continue;
                            }
                            int n = ((ny - this.minY) * sz + (nz - this.minZ)) * sx + (nx - this.minX);
                            if (seen[n]) {
                                continue;
                            }
                            seen[n] = true;
                            if (this.member(pos.set(nx, ny, nz))) {
                                queue.add(n);
                            }
                        }
                    }
                }
            }
            if (held) {
                continue;
            }
            for (int k = 0; k < group.size(); k++) {
                this.at(group.getInt(k), pos);
                states.add(this.world.getBlockState(pos));
                fallen.add(pos.toImmutable());
                this.set(pos, Blocks.AIR.getDefaultState());
                this.counts[3]++;
            }
        }
        for (int i = 0; i < fallen.size(); i++) {
            BlockPos from = fallen.get(i);
            if (this.roll(from.getX(), from.getY(), from.getZ(), 87) < this.level.rubble) {
                this.settle(from, states.get(i), false);
            }
        }
    }

    /** Sets {@code pos} to cell {@code i} of this chunk's slice (x fastest, then z, then y). */
    private BlockPos.Mutable at(int i, BlockPos.Mutable pos) {
        int sx = this.maxX - this.minX + 1;
        int sz = this.maxZ - this.minZ + 1;
        return pos.set(this.minX + i % sx, this.minY + i / (sx * sz), this.minZ + i / sx % sz);
    }

    /** Whether {@code pos} is solid enough to hold up or be held up: not air, plants, fluids or leaves. */
    private boolean member(BlockPos pos) {
        BlockState state = this.world.getBlockState(pos);
        return !clear(state) && !state.isIn(BlockTags.LEAVES);
    }

    /**
     * Lays a fallen block of {@code state} on what's under {@code from}, scattered up to a block sideways. A
     * {@code heap} piles up: rubble on rubble, and slabs on slabs of the same kind into double slabs.
     */
    private void settle(BlockPos from, BlockState state, boolean heap) {
        int dx = (int) (this.roll(from.getX(), from.getY(), from.getZ(), 84) * 3) - 1;
        int dz = (int) (this.roll(from.getX(), from.getY(), from.getZ(), 85) * 3) - 1;
        BlockPos.Mutable pos = new BlockPos.Mutable(from.getX() + dx, from.getY(), from.getZ() + dz);
        if (!this.chunkBox.contains(pos) || !clear(this.world.getBlockState(pos))) {
            return;
        }
        for (int i = 0; i < RUBBLE_FALL && pos.getY() > this.chunkBox.getMinY() && clear(this.world.getBlockState(pos.down())); i++) {
            pos.move(Direction.DOWN);
        }
        BlockPos down = pos.down();
        BlockState below = this.world.getBlockState(down);
        if (below.isOf(RuinBlocks.COLLAPSED_RUBBLE) && below.get(RubbleBlock.LAYERS) < RubbleBlock.MAX_LAYERS) {
            int layers = below.get(RubbleBlock.LAYERS) + (heap ? 2 : 1);
            this.set(down, below.with(RubbleBlock.LAYERS, Math.min(RubbleBlock.MAX_LAYERS, layers)));
            return;
        }
        Block slab = slab(state.getBlock());
        if (heap && slab != null && below.isOf(slab) && below.get(SlabBlock.TYPE) == SlabType.BOTTOM) {
            this.set(down, below.with(SlabBlock.TYPE, SlabType.DOUBLE));
            return;
        }
        boolean onRubble = below.isOf(RuinBlocks.COLLAPSED_RUBBLE);
        if (!onRubble && !below.isSideSolidFullSquare(this.world, down, Direction.UP)) {
            return;
        }
        BlockState piece = this.debris(state, this.roll(from.getX(), from.getY(), from.getZ(), 86), from, heap);
        if (piece != null) {
            this.set(pos, piece);
        }
    }
}
