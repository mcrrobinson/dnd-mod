package mattonfire.dnd.world.gen.village;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
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
 * Covers the whole village and makes the grounds safe before anything else is built: surface
 * lava lakes (which generate before structures, so the planner can't avoid them) become ponds.
 */
public class VillageGroundsPiece extends StructurePiece {
    public VillageGroundsPiece(int minX, int minZ, int maxX, int maxZ, int y) {
        // Sits well under the ground, out of reach of terrain adaptation (see PathPiece).
        super(HobbitVillageStructures.GROUNDS, 0, new BlockBox(minX, y - 40, minZ, maxX, y - 38, maxZ));
    }

    public VillageGroundsPiece(NbtCompound nbt) {
        super(HobbitVillageStructures.GROUNDS, nbt);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        int minX = Math.max(this.boundingBox.getMinX(), chunkBox.getMinX());
        int maxX = Math.min(this.boundingBox.getMaxX(), chunkBox.getMaxX());
        int minZ = Math.max(this.boundingBox.getMinZ(), chunkBox.getMinZ());
        int maxZ = Math.min(this.boundingBox.getMaxZ(), chunkBox.getMaxZ());
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                pos.set(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                while (pos.getY() > world.getBottomY() && world.getFluidState(pos).isIn(FluidTags.LAVA)) {
                    world.setBlockState(pos, Blocks.WATER.getDefaultState(), 2);
                    pos.move(0, -1, 0);
                }
            }
        }
    }
}
