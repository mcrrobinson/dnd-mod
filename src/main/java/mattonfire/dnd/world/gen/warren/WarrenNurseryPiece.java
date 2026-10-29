package mattonfire.dnd.world.gen.warren;

import mattonfire.dnd.entity.kobold.Kobolds;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The egg nursery (design 10, 4.3), east of the den: a low room 7 x 7 floored with straw and kept dark, holding
 * {@value #CLUTCHES_MIN}-{@value #CLUTCHES_MAX} Kobold Egg Clutches and a kobold minding them. Breaking a clutch is
 * the worst thing you can do in a warren ({@link KoboldWarrenLaw#eggClutchBroken}). Room a = 10..16, b = -3..3 in
 * the den's frame.
 */
public class WarrenNurseryPiece extends WarrenPiece {
    public static final int CLUTCHES_MIN = 3;
    public static final int CLUTCHES_MAX = 6;
    private static final int[][] SPOTS = {{12, -2}, {14, -2}, {15, 0}, {14, 2}, {12, 2}, {13, 0}, {16, -2}, {16, 2}};

    public WarrenNurseryPiece(BlockPos den, Direction front, long seed, int tier) {
        super(KoboldWarrens.NURSERY, den, front, seed, tier, 6, -2, -4, 17, 6, 4);
    }

    public WarrenNurseryPiece(NbtCompound nbt) {
        super(KoboldWarrens.NURSERY, nbt);
    }

    /** A clutch's spot (the first one placed). */
    public BlockPos firstClutch() {
        return this.at(SPOTS[0][0], 1, SPOTS[0][1]);
    }

    @Override
    protected void build(Builder b) {
        b.seal(6, -1, -4, 17, 5, 4);
        for (int a = 7; a <= 9; a++) {
            for (int bb = -1; bb <= 1; bb++) {
                b.floor(a, 0, bb, b.dirtFloor());
            }
        }
        b.carve(7, 1, -1, 9, 3, 1);
        for (int a = 10; a <= 16; a++) {
            for (int bb = -3; bb <= 3; bb++) {
                b.floor(a, 0, bb, Blocks.HAY_BLOCK.getDefaultState());
            }
        }
        b.carve(10, 1, -3, 16, 4, 3);
        int clutches = CLUTCHES_MIN + b.random.nextInt(CLUTCHES_MAX - CLUTCHES_MIN + 1);
        for (int i = 0; i < clutches; i++) {
            b.set(SPOTS[i][0], 1, SPOTS[i][1], KoboldWarrens.KOBOLD_EGG_CLUTCH);
        }
        b.kobold(11, 1, 0, Kobolds.KOBOLD, 5);
    }
}
