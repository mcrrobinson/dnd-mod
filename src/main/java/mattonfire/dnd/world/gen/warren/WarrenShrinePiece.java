package mattonfire.dnd.world.gen.warren;

import java.util.List;
import mattonfire.dnd.classes.Blocks.CrumblingFloorBlock;
import mattonfire.dnd.classes.Blocks.DartLauncherBlock;
import mattonfire.dnd.classes.Blocks.TrapBlocks;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.classes.Registry.DwarvenBlocks;
import mattonfire.dnd.entity.kobold.Kobolds;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The red-dragon shrine (design 10, 4.3), west of the den: a hall 9 x 7, 6 high. A dragon idol of red terracotta
 * with nether-brick horns and coin piles at its feet, the Dragon Tribute Bowl in front of it and two red candles. A
 * {@link TrapKind#DART} trap guards its corridor (a stone tile in the floor, a launcher in the south wall). Laid out in
 * the den's frame: the room runs a = -19..-11, b = -3..3.
 */
public class WarrenShrinePiece extends WarrenPiece {
    /** The dart trap's plate (visible: a worn stone tile). */
    public static final int PLATE_A = -9;

    public WarrenShrinePiece(BlockPos den, Direction front, long seed, int tier) {
        super(KoboldWarrens.SHRINE, den, front, seed, tier, -20, -2, -4, -6, 8, 4);
    }

    public WarrenShrinePiece(NbtCompound nbt) {
        super(KoboldWarrens.SHRINE, nbt);
    }

    public BlockPos bowl() {
        return this.at(-15, 1, 0);
    }

    public BlockPos idol() {
        return this.at(-19, 3, 0);
    }

    public BlockPos plate() {
        return this.at(PLATE_A, 0, 0);
    }

    @Override
    protected void build(Builder b) {
        b.seal(-20, -1, -4, -6, 7, 4);
        // The corridor from the den, floored in old stone
        for (int a = -10; a <= -7; a++) {
            for (int bb = -1; bb <= 1; bb++) {
                b.floor(a, 0, bb, (b.chance(0.5F) ? Blocks.COBBLESTONE : Blocks.STONE).getDefaultState());
            }
        }
        b.carve(-10, 1, -1, -7, 3, 1);
        // The hall
        for (int a = -19; a <= -11; a++) {
            for (int bb = -3; bb <= 3; bb++) {
                b.floor(a, 0, bb, b.chance(0.3F) ? Blocks.COBBLESTONE.getDefaultState() : b.dirtFloor());
            }
        }
        b.carve(-19, 1, -3, -11, 6, 3);
        // A red carpet up to the idol
        for (int a = -16; a <= -11; a++) {
            b.set(a, 1, 0, Blocks.RED_CARPET);
        }

        // The idol: a squat red dragon against the west wall
        b.fill(-19, 1, -1, -19, 3, 1, Blocks.RED_TERRACOTTA);
        b.fill(-18, 1, -1, -18, 1, 1, Blocks.RED_NETHER_BRICKS);
        b.set(-19, 3, -2, Blocks.RED_TERRACOTTA);
        b.set(-19, 3, 2, Blocks.RED_TERRACOTTA);
        b.set(-19, 4, -2, Blocks.RED_TERRACOTTA);
        b.set(-19, 4, 2, Blocks.RED_TERRACOTTA);
        b.set(-19, 4, 0, Blocks.RED_TERRACOTTA);
        b.set(-18, 4, 0, Blocks.MAGMA_BLOCK);
        b.set(-19, 5, 0, Blocks.RED_TERRACOTTA);
        b.set(-19, 5, -1, Blocks.NETHER_BRICK_FENCE);
        b.set(-19, 5, 1, Blocks.NETHER_BRICK_FENCE);
        b.set(-19, 6, -1, Blocks.NETHER_BRICK_FENCE);
        b.set(-19, 6, 1, Blocks.NETHER_BRICK_FENCE);
        // Coins heaped at its feet
        for (int bb = -2; bb <= 2; bb++) {
            int layers = 1 + b.random.nextInt(3);
            b.set(-17, 1, bb, DwarvenBlocks.TREASURY_COIN_PILE.getDefaultState().with(SnowBlock.LAYERS, layers));
        }
        // The tribute bowl and two candles
        b.set(-15, 1, 0, Kobolds.DRAGON_TRIBUTE_BOWL.getDefaultState());
        b.set(-15, 1, -2, Blocks.RED_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
        b.set(-15, 1, 2, Blocks.RED_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));

        // The dart trap in the corridor: a plate in the floor, a launcher in the south wall facing across
        b.set(PLATE_A, 1, 2, TrapBlocks.DART_LAUNCHER.getDefaultState().with(DartLauncherBlock.FACING, Direction.NORTH));
        b.trap(this.plate(), TrapKind.DART, List.of(this.at(PLATE_A, 1, 2)), List.of(), List.of(), null, Direction.NORTH,
                CrumblingFloorBlock.Covering.STONE, false);

        b.kobold(-13, 1, 2, Kobolds.KOBOLD, 6);
    }
}
