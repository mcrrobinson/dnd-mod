package mattonfire.dnd.world.gen.fortress;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The barracks: rows of bunks with footlockers along both walls, suits of armour on stands down
 * the middle, and axes racked on the back wall.
 */
public class BarracksPiece extends FortressPiece {
    private static final int WIDTH = 13;
    private static final int DEPTH = 13;
    private static final int HEIGHT = 8;
    private static final int MID = WIDTH / 2;

    public BarracksPiece(BlockBox box, Direction facing, long seed) {
        super(DwarvenFortressStructures.BARRACKS, box, facing, seed);
    }

    public BarracksPiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.BARRACKS, nbt);
    }

    public static BarracksPiece create(Random random, BlockPos front, Direction facing) {
        return new BarracksPiece(boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        int back = DEPTH - 1;
        DyeColor blanket = b.pick(new DyeColor[]{DyeColor.RED, DyeColor.BLUE, DyeColor.GRAY, DyeColor.BROWN});
        b.shell(WIDTH, HEIGHT, DEPTH, Blocks.SPRUCE_PLANKS.getDefaultState());
        b.doorway(MID, 0, 3, 4);

        // Bunks against both walls, heads to the wall, a footlocker at each foot.
        for (int z = 2; z <= back - 2; z += 2) {
            b.bed(2, 1, z, Direction.WEST, blanket);
            b.bed(WIDTH - 3, 1, z, Direction.EAST, blanket);
            if (z % 4 == 2) {
                b.barrel(3, 1, z, Direction.UP, BARRACKS);
                b.barrel(WIDTH - 4, 1, z, Direction.UP, BARRACKS);
            }
            if (z % 4 == 0) {
                b.set(1, 2, z, Blocks.SPRUCE_FENCE);
                b.set(1, 3, z, lantern(false));
                b.set(WIDTH - 2, 2, z, Blocks.SPRUCE_FENCE);
                b.set(WIDTH - 2, 3, z, lantern(false));
            }
        }

        // Armour stands down the middle.
        for (int z = 3; z <= back - 3; z += 3) {
            ItemStack helmet = new ItemStack(b.chance(0.3F) ? Items.GOLDEN_HELMET : Items.IRON_HELMET);
            b.armorStand(MID, 1, z, OUT, helmet, new ItemStack(Items.IRON_CHESTPLATE),
                    new ItemStack(Items.CHAINMAIL_LEGGINGS), new ItemStack(Items.IRON_BOOTS));
            b.set(MID, 0, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
        }

        // The weapon rack and the quartermaster's chest.
        for (int x = 2; x <= WIDTH - 3; x += 2) {
            b.frame(x, 3, back - 1, OUT, new ItemStack(b.chance(0.25F) ? Items.GOLDEN_AXE : Items.IRON_AXE));
        }
        b.chest(MID, 1, back - 1, OUT, BARRACKS);
        b.fill(1, 5, back - 1, WIDTH - 2, 5, back - 1, Blocks.SPRUCE_PLANKS);
        b.set(MID, HEIGHT - 2, 3, lantern(true));
        b.set(MID, HEIGHT - 2, back - 3, lantern(true));

        for (int i = 0; i < 3; i++) {
            b.dwarf(MID + (i - 1) * 2, 1, 5 + i * 2);
        }
    }
}
