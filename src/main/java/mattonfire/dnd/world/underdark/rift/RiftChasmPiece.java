package mattonfire.dnd.world.underdark.rift;

import static mattonfire.dnd.world.underdark.rift.UnderdarkRiftStructure.CEILING;
import static mattonfire.dnd.world.underdark.rift.UnderdarkRiftStructure.CEILING_JAG;
import static mattonfire.dnd.world.underdark.rift.UnderdarkRiftStructure.FLOOR;

import mattonfire.dnd.classes.Blocks.RubbleBlock;
import mattonfire.dnd.classes.Registry.RiftBlocks;
import mattonfire.dnd.classes.Registry.RuinBlocks;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MultifaceGrowthBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.enums.Thickness;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * The rift's chasm: a jagged slit about 7 wide, 21 long and 28 tall in the deepslate, from y {@link
 * UnderdarkRiftStructure#FLOOR} to about {@link UnderdarkRiftStructure#CEILING}, along the axis (ux, uz). A 3x5
 * pool of Rift Veil edged with Faerzress Stone lies at its back end (local t -9..-5), a 4x4 ledge of Collapsed
 * Rubble beside it (t -3..0), and the sinkhole comes down at its front end (t +4..+10, {@link RiftSinkholePiece}).
 * Its walls are a quarter Faerzress Stone and hung with glow lichen, with dripstone from the roof, and a two-block
 * shell of deepslate keeps aquifers, water and lava out (as the Beholder lair's).
 * <p>
 * Local coordinates: t runs along the chasm (+ toward the sinkhole), s across it. Built once per chunk it
 * overlaps: per-block choices come from a position hash and the rest from its seed.
 */
public class RiftChasmPiece extends StructurePiece {
    /** Half the chasm's width before its jags (so 7 wide), and half its length (21 long). */
    private static final int HALF_WIDTH = 3;
    private static final int HALF_LENGTH = 10;
    private static final int SHELL = 3;
    /** The pool: t from {@link #POOL_T0} to {@link #POOL_T1}, s from -1 to 1. */
    public static final int POOL_T0 = -9;
    public static final int POOL_T1 = -5;
    /** The rubble ledge: t -3..0, s -2..1. */
    private static final int LEDGE_T0 = -3;
    private static final int LEDGE_T1 = 0;
    private static final int LEDGE_S0 = -2;
    private static final int LEDGE_S1 = 1;
    private static final float FAERZRESS_WALL = 0.25F;
    private static final float LICHEN = 0.22F;
    private static final int DRIPSTONES = 9;

    private final BlockPos center;
    private final int ux;
    private final int uz;
    private final long seed;

    public RiftChasmPiece(BlockPos center, int ux, int uz, long seed) {
        super(UnderdarkRiftStructures.CHASM, 0, new BlockBox(center.getX() - 15, FLOOR - SHELL - 1, center.getZ() - 15,
                center.getX() + 15, CEILING + CEILING_JAG + SHELL + 1, center.getZ() + 15));
        this.center = center;
        this.ux = ux;
        this.uz = uz;
        this.seed = seed;
        this.setOrientation(Direction.fromVector(ux, 0, uz));
    }

    public RiftChasmPiece(NbtCompound nbt) {
        super(UnderdarkRiftStructures.CHASM, nbt);
        this.center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        this.ux = nbt.getInt("UX");
        this.uz = nbt.getInt("UZ");
        this.seed = nbt.getLong("Seed");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putInt("UX", this.ux);
        nbt.putInt("UZ", this.uz);
        nbt.putLong("Seed", this.seed);
    }

    /** The world position of local (t, s) at height y. */
    public BlockPos at(int t, int y, int s) {
        return new BlockPos(this.center.getX() + t * this.ux - s * this.uz, y, this.center.getZ() + t * this.uz + s * this.ux);
    }

    /** Where someone coming up through a Rift Anchor arrives: on the rubble ledge, feet a block above it. */
    public BlockPos ledge() {
        return this.at(-2, FLOOR + 2, 0);
    }

    /** The yaw that looks from the ledge toward the pool. */
    public float yawToPool() {
        return Direction.fromVector(-this.ux, 0, -this.uz).asRotation();
    }

    /** The middle of the Rift Veil pool (at the veil's level). */
    public BlockPos poolCenter() {
        return this.at((POOL_T0 + POOL_T1) / 2, FLOOR, 0);
    }

    private int halfWidth(int t, int y) {
        return HALF_WIDTH + (UnderdarkRiftStructures.noise(this.seed, t >> 1, y >> 2, 11) > 0.6F ? 1 : 0);
    }

    private int halfLength(int s, int y) {
        return HALF_LENGTH + (UnderdarkRiftStructures.noise(this.seed, s, y >> 2, 23) > 0.7F ? 1 : 0);
    }

    private int ceiling(int t, int s) {
        return CEILING + (int) (UnderdarkRiftStructures.noise(this.seed, t >> 1, 37, s >> 1) * (CEILING_JAG + 1));
    }

    private boolean inside(int t, int y, int s) {
        return y > FLOOR && y <= this.ceiling(t, s) && Math.abs(s) <= this.halfWidth(t, y)
                && Math.abs(t) <= this.halfLength(s, y);
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random chunkRandom, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BlockBox box = this.getBoundingBox();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int cx = this.center.getX();
        int cz = this.center.getZ();
        for (int x = Math.max(box.getMinX(), chunkBox.getMinX()); x <= Math.min(box.getMaxX(), chunkBox.getMaxX()); x++) {
            for (int z = Math.max(box.getMinZ(), chunkBox.getMinZ()); z <= Math.min(box.getMaxZ(), chunkBox.getMaxZ()); z++) {
                int t = (x - cx) * this.ux + (z - cz) * this.uz;
                int s = -(x - cx) * this.uz + (z - cz) * this.ux;
                if (Math.abs(t) > HALF_LENGTH + 1 + SHELL || Math.abs(s) > HALF_WIDTH + 1 + SHELL) {
                    continue;
                }
                int ceiling = this.ceiling(t, s);
                for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                    pos.set(x, y, z);
                    float n = UnderdarkRiftStructures.noise(this.seed, x, y, z);
                    int hw = this.halfWidth(t, y);
                    int hl = this.halfLength(s, y);
                    boolean footprint = Math.abs(s) <= hw && Math.abs(t) <= hl;
                    if (y > FLOOR && y <= ceiling && footprint) {
                        world.setBlockState(pos, this.open(t, y, s, hw, n), PieceBlocks.UPDATE);
                    } else if (y == FLOOR && footprint) {
                        world.setBlockState(pos, floor(t, s, n), PieceBlocks.UPDATE);
                    } else {
                        int d = Math.max(Math.max(Math.abs(s) - hw, Math.abs(t) - hl), Math.max(y - ceiling, FLOOR - y));
                        if (d == 1) {
                            boolean underPool = y == FLOOR - 1 && t >= POOL_T0 - 1 && t <= POOL_T1 + 1 && Math.abs(s) <= 2;
                            world.setBlockState(pos, (underPool || n < FAERZRESS_WALL ? UnderdarkBlocks.FAERZRESS_STONE
                                    : Blocks.DEEPSLATE).getDefaultState(), PieceBlocks.UPDATE);
                        } else if (d <= SHELL) {
                            BlockState state = world.getBlockState(pos);
                            if (state.isAir() || !state.getFluidState().isEmpty() || state.isReplaceable()) {
                                world.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState(), PieceBlocks.UPDATE);
                            }
                        }
                    }
                }
            }
        }

        // The rubble ledge beside the pool, where the anchor brings you up.
        for (int t = LEDGE_T0; t <= LEDGE_T1; t++) {
            for (int s = LEDGE_S0; s <= LEDGE_S1; s++) {
                boolean corner = (t == LEDGE_T0 || t == LEDGE_T1) && (s == LEDGE_S0 || s == LEDGE_S1);
                set(world, chunkBox, this.at(t, FLOOR + 1, s), RuinBlocks.COLLAPSED_RUBBLE.getDefaultState()
                        .with(RubbleBlock.LAYERS, corner ? 4 : RubbleBlock.MAX_LAYERS));
            }
        }

        // Dripstone from the roof, clear of the sinkhole's end.
        Random random = Random.create(this.seed);
        for (int i = 0; i < DRIPSTONES; i++) {
            int t = -9 + random.nextInt(13);
            int s = -2 + random.nextInt(5);
            int length = 1 + random.nextInt(3);
            int top = this.ceiling(t, s);
            for (int k = 0; k < length; k++) {
                Thickness thickness = k == length - 1 ? Thickness.TIP : k == 0 && length > 2 ? Thickness.BASE
                        : length == 1 ? Thickness.TIP : Thickness.MIDDLE;
                if (k == length - 2 && length > 1) {
                    thickness = Thickness.FRUSTUM;
                }
                set(world, chunkBox, this.at(t, top - k, s), Blocks.POINTED_DRIPSTONE.getDefaultState()
                        .with(PointedDripstoneBlock.VERTICAL_DIRECTION, Direction.DOWN)
                        .with(PointedDripstoneBlock.THICKNESS, thickness));
            }
            set(world, chunkBox, this.at(t, top + 1, s), Blocks.DRIPSTONE_BLOCK.getDefaultState());
        }
    }

    /** An open cell: air, or glow lichen on the side wall it backs onto. */
    private BlockState open(int t, int y, int s, int hw, float n) {
        if (Math.abs(s) == hw && y > FLOOR + 1 && n < LICHEN && !(t >= 3)) {
            // The wall is the next cell out across the chasm.
            int ws = s > 0 ? 1 : -1;
            Direction wall = Direction.fromVector(-ws * this.uz, 0, ws * this.ux);
            if (wall != null) {
                return Blocks.GLOW_LICHEN.getDefaultState().with(MultifaceGrowthBlock.getProperty(wall), true);
            }
        }
        return Blocks.AIR.getDefaultState();
    }

    /** The floor: deepslate and tuff, with the veil pool edged in Faerzress Stone at the back end. */
    private static BlockState floor(int t, int s, float n) {
        if (t >= POOL_T0 && t <= POOL_T1 && Math.abs(s) <= 1) {
            return RiftBlocks.RIFT_VEIL.getDefaultState();
        }
        if (t >= POOL_T0 - 1 && t <= POOL_T1 + 1 && Math.abs(s) <= 2) {
            return UnderdarkBlocks.FAERZRESS_STONE.getDefaultState();
        }
        return (n < 0.5F ? Blocks.DEEPSLATE : n < 0.75F ? Blocks.TUFF : Blocks.COBBLED_DEEPSLATE).getDefaultState();
    }

    private static void set(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, state, PieceBlocks.UPDATE);
        }
    }
}
