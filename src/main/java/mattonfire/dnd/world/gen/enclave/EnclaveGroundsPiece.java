package mattonfire.dnd.world.gen.enclave;

import mattonfire.dnd.classes.Blocks.elven.SylvanGlyphStoneBlock;
import mattonfire.dnd.classes.Registry.ElvenBlocks;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Covers the whole enclave and makes the grounds safe before anything else is built: surface lava lakes
 * (which generate before structures, so the planner can't avoid them) become pools of water. It also stands a
 * Sylvan Glyph Stone on the ground in the middle of each side of the grounds, {@link #GLYPH_INSET} in from the
 * edge, marking where Sylvan Law begins: 2-4 of them, as a spot with no firm, open ground near it gets none.
 * Its box sits deep underground, so it doesn't count as "in a building" for spawning.
 */
public class EnclaveGroundsPiece extends StructurePiece {
    /** How far in from the grounds' edge the glyph stones stand. */
    static final int GLYPH_INSET = 2;
    /** How far along the side a glyph stone may move to find firm ground. */
    private static final int GLYPH_SEARCH = 4;

    public EnclaveGroundsPiece(int minX, int minZ, int maxX, int maxZ, int y) {
        super(ElvenEnclaveStructures.GROUNDS, 0, new BlockBox(minX, y - 40, minZ, maxX, y - 38, maxZ));
    }

    public EnclaveGroundsPiece(NbtCompound nbt) {
        super(ElvenEnclaveStructures.GROUNDS, nbt);
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
                    world.setBlockState(pos, Blocks.WATER.getDefaultState(), PieceBlocks.UPDATE);
                    pos.move(0, -1, 0);
                }
            }
        }
        this.glyphStones(world, chunkBox);
    }

    /** Where the glyph stones go, before looking for ground: the middle of each side, inset. */
    int[][] glyphSpots() {
        BlockBox box = this.boundingBox;
        int midX = (box.getMinX() + box.getMaxX()) / 2;
        int midZ = (box.getMinZ() + box.getMaxZ()) / 2;
        return new int[][]{
                {midX, box.getMinZ() + GLYPH_INSET, 1, 0}, {midX, box.getMaxZ() - GLYPH_INSET, 1, 0},
                {box.getMinX() + GLYPH_INSET, midZ, 0, 1}, {box.getMaxX() - GLYPH_INSET, midZ, 0, 1}};
    }

    /**
     * Each glyph stone stands on the first firm, open spot along its side, trying the middle first and then up to
     * {@link #GLYPH_SEARCH} either way. The search runs only in the chunk the middle is in, so every chunk agrees.
     */
    private void glyphStones(StructureWorldAccess world, BlockBox chunkBox) {
        BlockState lower = ElvenBlocks.SYLVAN_GLYPH_STONE.getDefaultState();
        BlockState upper = lower.with(SylvanGlyphStoneBlock.HALF, DoubleBlockHalf.UPPER);
        for (int[] spot : this.glyphSpots()) {
            if (!chunkBox.contains(new BlockPos(spot[0], chunkBox.getMinY() + 1, spot[1]))) {
                continue;
            }
            for (int k = 0; k <= 2 * GLYPH_SEARCH; k++) {
                int off = (k % 2 == 0 ? 1 : -1) * ((k + 1) / 2);
                int x = spot[0] + spot[2] * off;
                int z = spot[1] + spot[3] * off;
                BlockPos foot = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                if (!chunkBox.contains(foot) || !isFirm(world, foot.down())) {
                    continue;
                }
                if (!world.getBlockState(foot).isReplaceable() || !world.getBlockState(foot.up()).isReplaceable()) {
                    continue;
                }
                world.setBlockState(foot, lower, PieceBlocks.UPDATE);
                world.setBlockState(foot.up(), upper, PieceBlocks.UPDATE);
                break;
            }
        }
    }

    /** Solid ground that isn't a tree or water: dirt, grass, moss, stone... */
    private static boolean isFirm(StructureWorldAccess world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getFluidState().isEmpty() && !state.isIn(BlockTags.LOGS) && !state.isIn(BlockTags.LEAVES)
                && state.isSideSolidFullSquare(world, pos, Direction.UP);
    }
}
