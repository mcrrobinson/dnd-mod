package mattonfire.dnd.world.gen.village;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/** A fenced orchard of blossoming apple trees, with beehives, wildflowers and barrels of apples. */
public class OrchardPiece extends HobbitPiece {
    private static final int SIZE = 13;
    private static final int HEIGHT = 9;

    public OrchardPiece(BlockBox box, Direction facing, long seed) {
        super(HobbitVillageStructures.ORCHARD, box, facing, seed);
    }

    public OrchardPiece(NbtCompound nbt) {
        super(HobbitVillageStructures.ORCHARD, nbt);
    }

    public static OrchardPiece create(Random random, int x, int y, int z, Direction facing) {
        return new OrchardPiece(centeredBox(x, y, z, facing, SIZE, HEIGHT, SIZE), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        int mid = SIZE / 2;
        b.lawn(0, 0, SIZE - 1, SIZE - 1, HEIGHT - 1);
        for (int x = 1; x < SIZE - 1; x++) {
            for (int z = 1; z < SIZE - 1; z++) {
                if (b.chance(0.25F)) {
                    b.flower(x, 1, z);
                } else if (b.chance(0.2F)) {
                    b.set(x, 1, z, Blocks.GRASS);
                }
            }
        }
        for (int z = 0; z < mid; z++) {
            b.ground(mid, z, Blocks.DIRT_PATH);
            b.set(mid, 1, z, Blocks.AIR);
            b.set(mid, 2, z, Blocks.AIR);
        }

        int[][] trees = {{3, 3}, {9, 3}, {3, 9}, {9, 9}};
        for (int[] t : trees) {
            this.appleTree(b, t[0], t[1]);
        }
        b.beehive(3, 2, 2, OUT, 3);
        b.beehive(9, 3, 10, IN, 2);

        // Apple barrels and a hay stack in the middle.
        b.set(mid, 1, mid, Blocks.HAY_BLOCK);
        b.set(mid, 2, mid, Blocks.AIR);
        b.barrel(mid - 1, 1, mid, Direction.UP, HARVEST);
        b.frame(mid - 1, 2, mid, Direction.UP, new ItemStack(Items.APPLE));
        b.barrel(mid + 1, 1, mid, Direction.UP, HARVEST);
        b.frame(mid + 1, 2, mid, Direction.UP, new ItemStack(Items.APPLE));
        b.set(mid, 1, mid + 1, Blocks.COMPOSTER);

        for (int x = 0; x < SIZE; x++) {
            b.set(x, 1, 0, Blocks.OAK_FENCE);
            b.set(x, 1, SIZE - 1, Blocks.OAK_FENCE);
        }
        for (int z = 0; z < SIZE; z++) {
            b.set(0, 1, z, Blocks.OAK_FENCE);
            b.set(SIZE - 1, 1, z, Blocks.OAK_FENCE);
        }
        b.gate(mid, 1, 0, Blocks.OAK_FENCE_GATE, OUT);
        b.set(mid - 1, 2, 0, lantern(false));
        b.set(mid + 1, 2, 0, lantern(false));

        if (b.chance(0.5F)) {
            b.hobbit(mid, 1, mid - 2);
        }
    }

    private void appleTree(Builder b, int x, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int y = 3; y <= 5; y++) {
                    int r = y == 5 ? 1 : 2;
                    boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                    if (Math.abs(dx) > r || Math.abs(dz) > r || (corner && y > 3) || (corner && b.chance(0.5F))) {
                        continue;
                    }
                    Block leaves = b.chance(0.3F) ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.OAK_LEAVES;
                    b.set(x + dx, y, z + dz, leaves(leaves));
                }
            }
        }
        b.fill(x, 1, z, x, 4, z, log(Blocks.OAK_LOG, Direction.Axis.Y));
    }
}
