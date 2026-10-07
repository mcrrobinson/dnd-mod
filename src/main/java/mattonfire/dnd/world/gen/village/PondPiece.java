package mattonfire.dnd.world.gen.village;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/** A lily pond fringed with reeds, with a little jetty to fish from. */
public class PondPiece extends HobbitPiece {
    private static final int WIDTH = 11;
    private static final int DEPTH = 9;
    private static final int HEIGHT = 4;

    public PondPiece(BlockBox box, Direction facing, long seed) {
        super(HobbitVillageStructures.POND, box, facing, seed);
    }

    public PondPiece(NbtCompound nbt) {
        super(HobbitVillageStructures.POND, nbt);
    }

    public static PondPiece create(Random random, int x, int y, int z, Direction facing) {
        return new PondPiece(centeredBox(x, y, z, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    private static double shape(int x, int z) {
        double fx = (x - 5) / 4.6D;
        double fz = (z - 4.5D) / 3.6D;
        return fx * fx + fz * fz;
    }

    @Override
    protected void build(Builder b) {
        b.lawn(0, 0, WIDTH - 1, DEPTH - 1, HEIGHT - 1);
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                double e = shape(x, z);
                if (e <= 1.0D) {
                    b.set(x, 0, z, Blocks.WATER);
                    b.set(x, -1, z, e < 0.55D ? Blocks.WATER.getDefaultState() : Blocks.CLAY.getDefaultState());
                    b.set(x, -2, z, b.chance(0.5F) ? Blocks.CLAY : Blocks.SAND);
                    if (e < 0.8D && b.chance(0.14F)) {
                        b.set(x, 1, z, Blocks.LILY_PAD);
                    }
                } else if (e <= 1.45D) {
                    boolean nextToWater = shape(x + 1, z) <= 1.0D || shape(x - 1, z) <= 1.0D
                            || shape(x, z + 1) <= 1.0D || shape(x, z - 1) <= 1.0D;
                    if (nextToWater && b.chance(0.35F)) {
                        b.set(x, 1, z, Blocks.SUGAR_CANE);
                        if (b.chance(0.6F)) {
                            b.set(x, 2, z, Blocks.SUGAR_CANE);
                        }
                    } else if (b.chance(0.25F)) {
                        b.flower(x, 1, z);
                    }
                } else if (b.chance(0.12F)) {
                    b.flower(x, 1, z);
                }
            }
        }

        // Jetty from the front edge out over the water.
        int mid = WIDTH / 2;
        b.ground(mid, 0, Blocks.DIRT_PATH);
        b.set(mid, 1, 0, Blocks.AIR);
        b.set(mid, 2, 0, Blocks.AIR);
        for (int z = 1; z <= 3; z++) {
            b.set(mid, 1, z, bottomSlab(Blocks.SPRUCE_SLAB));
            b.set(mid, 2, z, Blocks.AIR);
        }
        b.barrel(mid + 1, 1, 0, Direction.UP, HARVEST);
        b.set(mid - 1, 1, 0, Blocks.OAK_FENCE);
        b.set(mid - 1, 2, 0, lantern(false));

        if (b.chance(0.5F)) {
            b.hobbit(mid, 1, 2);
        }
    }
}
