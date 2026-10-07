package mattonfire.dnd.world.gen.fortress;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/** A short vaulted passage from a side doorway of the great hall to one of the rooms. */
public class CorridorPiece extends FortressPiece {
    private static final int WIDTH = 5;
    private static final int DEPTH = 5;
    private static final int HEIGHT = 6;

    public CorridorPiece(BlockBox box, Direction facing, long seed) {
        super(DwarvenFortressStructures.CORRIDOR, box, facing, seed);
    }

    public CorridorPiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.CORRIDOR, nbt);
    }

    public static CorridorPiece create(Random random, BlockPos front, Direction facing) {
        return new CorridorPiece(boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        b.shell(WIDTH, HEIGHT, DEPTH, Blocks.POLISHED_DEEPSLATE.getDefaultState());
        b.air(1, 1, 0, 3, 4, 0);
        b.air(1, 1, DEPTH - 1, 3, 4, DEPTH - 1);
        b.fill(2, 0, 0, 2, 0, DEPTH - 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
        for (int z = 0; z < DEPTH; z++) {
            b.set(1, 4, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, true));
            b.set(3, 4, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, true));
        }
        b.set(2, 4, DEPTH / 2, lantern(true));
    }
}
