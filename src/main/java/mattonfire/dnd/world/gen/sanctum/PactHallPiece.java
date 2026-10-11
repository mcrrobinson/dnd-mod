package mattonfire.dnd.world.gen.sanctum;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The Pact Hall at the back of the sanctum, the Tiefling hearth: an 11 by 12 nave of crimson planks between
 * polished blackstone brick piers, under a gabled crimson roof, with red glass windows. Inside: pews either side
 * of the aisle, two soul-fire chandeliers, and at the far end an altar of crying obsidian with red candles, a
 * lectern before it and the offering chest behind it (a DC 15 lock). The Pactbroker keeps to the hall.
 */
public class PactHallPiece extends BrimstonePiece {
    static final int A = 5;
    static final int FRONT = -3;
    static final int BACK = -14;
    static final int EAVES = 7;
    static final int ALTAR_B = BACK + 2;
    /** The Pactbroker's spot, beside the lectern. */
    static final int BROKER_A = 2;
    static final int BROKER_B = ALTAR_B + 3;

    public PactHallPiece(BlockPos center, Direction gate, long seed, long sanctum) {
        super(BrimstoneSanctumStructures.PACT_HALL, center, gate, seed, sanctum, -A, 0, BACK, A, EAVES + A + 1, FRONT + 1);
    }

    public PactHallPiece(NbtCompound nbt) {
        super(BrimstoneSanctumStructures.PACT_HALL, nbt);
    }

    private static int roofY(int a) {
        return EAVES + (A - Math.abs(a));
    }

    /** The offering chest behind the altar, in the world. */
    public BlockPos offering() {
        return this.at(0, 1, BACK + 1);
    }

    /** The altar's middle block, in the world. */
    public BlockPos altar() {
        return this.at(0, 1, ALTAR_B);
    }

    /** Where the Pactbroker stands, in the world. */
    public BlockPos broker() {
        return this.at(BROKER_A, 1, BROKER_B);
    }

    @Override
    protected void build(Builder b) {
        for (int x = -A; x <= A; x++) {
            for (int z = BACK; z <= FRONT; z++) {
                boolean aisle = Math.abs(x) <= 1;
                b.set(x, 0, z, aisle ? Blocks.POLISHED_BLACKSTONE : Blocks.CRIMSON_PLANKS);
                int top = roofY(x);
                boolean side = Math.abs(x) == A;
                boolean end = z == BACK || z == FRONT;
                boolean pier = side && (z - BACK) % 3 == 0 || end && side;
                for (int y = 1; y < top; y++) {
                    if (pier && y < EAVES) {
                        b.set(x, y, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
                    } else if (side && y < EAVES) {
                        boolean window = (y == 3 || y == 4) && (z - BACK) % 3 != 0 && (z - BACK) % 6 > 3;
                        b.set(x, y, z, window ? Blocks.RED_STAINED_GLASS_PANE.getDefaultState() : Blocks.CRIMSON_PLANKS.getDefaultState());
                    } else if (end) {
                        b.set(x, y, z, y >= EAVES || Math.abs(x) == A - 1 ? masonry(b) : Blocks.CRIMSON_PLANKS);
                    } else {
                        b.set(x, y, z, Blocks.AIR);
                    }
                }
                // The gabled roof: crimson stairs climbing to a blackstone ridge.
                if (x == 0) {
                    b.set(x, top, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
                    b.set(x, top + 1, z, slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, false));
                } else {
                    b.set(x, top, z, stairs(Blocks.CRIMSON_STAIRS, x < 0 ? Direction.EAST : Direction.WEST, false));
                }
            }
        }
        // The door onto the square, with soul lanterns either side.
        b.air(-1, 1, FRONT, 1, 4, FRONT);
        b.set(-2, 4, FRONT + 1, soulLantern(false));
        b.set(2, 4, FRONT + 1, soulLantern(false));
        b.fill(-2, 1, FRONT + 1, -2, 3, FRONT + 1, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        b.fill(2, 1, FRONT + 1, 2, 3, FRONT + 1, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);

        // Pews either side of the aisle, facing the altar.
        for (int z = BROKER_B + 1; z <= FRONT - 2; z += 2) {
            for (int x = 2; x <= A - 1; x++) {
                b.set(x, 1, z, stairs(Blocks.CRIMSON_STAIRS, Direction.SOUTH, false));
                b.set(-x, 1, z, stairs(Blocks.CRIMSON_STAIRS, Direction.SOUTH, false));
            }
        }

        // The altar: crying obsidian on a gilded step, red candles, the lectern before it.
        b.fill(-2, 0, ALTAR_B - 1, 2, 0, ALTAR_B + 1, Blocks.GILDED_BLACKSTONE);
        b.fill(-1, 1, ALTAR_B, 1, 1, ALTAR_B, Blocks.CRYING_OBSIDIAN);
        for (int x = -1; x <= 1; x++) {
            b.set(x, 2, ALTAR_B, candle(b, x == 0 ? Blocks.BLACK_CANDLE : Blocks.RED_CANDLE));
        }
        b.set(0, 1, ALTAR_B + 2, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.SOUTH));
        // The offering chest behind it, between two crying obsidian pillars.
        b.container(0, 1, BACK + 1, facing(Blocks.CHEST, Direction.SOUTH), ALTAR_LOOT);
        b.fill(-2, 1, BACK + 1, -2, 3, BACK + 1, Blocks.CRYING_OBSIDIAN);
        b.fill(2, 1, BACK + 1, 2, 3, BACK + 1, Blocks.CRYING_OBSIDIAN);

        // Soul-fire chandeliers from the ridge.
        b.chandelier(0, roofY(0) - 1, ALTAR_B + 3, 3);
        b.chandelier(0, roofY(0) - 1, FRONT - 3, 3);

        b.tiefling(BROKER_A, 1, BROKER_B, ModEntityTypes.TIEFLING_PACTBROKER, 4);
    }

    private static BlockState candle(Builder b, Block candle) {
        return candle.getDefaultState().with(CandleBlock.CANDLES, 1 + b.random.nextInt(4)).with(CandleBlock.LIT, true);
    }
}
