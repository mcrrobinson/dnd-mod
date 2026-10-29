package mattonfire.dnd.world.gen.warren;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.CrumblingFloorBlock;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.classes.Registry.DwarvenBlocks;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import mattonfire.dnd.entity.kobold.Kobolds;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The chief's hoard room (design 10, 4.3), straight back from the den: a hall 11 x 9, 5 high, behind a straw-lidded
 * {@link TrapKind#PIT} in its corridor (3 x 2 over a 4-deep pit of pointed dripstone, a ladder up its far wall). The
 * Kobold Dragonshield and two kobolds, the hoard chest ({@code chests/kobold_hoard}, guarded by the warren law), coin
 * piles, a gold block and two red-dragon banners. Room a = -5..5, b = -20..-12 in the den's frame; the corridor
 * a = -1..1, b = -11..-5, the pit at b = -8 and -9.
 */
public class WarrenChiefPiece extends WarrenPiece {
    public static final int PIT_NEAR = -8;
    public static final int CHEST_B = -20;

    public WarrenChiefPiece(BlockPos den, Direction front, long seed, int tier) {
        super(KoboldWarrens.CHIEF, den, front, seed, tier, -6, -6, -21, 6, 7, -4);
    }

    public WarrenChiefPiece(NbtCompound nbt) {
        super(KoboldWarrens.CHIEF, nbt);
    }

    public BlockPos chest() {
        return this.at(0, 1, CHEST_B);
    }

    /** The pit's buried trigger (under the tile in front of it). */
    public BlockPos pitTrigger() {
        return this.at(0, -1, PIT_NEAR + 1);
    }

    @Override
    protected void build(Builder b) {
        b.seal(-6, -6, -21, 6, 6, -4);
        for (int a = -1; a <= 1; a++) {
            for (int bb = -11; bb <= -5; bb++) {
                b.floor(a, 0, bb, b.dirtFloor());
            }
        }
        b.carve(-1, 1, -11, 1, 3, -5);
        for (int a = -5; a <= 5; a++) {
            for (int bb = -20; bb <= -12; bb++) {
                b.floor(a, 0, bb, b.dirtFloor());
            }
        }
        b.carve(-5, 1, -20, 5, 5, -12);

        // The hoard
        b.chest(0, 1, CHEST_B, Direction.SOUTH, HOARD);
        b.set(-1, 1, CHEST_B, Blocks.GOLD_BLOCK);
        for (int a = -4; a <= 4; a++) {
            for (int bb = -20; bb <= -18; bb++) {
                int layers = 1 + b.random.nextInt(4);
                boolean pile = b.chance(0.6F);
                if (pile && !(a == 0 && bb == CHEST_B) && !(a == -1 && bb == CHEST_B)) {
                    b.set(a, 1, bb, DwarvenBlocks.TREASURY_COIN_PILE.getDefaultState().with(SnowBlock.LAYERS, layers));
                }
            }
        }
        b.banner(-3, 4, -20, Direction.SOUTH, DyeColor.RED, ModBannerPatterns.GREEN_DRAGON.in(DyeColor.BLACK));
        b.banner(3, 4, -20, Direction.SOUTH, DyeColor.RED, ModBannerPatterns.GREEN_DRAGON.in(DyeColor.BLACK));
        b.set(-5, 1, -12, Blocks.BONE_BLOCK);
        b.set(5, 1, -12, Blocks.SKELETON_SKULL.getDefaultState().with(SkullBlock.ROTATION, 8));
        b.wallTorch(-5, 3, -16, Direction.EAST);
        b.wallTorch(5, 3, -16, Direction.WEST);

        // The covered pit in the corridor
        List<BlockPos> crumbles = new ArrayList<>();
        BlockPos ladder = this.at(0, 0, PIT_NEAR - 1);
        if (b.chunkBox.intersects(this.getBoundingBox())) {
            KoboldTrapRigs.pit(b.world, b.chunkBox, this.at(0, 0, PIT_NEAR), b.dir(IN), b.dir(Direction.EAST), crumbles,
                    ladder, b.dir(OUT));
        }
        b.trap(this.pitTrigger(), TrapKind.PIT, List.of(), crumbles, List.of(), ladder, b.dir(OUT),
                CrumblingFloorBlock.Covering.STRAW, true);

        b.kobold(0, 1, -16, Kobolds.KOBOLD_DRAGONSHIELD, 6);
        b.kobold(-3, 1, -14, Kobolds.KOBOLD, 8);
        b.kobold(3, 1, -14, Kobolds.KOBOLD_SLINGER, 8);
    }
}
