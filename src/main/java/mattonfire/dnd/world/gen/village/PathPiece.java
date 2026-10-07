package mattonfire.dnd.world.gen.village;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * A stretch of winding dirt lane, laid on whatever the ground is at each column (bridged with
 * planks over water), with the odd lamp post beside it.
 */
public class PathPiece extends StructurePiece {
    private final int[] cells;
    private final int[] lamps;
    private final long seed;

    public PathPiece(int[] cells, int[] lamps, int y, long seed) {
        super(HobbitVillageStructures.PATH, 0, bounds(cells, lamps, y));
        this.cells = cells;
        this.lamps = lamps;
        this.seed = seed;
    }

    public PathPiece(NbtCompound nbt) {
        super(HobbitVillageStructures.PATH, nbt);
        this.cells = nbt.getIntArray("Cells");
        this.lamps = nbt.getIntArray("Lamps");
        this.seed = nbt.getLong("Seed");
    }

    private static BlockBox bounds(int[] cells, int[] lamps, int y) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int[] list : new int[][]{cells, lamps}) {
            for (int i = 0; i < list.length; i += 2) {
                minX = Math.min(minX, list[i]);
                maxX = Math.max(maxX, list[i]);
                minZ = Math.min(minZ, list[i + 1]);
                maxZ = Math.max(maxZ, list[i + 1]);
            }
        }
        // Lanes follow the lie of the land rather than levelling it: terrain adaptation only reaches
        // 12 blocks above or below a piece's box, so the box sits well under the ground. Only its
        // x/z extent matters, for which chunks the lane is laid in.
        return new BlockBox(minX, y - 40, minZ, maxX, y - 38, maxZ);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putIntArray("Cells", this.cells);
        nbt.putIntArray("Lamps", this.lamps);
        nbt.putLong("Seed", this.seed);
    }

    private static boolean inColumn(BlockBox box, int x, int z) {
        return x >= box.getMinX() && x <= box.getMaxX() && z >= box.getMinZ() && z <= box.getMaxZ();
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        Random rand = Random.create(this.seed);
        for (int i = 0; i < this.cells.length; i += 2) {
            float roll = rand.nextFloat();
            int x = this.cells[i];
            int z = this.cells[i + 1];
            if (!inColumn(chunkBox, x, z)) {
                continue;
            }
            BlockPos ground = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
            BlockState state = world.getBlockState(ground);
            if (state.getFluidState().isIn(FluidTags.WATER)) {
                world.setBlockState(ground, Blocks.SPRUCE_PLANKS.getDefaultState(), 2);
            } else if (state.isIn(BlockTags.DIRT) || state.isOf(Blocks.SAND) || state.isOf(Blocks.GRAVEL)) {
                world.setBlockState(ground, (roll < 0.85F ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT).getDefaultState(), 2);
                clearPlant(world, ground.up());
            }
        }
        for (int i = 0; i < this.lamps.length; i += 2) {
            int x = this.lamps[i];
            int z = this.lamps[i + 1];
            if (!inColumn(chunkBox, x, z)) {
                continue;
            }
            BlockPos ground = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
            if (world.getBlockState(ground).isIn(BlockTags.DIRT)) {
                clearPlant(world, ground.up());
                world.setBlockState(ground.up(), Blocks.OAK_FENCE.getDefaultState(), 2);
                world.setBlockState(ground.up(2), Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, false), 2);
            }
        }
    }

    private static void clearPlant(StructureWorldAccess world, BlockPos pos) {
        BlockState plant = world.getBlockState(pos);
        if (plant.isAir() || !plant.getMaterial().isReplaceable() && !plant.isIn(BlockTags.FLOWERS)) {
            return;
        }
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        if (plant.contains(TallPlantBlock.HALF) && plant.get(TallPlantBlock.HALF) == DoubleBlockHalf.LOWER) {
            world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), 2);
        }
    }
}
