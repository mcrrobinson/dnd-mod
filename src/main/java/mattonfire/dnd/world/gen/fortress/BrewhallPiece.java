package mattonfire.dnd.world.gen.fortress;

import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * A mead hall: walls lined with stacked ale casks, a long table laid with roasts and mugs, and a
 * roaring hearth at the back.
 */
public class BrewhallPiece extends FortressPiece {
    private static final int WIDTH = 13;
    private static final int DEPTH = 13;
    private static final int HEIGHT = 9;
    private static final int MID = WIDTH / 2;

    public BrewhallPiece(BlockBox box, Direction facing, long seed) {
        super(DwarvenFortressStructures.BREWHALL, box, facing, seed);
    }

    public BrewhallPiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.BREWHALL, nbt);
    }

    public static BrewhallPiece create(Random random, BlockPos front, Direction facing) {
        return new BrewhallPiece(boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        int back = DEPTH - 1;
        b.shell(WIDTH, HEIGHT, DEPTH, Blocks.DARK_OAK_PLANKS.getDefaultState());
        b.doorway(MID, 0, 3, 4);

        // Casks two high down both walls, a third of them still full.
        for (int z = 2; z <= back - 2; z++) {
            for (int y = 1; y <= 2; y++) {
                if (b.chance(0.35F)) {
                    b.barrel(1, y, z, Direction.EAST, BREWHALL);
                } else {
                    b.set(1, y, z, facing(Blocks.BARREL, Direction.EAST));
                }
                if (b.chance(0.35F)) {
                    b.barrel(WIDTH - 2, y, z, Direction.WEST, BREWHALL);
                } else {
                    b.set(WIDTH - 2, y, z, facing(Blocks.BARREL, Direction.WEST));
                }
            }
            b.set(1, 3, z, bottomSlab(Blocks.DARK_OAK_SLAB));
            b.set(WIDTH - 2, 3, z, bottomSlab(Blocks.DARK_OAK_SLAB));
        }

        // The long table with benches.
        ItemStack[] feast = {new ItemStack(Items.COOKED_BEEF), new ItemStack(Items.COOKED_PORKCHOP), new ItemStack(Items.BREAD),
                new ItemStack(Items.HONEY_BOTTLE), new ItemStack(Items.MUSHROOM_STEW), new ItemStack(Items.COOKED_MUTTON),
                new ItemStack(Items.BAKED_POTATO)};
        for (int z = 3; z <= back - 3; z++) {
            for (int x = MID - 1; x <= MID + 1; x++) {
                b.set(x, 1, z, topSlab(Blocks.SPRUCE_SLAB));
                if (x != MID && b.chance(0.6F)) {
                    b.frame(x, 2, z, Direction.UP, b.pick(feast).copy());
                } else if (x == MID && z % 3 == 0) {
                    b.set(x, 2, z, b.candleState());
                }
            }
            b.set(MID - 2, 1, z, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
            b.set(MID + 2, 1, z, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
        }

        // The hearth in the back wall.
        b.fill(MID - 2, 1, back - 1, MID + 2, 4, back - 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.set(MID, 1, back - 1, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.FACING, OUT));
        b.air(MID, 2, back - 1, MID, 2, back - 1);
        b.fill(MID, 3, back - 1, MID, HEIGHT - 1, back - 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.set(MID - 1, 1, back - 1, facing(Blocks.SMOKER, OUT));
        b.set(MID + 1, 1, back - 1, facing(Blocks.SMOKER, OUT));
        b.fill(MID - 2, 5, back - 2, MID + 2, 5, back - 2, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, OUT, true));
        b.set(MID - 2, 6, back - 2, lantern(false));
        b.set(MID + 2, 6, back - 2, lantern(false));

        b.chandelier(MID, HEIGHT - 1, 6, 3);
        b.dwarf(MID - 2, 1, 5);
        b.dwarf(MID + 2, 1, 7);
        b.dwarf(MID, 1, back - 2);
    }
}
