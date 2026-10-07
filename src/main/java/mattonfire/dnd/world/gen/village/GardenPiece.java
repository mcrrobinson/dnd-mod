package mattonfire.dnd.world.gen.village;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * A fenced vegetable garden: rows of ripe crops either side of a path, irrigation channels, a
 * pumpkin patch, a scarecrow, a beehive and a barrel of the harvest.
 */
public class GardenPiece extends HobbitPiece {
    private static final int HEIGHT = 7;
    private static final Block[] CROPS = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};

    public GardenPiece(BlockBox box, Direction facing, long seed) {
        super(HobbitVillageStructures.GARDEN, box, facing, seed);
    }

    public GardenPiece(NbtCompound nbt) {
        super(HobbitVillageStructures.GARDEN, nbt);
    }

    public static GardenPiece create(Random random, int x, int y, int z, Direction facing) {
        int width = 11 + 2 * random.nextInt(3);
        int depth = 11 + 2 * random.nextInt(2);
        return new GardenPiece(centeredBox(x, y, z, facing, width, HEIGHT, depth), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        int w = this.width();
        int d = this.depth();
        int mid = w / 2;
        b.lawn(0, 0, w - 1, d - 1, HEIGHT - 1);

        // Beds in groups of three columns, each group one crop, with a water channel between groups.
        BlockState farmland = Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, 7);
        for (int x = 1; x < w - 1; x++) {
            int k = Math.abs(x - mid);
            if (k == 0) {
                continue;
            }
            Block crop = b.pick(CROPS);
            boolean patch = b.chance(0.2F);
            for (int z = 1; z < d - 1; z++) {
                if (k % 4 == 0 && z > 1 && z < d - 2) {
                    b.ground(x, z, Blocks.WATER);
                } else if (patch) {
                    float roll = b.random.nextFloat();
                    b.set(x, 1, z, roll < 0.3F ? Blocks.PUMPKIN : roll < 0.45F ? Blocks.MELON : Blocks.AIR);
                } else {
                    b.ground(x, z, farmland);
                    b.set(x, 1, z, ripe(crop));
                }
            }
        }
        for (int z = 0; z < d - 1; z++) {
            b.ground(mid, z, Blocks.DIRT_PATH);
        }

        // Fence, gate and lamps.
        for (int x = 0; x < w; x++) {
            b.set(x, 1, 0, Blocks.OAK_FENCE);
            b.set(x, 1, d - 1, Blocks.OAK_FENCE);
        }
        for (int z = 0; z < d; z++) {
            b.set(0, 1, z, Blocks.OAK_FENCE);
            b.set(w - 1, 1, z, Blocks.OAK_FENCE);
        }
        b.gate(mid, 1, 0, Blocks.OAK_FENCE_GATE, OUT);
        b.set(mid - 1, 2, 0, lantern(false));
        b.set(mid + 1, 2, 0, lantern(false));

        // Scarecrow
        int sx = mid + (b.random.nextBoolean() ? 2 : -2);
        int sz = d / 2;
        b.ground(sx, sz, Blocks.GRASS_BLOCK);
        b.set(sx, 1, sz, Blocks.OAK_FENCE);
        b.set(sx, 2, sz, Blocks.HAY_BLOCK);
        b.set(sx, 3, sz, facing(Blocks.CARVED_PUMPKIN, OUT));
        b.set(sx - 1, 2, sz, Blocks.OAK_FENCE);
        b.set(sx + 1, 2, sz, Blocks.OAK_FENCE);

        // Corners: beehive, compost, the harvest barrel and some hay.
        b.ground(1, 1, Blocks.GRASS_BLOCK);
        b.beehive(1, 1, 1, OUT, 2);
        b.ground(1, d - 2, Blocks.GRASS_BLOCK);
        b.set(1, 1, d - 2, Blocks.COMPOSTER);
        b.ground(w - 2, d - 2, Blocks.GRASS_BLOCK);
        b.barrel(w - 2, 1, d - 2, Direction.UP, HARVEST);
        b.frame(w - 2, 2, d - 2, Direction.UP, HobbitFood.produce(b.random));
        b.ground(w - 3, d - 2, Blocks.GRASS_BLOCK);
        b.set(w - 3, 1, d - 2, Blocks.HAY_BLOCK);
        b.set(w - 3, 2, d - 2, Blocks.AIR);
        b.ground(w - 2, 1, Blocks.GRASS_BLOCK);
        b.set(w - 2, 1, 1, Blocks.HAY_BLOCK);

        if (b.chance(0.6F)) {
            b.hobbit(mid, 1, d / 2);
        }
    }
}
