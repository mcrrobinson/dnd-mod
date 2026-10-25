package mattonfire.dnd.world.underdark.rift;

import static mattonfire.dnd.world.underdark.rift.UnderdarkRiftStructure.CEILING;
import static mattonfire.dnd.world.underdark.rift.UnderdarkRiftStructure.FLOOR;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.MultifaceGrowthBlock;
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
 * The rift's sinkhole: a ragged 7x7 shaft from the chasm's floor up to the surface, ringed with tuff and cobbled
 * deepslate at the top under a two-block lip of coarse dirt, with lanterns on posts. One-wide ledges spiral up its
 * walls: step k of the climb is ring cell {@code k % 24} of the shaft's outer ring, at height {@link #ledgeY}
 * (one block up every {@link #CELLS_PER_RISE} cells), so you can walk and hop all the way up or down without
 * placing a block, and each turn of the spiral is {@link #LAP_RISE} blocks above the one below.
 */
public class RiftSinkholePiece extends StructurePiece {
    /** The shaft's open half-width (7 wide); its wall ring, then a two-block shell. */
    public static final int RADIUS = 3;
    private static final int WALL = RADIUS + 1;
    private static final int SHELL = WALL + 2;
    /** Cells walked per block climbed along the ledge. */
    public static final int CELLS_PER_RISE = 2;
    /** The 24 cells of the ring at {@link #RADIUS}, in climbing order (each next to the last). */
    public static final int[][] RING = ring();
    public static final int LAP_RISE = RING.length / CELLS_PER_RISE;
    /** How far above the ground the shaft is cleared (hillsides, trees). */
    private static final int CLEAR_ABOVE = 8;

    private final BlockPos bottom;
    private final int top;
    private final int ux;
    private final int uz;
    private final long seed;

    /**
     * @param bottom the shaft's axis at the chasm floor's height
     * @param top    the ground's y at the top
     */
    public RiftSinkholePiece(BlockPos bottom, int top, int ux, int uz, long seed) {
        super(UnderdarkRiftStructures.SINKHOLE, 0, new BlockBox(bottom.getX() - SHELL, bottom.getY(), bottom.getZ() - SHELL,
                bottom.getX() + SHELL, top + CLEAR_ABOVE, bottom.getZ() + SHELL));
        this.bottom = bottom;
        this.top = top;
        this.ux = ux;
        this.uz = uz;
        this.seed = seed;
        this.setOrientation(Direction.fromVector(ux, 0, uz));
    }

    public RiftSinkholePiece(NbtCompound nbt) {
        super(UnderdarkRiftStructures.SINKHOLE, nbt);
        this.bottom = NbtHelper.toBlockPos(nbt.getCompound("Bottom"));
        this.top = nbt.getInt("Top");
        this.ux = nbt.getInt("UX");
        this.uz = nbt.getInt("UZ");
        this.seed = nbt.getLong("Seed");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Bottom", NbtHelper.fromBlockPos(this.bottom));
        nbt.putInt("Top", this.top);
        nbt.putInt("UX", this.ux);
        nbt.putInt("UZ", this.uz);
        nbt.putLong("Seed", this.seed);
    }

    /** The ground's y at the top of the shaft. */
    public int top() {
        return this.top;
    }

    /** The shaft's axis at ground level (the top of the hole). */
    public BlockPos mouth() {
        return new BlockPos(this.bottom.getX(), this.top, this.bottom.getZ());
    }

    /** The y of the ledge block of climbing step k (you stand a block above it). Steps 0 and 1 are the floor. */
    public static int ledgeY(int k) {
        return FLOOR + k / CELLS_PER_RISE;
    }

    /** How many steps the climb has: the last one is level with the ground. */
    public int steps() {
        return (this.top - FLOOR) * CELLS_PER_RISE + 1;
    }

    /** Where your feet are on climbing step k. */
    public BlockPos stepFeet(int k) {
        int[] cell = RING[k % RING.length];
        return new BlockPos(this.bottom.getX() + cell[0], ledgeY(k) + 1, this.bottom.getZ() + cell[1]);
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random chunkRandom, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        int sx = this.bottom.getX();
        int sz = this.bottom.getZ();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -SHELL; dx <= SHELL; dx++) {
            for (int dz = -SHELL; dz <= SHELL; dz++) {
                if (!chunkBox.contains(sx + dx, chunkBox.getMinY(), sz + dz)) {
                    continue;
                }
                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                int cell = ring == RADIUS ? cellOf(dx, dz) : -1;
                for (int y = FLOOR + 1; y <= this.top + CLEAR_ABOVE; y++) {
                    pos.set(sx + dx, y, sz + dz);
                    float n = UnderdarkRiftStructures.noise(this.seed, pos.getX(), y, pos.getZ());
                    BlockState state = this.blockAt(dx, y, dz, ring, cell, n);
                    if (state == null) {
                        continue;
                    }
                    if (state == FILL) {
                        BlockState old = world.getBlockState(pos);
                        if (old.isAir() || !old.getFluidState().isEmpty() || old.isReplaceable()) {
                            world.setBlockState(pos, rock(y, n), PieceBlocks.UPDATE);
                        }
                        continue;
                    }
                    world.setBlockState(pos, state, PieceBlocks.UPDATE);
                }
            }
        }
        // Lanterns on posts at the four corners of the lip.
        for (int[] corner : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            int x = sx + corner[0] * (WALL + 1);
            int z = sz + corner[1] * (WALL + 1);
            set(world, chunkBox, new BlockPos(x, this.top, z), Blocks.COBBLED_DEEPSLATE.getDefaultState());
            set(world, chunkBox, new BlockPos(x, this.top + 1, z), Blocks.COBBLED_DEEPSLATE_WALL.getDefaultState());
            set(world, chunkBox, new BlockPos(x, this.top + 2, z), Blocks.CHAIN.getDefaultState());
            set(world, chunkBox, new BlockPos(x, this.top + 3, z), Blocks.LANTERN.getDefaultState()
                    .with(LanternBlock.HANGING, false));
        }
    }

    /** Marks a shell cell: fill it with rock only if it's air, fluid or replaceable. */
    private static final BlockState FILL = Blocks.BARRIER.getDefaultState();

    /** What goes at (dx, y, dz) from the axis, {@link #FILL}, or null to leave it. */
    private BlockState blockAt(int dx, int y, int dz, int ring, int cell, float n) {
        BlockState air = Blocks.AIR.getDefaultState();
        if (ring <= RADIUS) {
            if (y > this.top) {
                return air;
            }
            if (cell >= 0) {
                // A ledge cell carries step k = cell + 24m at ledgeY(k), one block thick under it.
                for (int k = cell; k < this.steps(); k += RING.length) {
                    int ly = ledgeY(k);
                    if (ly <= FLOOR) {
                        continue;
                    }
                    if (y == ly || y == ly - 1) {
                        return y > this.top - 4 ? Blocks.TUFF.getDefaultState() : rock(y, n);
                    }
                }
                // Glow lichen on the wall behind, now and then (not in the chasm, where there's no wall).
                if (y > CEILING + 3 && n < 0.12F && !isCorner(dx, dz)) {
                    Direction out = Direction.fromVector(Integer.signum(Math.abs(dx) == RADIUS ? dx : 0), 0,
                            Integer.signum(Math.abs(dz) == RADIUS ? dz : 0));
                    if (out != null) {
                        return Blocks.GLOW_LICHEN.getDefaultState().with(MultifaceGrowthBlock.getProperty(out), true);
                    }
                }
            }
            return air;
        }
        if (y <= CEILING) {
            return null; // the chasm's own walls
        }
        if (ring == WALL) {
            if (y > this.top) {
                return air;
            }
            if (y == this.top) {
                return Blocks.COARSE_DIRT.getDefaultState();
            }
            if (y > this.top - 6) {
                return (n < 0.55F ? Blocks.TUFF : Blocks.COBBLED_DEEPSLATE).getDefaultState();
            }
            return rock(y, n);
        }
        // The shell.
        if (y > this.top) {
            return ring == WALL + 1 && y <= this.top + 2 ? air : null;
        }
        if (y == this.top && ring == WALL + 1) {
            return Blocks.COARSE_DIRT.getDefaultState();
        }
        return y < this.top ? FILL : null;
    }

    private static BlockState rock(int y, float n) {
        if (y < 0) {
            return (n < 0.8F ? Blocks.DEEPSLATE : Blocks.COBBLED_DEEPSLATE).getDefaultState();
        }
        return (n < 0.8F ? Blocks.STONE : Blocks.ANDESITE).getDefaultState();
    }

    private static boolean isCorner(int dx, int dz) {
        return Math.abs(dx) == RADIUS && Math.abs(dz) == RADIUS;
    }

    private static int cellOf(int dx, int dz) {
        for (int i = 0; i < RING.length; i++) {
            if (RING[i][0] == dx && RING[i][1] == dz) {
                return i;
            }
        }
        return -1;
    }

    /** The square ring at {@link #RADIUS}, walked clockwise (seen from above) from its north-west corner. */
    private static int[][] ring() {
        List<int[]> cells = new ArrayList<>();
        int r = RADIUS;
        for (int x = -r; x < r; x++) {
            cells.add(new int[] {x, -r});
        }
        for (int z = -r; z < r; z++) {
            cells.add(new int[] {r, z});
        }
        for (int x = r; x > -r; x--) {
            cells.add(new int[] {x, r});
        }
        for (int z = r; z > -r; z--) {
            cells.add(new int[] {-r, z});
        }
        return cells.toArray(new int[0][]);
    }

    private static void set(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, state, PieceBlocks.UPDATE);
        }
    }
}
