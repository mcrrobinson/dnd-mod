package mattonfire.dnd.world.gen.beholder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.StairsBlock;
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
 * The way down to a Beholder's cavern: a 5x5 deepslate-brick shaft round a central pillar, with a
 * spiral stair (one step up per block round) from the tunnel at its foot to a crumbling ring of
 * wall on the surface. Its centre is {@link #OFFSET} blocks east of the cavern's.
 */
public class BeholderShaftPiece extends StructurePiece {
    /** How far east of the cavern centre the shaft stands. */
    public static final int OFFSET = BeholderCavernPiece.RADIUS + 4;

    /** The eight cells round the pillar, in climbing order (clockwise seen from above). */
    private static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};

    private final BlockPos bottom;
    private final int top;
    private final long seed;

    /**
     * @param bottom centre of the shaft at the cavern floor's height
     * @param top    y of the ground at the top
     */
    public BeholderShaftPiece(BlockPos bottom, int top, long seed) {
        super(BeholderLairStructures.SHAFT, 0, new BlockBox(bottom.getX() - 2, bottom.getY(), bottom.getZ() - 2,
                bottom.getX() + 2, top + 3, bottom.getZ() + 2));
        this.bottom = bottom;
        this.top = top;
        this.seed = seed;
    }

    public BeholderShaftPiece(NbtCompound nbt) {
        super(BeholderLairStructures.SHAFT, nbt);
        this.bottom = NbtHelper.toBlockPos(nbt.getCompound("Bottom"));
        this.top = nbt.getInt("Top");
        this.seed = nbt.getLong("Seed");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Bottom", NbtHelper.fromBlockPos(this.bottom));
        nbt.putInt("Top", this.top);
        nbt.putLong("Seed", this.seed);
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random chunkRandom, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        int sx = this.bottom.getX();
        int y0 = this.bottom.getY();
        int sz = this.bottom.getZ();
        for (int y = y0; y <= this.top + 3; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = new BlockPos(sx + dx, y, sz + dz);
                    if (!chunkBox.contains(pos)) {
                        continue;
                    }
                    float n = BeholderLairStructures.noise(this.seed, pos.getX(), y, pos.getZ());
                    BlockState state = this.blockAt(dx, y, dz, n);
                    if (state != null) {
                        world.setBlockState(pos, state, 2);
                    }
                }
            }
        }
    }

    /** What goes at (dx, y, dz) from the shaft's axis, or null to leave the world as it is. */
    private BlockState blockAt(int dx, int y, int dz, float n) {
        int y0 = this.bottom.getY();
        int ring = Math.max(Math.abs(dx), Math.abs(dz));
        if (y == y0) {
            return Blocks.DEEPSLATE_TILES.getDefaultState();
        }
        if (ring == 2) {
            // The doorway from the tunnel, on the west side
            if (dx == -2 && Math.abs(dz) <= 1 && y <= y0 + 3) {
                return Blocks.AIR.getDefaultState();
            }
            if (y <= this.top) {
                boolean surface = y > this.top - 3;
                return (surface ? (n < 0.5F ? Blocks.MOSSY_COBBLESTONE : Blocks.CRACKED_STONE_BRICKS)
                        : n < 0.15F ? Blocks.CRACKED_DEEPSLATE_BRICKS : Blocks.DEEPSLATE_BRICKS).getDefaultState();
            }
            // A broken parapet round the top, gaps to climb out through
            if (y == this.top + 1 && (dx + dz) % 2 == 0 && n < 0.7F) {
                return Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }
        if (ring == 0) {
            if (y <= this.top) {
                return Blocks.POLISHED_DEEPSLATE.getDefaultState();
            }
            return y == this.top + 1 ? Blocks.SOUL_LANTERN.getDefaultState() : Blocks.AIR.getDefaultState();
        }
        // The stair: step s sits in ring cell s % 8, one block above the last.
        int step = y - (y0 + 1);
        int cell = cellOf(dx, dz);
        if (y <= this.top && step % RING.length == cell) {
            int[] from = RING[(cell + RING.length - 1) % RING.length];
            int[] to = RING[cell];
            Direction facing = Direction.fromVector(to[0] - from[0], 0, to[1] - from[1]);
            return Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState().with(StairsBlock.FACING, facing == null ? Direction.NORTH : facing);
        }
        if (y > y0 + 3 && (y - y0 - 3) % 16 == 0 && cell == 3 && y < this.top - 2) {
            // Now and then a lantern hung under the stair above
            return Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true);
        }
        return Blocks.AIR.getDefaultState();
    }

    private static int cellOf(int dx, int dz) {
        for (int i = 0; i < RING.length; i++) {
            if (RING[i][0] == dx && RING[i][1] == dz) {
                return i;
            }
        }
        return -1;
    }
}
