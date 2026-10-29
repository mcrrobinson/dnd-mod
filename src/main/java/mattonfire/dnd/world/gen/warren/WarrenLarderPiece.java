package mattonfire.dnd.world.gen.warren;

import mattonfire.dnd.entity.kobold.Kobolds;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The larder (design 10, 4.3), back east of the den: a room 5 x 5 of barrels ({@code chests/kobold_larder}), a
 * composter and mushrooms in the damp. A bolt-hole runs from it down to the chief's room. Room a = 6..10,
 * b = -10..-6 in the den's frame.
 */
public class WarrenLarderPiece extends WarrenPiece {
    public WarrenLarderPiece(BlockPos den, Direction front, long seed, int tier) {
        super(KoboldWarrens.LARDER, den, front, seed, tier, 2, -2, -11, 11, 5, -2);
    }

    public WarrenLarderPiece(NbtCompound nbt) {
        super(KoboldWarrens.LARDER, nbt);
    }

    @Override
    protected void build(Builder b) {
        b.seal(2, -1, -11, 11, 4, -2);
        for (int a = 3; a <= 5; a++) {
            for (int bb = -8; bb <= -3; bb++) {
                b.floor(a, 0, bb, b.dirtFloor());
            }
        }
        b.carve(3, 1, -8, 5, 3, -3);
        for (int a = 6; a <= 10; a++) {
            for (int bb = -10; bb <= -6; bb++) {
                b.floor(a, 0, bb, b.dirtFloor());
            }
        }
        b.carve(6, 1, -10, 10, 3, -6);
        b.barrel(10, 1, -10, Direction.UP, LARDER);
        b.barrel(10, 1, -9, Direction.UP, LARDER);
        b.barrel(9, 1, -10, Direction.UP, LARDER);
        b.set(10, 2, -10, Blocks.BARREL);
        b.set(6, 1, -10, Blocks.COMPOSTER);
        b.set(10, 1, -6, Blocks.BROWN_MUSHROOM);
        b.set(7, 1, -6, Blocks.BROWN_MUSHROOM);
        if (b.chance(0.5F)) {
            b.kobold(8, 1, -8, Kobolds.KOBOLD, 6);
        }
    }
}
