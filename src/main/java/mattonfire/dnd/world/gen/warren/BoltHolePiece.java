package mattonfire.dnd.world.gen.warren;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtLongArray;
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
import org.jetbrains.annotations.Nullable;

/**
 * A kobold bolt-hole (design 10, 4.3): a 1 x 1 tunnel (one block high where it runs level) that kobolds, 0.95 tall,
 * scurry through and players can't. Each warren has three: den to shrine, larder to the chief's room, and one from
 * the tunnel up to the hillside, coming out behind a bush. Badly hurt kobolds run for the nearest one
 * ({@link KoboldWarren#refuge}). The cells are world positions worked out by {@link KoboldWarrenPlanner}.
 */
public class BoltHolePiece extends StructurePiece {
    private final List<BlockPos> cells;
    @Nullable
    private final BlockPos exit;

    public BoltHolePiece(Direction front, long seed, List<BlockPos> cells, @Nullable BlockPos exit) {
        super(KoboldWarrens.BOLT_HOLE, 0, box(cells, exit));
        this.cells = List.copyOf(cells);
        this.exit = exit;
        this.setOrientation(front);
    }

    public BoltHolePiece(NbtCompound nbt) {
        super(KoboldWarrens.BOLT_HOLE, nbt);
        List<BlockPos> list = new ArrayList<>();
        for (long l : nbt.getLongArray("Cells")) {
            list.add(BlockPos.fromLong(l));
        }
        this.cells = list;
        this.exit = nbt.contains("Exit") ? BlockPos.fromLong(nbt.getLong("Exit")) : null;
    }

    private static BlockBox box(List<BlockPos> cells, @Nullable BlockPos exit) {
        List<BlockPos> all = new ArrayList<>(cells);
        if (exit != null) {
            all.add(exit);
        }
        BlockBox box = BlockBox.encompassPositions(all).orElse(new BlockBox(BlockPos.ORIGIN));
        return box.expand(1);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        long[] longs = new long[this.cells.size()];
        for (int i = 0; i < longs.length; i++) {
            longs[i] = this.cells.get(i).asLong();
        }
        nbt.put("Cells", new NbtLongArray(longs));
        if (this.exit != null) {
            nbt.putLong("Exit", this.exit.asLong());
        }
    }

    public List<BlockPos> cells() {
        return this.cells;
    }

    @Nullable
    public BlockPos exit() {
        return this.exit;
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        for (BlockPos cell : this.cells) {
            if (!chunkBox.contains(cell)) {
                continue;
            }
            world.setBlockState(cell, Blocks.AIR.getDefaultState(), PieceBlocks.UPDATE);
            BlockPos below = cell.down();
            BlockState floor = world.getBlockState(below);
            if (chunkBox.contains(below) && !this.cells.contains(below)
                    && (floor.isAir() || !floor.getFluidState().isEmpty() || floor.isReplaceable())) {
                world.setBlockState(below, Blocks.STONE.getDefaultState(), PieceBlocks.UPDATE);
            }
        }
        if (this.exit != null && chunkBox.contains(this.exit)) {
            BlockState bush = Blocks.SWEET_BERRY_BUSH.getDefaultState();
            if (!bush.canPlaceAt(world, this.exit)) {
                bush = Blocks.DEAD_BUSH.getDefaultState();
            }
            if (world.getBlockState(this.exit).isReplaceable() && bush.canPlaceAt(world, this.exit)) {
                world.setBlockState(this.exit, bush, PieceBlocks.UPDATE);
            }
        }
    }
}
