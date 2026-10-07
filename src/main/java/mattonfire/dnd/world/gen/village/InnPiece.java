package mattonfire.dnd.world.gen.village;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The Green Dragon: a thatched inn with a round green door, a roaring fire, a bar backed by ale
 * casks and tables laid for supper.
 */
public class InnPiece extends HobbitPiece {
    private static final int WIDTH = 15;
    private static final int DEPTH = 11;
    private static final int HEIGHT = 13;

    public InnPiece(BlockBox box, Direction facing, long seed) {
        super(HobbitVillageStructures.INN, box, facing, seed);
    }

    public InnPiece(NbtCompound nbt) {
        super(HobbitVillageStructures.INN, nbt);
    }

    public static InnPiece create(Random random, int x, int y, int z, Direction facing) {
        return new InnPiece(centeredBox(x, y, z, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    private static Block fieldstone(Builder b) {
        return b.chance(0.4F) ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE;
    }

    @Override
    protected void build(Builder b) {
        b.lawn(0, 0, WIDTH - 1, DEPTH - 1, HEIGHT - 1);
        b.fill(1, 0, 1, 13, 0, 9, Blocks.SPRUCE_PLANKS);

        // Walls: a fieldstone footing, plank walls and timber corner posts.
        for (int x = 1; x <= 13; x++) {
            for (int z = 1; z <= 9; z++) {
                if (x == 1 || x == 13 || z == 1 || z == 9) {
                    b.set(x, 1, z, fieldstone(b));
                    b.fill(x, 2, z, x, 4, z, Blocks.OAK_PLANKS);
                }
            }
        }
        for (int[] p : new int[][]{{1, 1}, {13, 1}, {1, 9}, {13, 9}}) {
            b.fill(p[0], 1, p[1], p[0], 4, p[1], log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        }
        b.air(2, 1, 2, 12, 4, 8);

        this.roof(b);

        // Fireplace in the west wall, chimney up the outside.
        b.fill(1, 1, 4, 1, 4, 6, Blocks.BRICKS);
        b.set(1, 1, 5, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.FACING, Direction.EAST));
        b.set(1, 2, 5, Blocks.AIR);
        b.fill(0, 1, 4, 0, 2, 6, Blocks.BRICKS);
        b.fill(0, 3, 5, 0, 11, 5, Blocks.BRICKS);
        b.set(0, 12, 5, Blocks.CAMPFIRE);
        b.fill(2, 3, 4, 2, 3, 6, topSlab(Blocks.SPRUCE_SLAB));
        b.candles(2, 4, 4);
        b.pot(2, 4, 5);
        b.candles(2, 4, 6);

        // Round door and windows.
        b.roundDoor(7, 1, 1, Blocks.WARPED_DOOR, Blocks.WARPED_PLANKS);
        b.roundWindow(3, 3, 1, true);
        b.roundWindow(11, 3, 1, true);
        b.roundWindow(4, 3, 9, true);
        b.roundWindow(10, 3, 9, true);
        b.roundWindow(13, 3, 5, false);

        this.furnish(b);
        this.frontage(b);
    }

    /** A thatched gable roof: hay bales stepping up from front and back to a ridge along x. */
    private void roof(Builder b) {
        for (int k = 0; k <= 5; k++) {
            int y = 5 + k;
            int front = k;
            int rear = 10 - k;
            for (int x = 0; x < WIDTH; x++) {
                b.set(x, y, front, log(Blocks.HAY_BLOCK, Direction.Axis.X));
                b.set(x, y, rear, log(Blocks.HAY_BLOCK, Direction.Axis.X));
            }
            for (int z = front + 1; z < rear; z++) {
                if (k == 0) {
                    b.fill(1, y, z, 13, y, z, Blocks.OAK_PLANKS);
                    b.set(0, y, z, log(Blocks.HAY_BLOCK, Direction.Axis.X));
                    b.set(WIDTH - 1, y, z, log(Blocks.HAY_BLOCK, Direction.Axis.X));
                } else {
                    b.set(1, y, z, Blocks.OAK_PLANKS);
                    b.set(13, y, z, Blocks.OAK_PLANKS);
                }
            }
        }
    }

    private void furnish(Builder b) {
        b.fill(5, 1, 2, 9, 1, 4, Blocks.GREEN_CARPET);

        // Bar counter with drinks and nibbles, ale casks stacked behind.
        for (int x = 3; x <= 9; x++) {
            b.set(x, 1, 6, topSlab(Blocks.SPRUCE_SLAB));
            if (b.chance(0.45F)) {
                b.frame(x, 2, 6, Direction.UP, new ItemStack(Items.HONEY_BOTTLE));
            } else {
                b.tableFood(x, 2, 6);
            }
            b.barrel(x, 1, 8, OUT, ALE);
            b.barrel(x, 2, 8, OUT, x % 2 == 0 ? ALE : PANTRY);
        }
        b.set(10, 1, 8, Blocks.HAY_BLOCK);
        b.set(11, 1, 8, Blocks.CAKE);

        // Tables by the front windows and along the east wall.
        for (int x : new int[]{3, 4, 10, 11}) {
            b.set(x, 1, 3, topSlab(Blocks.SPRUCE_SLAB));
            b.tableFood(x, 2, 3);
            b.set(x, 1, 4, stairs(Blocks.OAK_STAIRS, IN, false));
            b.set(x, 1, 2, stairs(Blocks.OAK_STAIRS, OUT, false));
        }
        for (int z = 5; z <= 6; z++) {
            b.set(11, 1, z, topSlab(Blocks.SPRUCE_SLAB));
            b.tableFood(11, 2, z);
            b.set(12, 1, z, stairs(Blocks.OAK_STAIRS, Direction.EAST, false));
        }
        b.set(10, 1, 5, stairs(Blocks.OAK_STAIRS, Direction.WEST, false));

        b.set(4, 4, 4, lantern(true));
        b.set(10, 4, 4, lantern(true));
        b.set(7, 4, 6, lantern(true));
        b.set(3, 4, 7, lantern(true));
        b.set(11, 4, 7, lantern(true));

        b.hobbit(6, 1, 7);
        b.hobbit(3, 1, 5);
        b.hobbit(9, 1, 4);
    }

    private void frontage(Builder b) {
        for (int x = 1; x <= 13; x++) {
            if (Math.abs(x - 7) > 2 && b.chance(0.75F)) {
                b.flower(x, 1, 0);
            }
        }
        b.ground(7, 0, Blocks.DIRT_PATH);

        b.set(4, 1, 0, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, 0));
        b.set(4, 2, 0, Blocks.AIR);
        BlockPos sign = b.pos(4, 1, 0);
        if (b.chunkBox.contains(sign) && b.world.getBlockEntity(sign) instanceof SignBlockEntity entity) {
            entity.setTextOnRow(0, Text.literal("~ The ~"));
            entity.setTextOnRow(1, Text.literal("Green Dragon"));
            entity.setTextOnRow(2, Text.literal("Inn"));
            entity.setTextOnRow(3, Text.literal("Ales & Suppers"));
        }
    }
}
