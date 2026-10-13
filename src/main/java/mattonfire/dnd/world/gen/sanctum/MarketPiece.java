package mattonfire.dnd.world.gen.sanctum;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * One stall of the Brass Market, east of the square (three of them, see
 * {@link BrimstoneSanctumStructure#STALLS}): crimson posts under an awning of orange, yellow or red wool, a
 * blackstone counter facing the square, and barrels of wares behind it (one filled from the market table).
 */
public class MarketPiece extends BrimstonePiece {
    private static final Block[] AWNINGS = {Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.RED_WOOL};
    private final int stallA;
    private final int stallB;

    public MarketPiece(BlockPos center, Direction gate, long seed, long sanctum, int stallA, int stallB) {
        super(BrimstoneSanctumStructures.MARKET, center, gate, seed, sanctum, stallA - 1, 0, stallB - 1, stallA + 1, 4, stallB + 1);
        this.stallA = stallA;
        this.stallB = stallB;
    }

    public MarketPiece(NbtCompound nbt) {
        super(BrimstoneSanctumStructures.MARKET, nbt);
        this.stallA = nbt.getInt("StallA");
        this.stallB = nbt.getInt("StallB");
    }

    @Override
    protected void writeNbt(net.minecraft.structure.StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("StallA", this.stallA);
        nbt.putInt("StallB", this.stallB);
    }

    @Override
    protected void build(Builder b) {
        int a = this.stallA;
        int z = this.stallB;
        Block awning = b.pick(AWNINGS);
        b.fill(a - 1, 0, z - 1, a + 1, 0, z + 1, Blocks.POLISHED_BLACKSTONE);
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                b.fill(a + dx, 1, z + dz, a + dx, 2, z + dz, Blocks.CRIMSON_FENCE);
            }
        }
        b.fill(a - 1, 3, z - 1, a + 1, 3, z + 1, awning);
        b.set(a, 4, z, Blocks.GOLD_BLOCK);
        // The counter, facing the square (west), and the wares behind it.
        b.set(a - 1, 1, z, slab(Blocks.POLISHED_BLACKSTONE_SLAB, true));
        b.container(a + 1, 1, z, facing(Blocks.BARREL, Direction.UP), MARKET_LOOT);
        b.set(a + 1, 2, z, facing(Blocks.BARREL, Direction.UP));
        b.set(a, 1, z, Blocks.AIR);
        b.set(a, 2, z, soulLantern(true));
    }
}
