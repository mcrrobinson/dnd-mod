package mattonfire.dnd.world.gen.village;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * A market stall under a low thatched roof, its counter piled with produce and baking, bundles of pipe-weed
 * stacked round it and a wattle-and-daub back wall.
 */
public class MarketStallPiece extends HobbitPiece {
    private static final int WIDTH = 9;
    private static final int DEPTH = 7;
    private static final int HEIGHT = 6;
    private static final Block[] AWNINGS = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.GREEN_WOOL, Blocks.BLUE_WOOL, Blocks.ORANGE_WOOL};
    private static final Block[] PILES = {Blocks.PUMPKIN, Blocks.MELON, ModBlockFamilies.PIPEWEED_BUNDLE.base(),
            Blocks.PUMPKIN};

    public MarketStallPiece(BlockBox box, Direction facing, long seed) {
        super(HobbitVillageStructures.MARKET, box, facing, seed);
    }

    public MarketStallPiece(NbtCompound nbt) {
        super(HobbitVillageStructures.MARKET, nbt);
    }

    public static MarketStallPiece create(Random random, int x, int y, int z, Direction facing) {
        return new MarketStallPiece(centeredBox(x, y, z, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                b.ground(x, z, b.chance(0.7F) ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT);
                b.air(x, 1, z, x, HEIGHT - 1, z);
            }
        }

        // A low thatched roof, gabled along x: stairs at the eaves, a flat middle and a raised ridge.
        Block stripe = b.pick(AWNINGS);
        for (int x = 0; x < WIDTH; x++) {
            b.set(x, 4, 0, stairs(this.thatch(x, 4, 0), IN, false));
            b.set(x, 4, DEPTH - 1, stairs(this.thatch(x, 4, DEPTH - 1), OUT, false));
            for (int z = 1; z < DEPTH - 1; z++) {
                b.set(x, 4, z, this.thatch(x, 4, z).base());
            }
            b.set(x, 5, 2, stairs(this.thatch(x, 5, 2), IN, false));
            b.set(x, 5, 3, slab(this.thatch(x, 5, 3), false));
            b.set(x, 5, 4, stairs(this.thatch(x, 5, 4), OUT, false));
        }
        // A striped valance under the front eave.
        for (int x = 0; x < WIDTH; x++) {
            b.set(x, 3, 0, x % 2 == 0 ? Blocks.WHITE_WOOL : stripe);
        }
        for (int[] p : new int[][]{{1, 1}, {7, 1}, {1, 5}, {7, 5}}) {
            b.fill(p[0], 1, p[1], p[0], 3, p[1], Blocks.SPRUCE_FENCE);
        }
        // Back wall behind the stock.
        b.fill(1, 1, DEPTH - 1, 7, 2, DEPTH - 1, ModBlockFamilies.WATTLE_AND_DAUB.base());
        b.fill(1, 3, DEPTH - 1, 7, 3, DEPTH - 1, ModBlockFamilies.TIMBERED_WATTLE_AND_DAUB.base());

        // Counter of barrels with goods on top, more stock behind.
        for (int x = 2; x <= 6; x++) {
            b.barrel(x, 1, 1, Direction.UP, HARVEST);
            switch (b.random.nextInt(4)) {
                case 0 -> b.set(x, 2, 1, Blocks.CAKE);
                case 1 -> b.set(x, 2, 1, b.pick(PILES));
                default -> b.frame(x, 2, 1, Direction.UP, HobbitFood.produce(b.random));
            }
            b.barrel(x, 1, 5, OUT, x % 2 == 0 ? PANTRY : HARVEST);
            b.set(x, 2, 5, b.pick(PILES));
        }
        for (int z = 2; z <= 4; z++) {
            b.set(0, 1, z, b.pick(PILES));
            b.set(8, 1, z, b.pick(PILES));
            if (b.chance(0.5F)) {
                b.set(0, 2, z, b.pick(PILES));
            }
        }
        b.set(2, 3, 3, lantern(true));
        b.set(6, 3, 3, lantern(true));

        b.hobbit(4, 1, 3);
    }
}
