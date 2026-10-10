package mattonfire.dnd.world.gen.enclave;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.Direction;

/**
 * The Moonwell, on the forest floor by the Heart Tree: a 5x5 pool two deep over a glass floor lit by sea
 * lanterns, rimmed with calcite and amethyst clusters, with a white banner on a post at each corner. Fill a
 * glass bottle from it at night for Moonwater ({@link mattonfire.dnd.world.gen.enclave.Moonwell}).
 */
public class MoonwellPiece extends EnclavePiece {
    /** Half the pool's width (5x5). */
    static final int POOL = 2;
    static final int REACH = 4;
    /** Local x of the rim, east of the pool. */
    public static final int RIM = POOL + 1;

    public MoonwellPiece(int x, int y, int z, long seed) {
        super(ElvenEnclaveStructures.MOONWELL, box(x, y, z, -REACH, -4, -REACH, REACH, 5, REACH), x, y, z, seed);
    }

    public MoonwellPiece(NbtCompound nbt) {
        super(ElvenEnclaveStructures.MOONWELL, nbt);
    }

    /** Whether local x/z is in the pool. */
    static boolean inPool(int x, int z) {
        return Math.abs(x) <= POOL && Math.abs(z) <= POOL;
    }

    @Override
    protected void build(Builder b) {
        b.clearForest(-REACH, 1, -REACH, REACH, 12, REACH);
        for (int x = -REACH; x <= REACH; x++) {
            for (int z = -REACH; z <= REACH; z++) {
                int c = Math.max(Math.abs(x), Math.abs(z));
                b.air(x, 1, z, x, 5, z);
                if (c <= POOL) {
                    b.set(x, 0, z, Blocks.WATER);
                    b.set(x, -1, z, Blocks.WATER);
                    b.set(x, -2, z, Blocks.GLASS);
                    b.set(x, -3, z, Blocks.SEA_LANTERN);
                    b.anchor(x, -3, z, Blocks.CALCITE.getDefaultState());
                } else if (c == POOL + 1) {
                    b.fill(x, -3, z, x, 0, z, Blocks.CALCITE);
                    b.anchor(x, -3, z, Blocks.DIRT.getDefaultState());
                    if ((x + z) % 2 == 0) {
                        b.set(x, 1, z, Blocks.AMETHYST_CLUSTER.getDefaultState().with(AmethystClusterBlock.FACING, Direction.UP));
                    }
                } else {
                    b.ground(x, z, this.noise(x, 0, z) < 0.4F ? Blocks.MOSS_BLOCK.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
                }
            }
        }
        for (int[] c : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            int x = c[0] * REACH;
            int z = c[1] * REACH;
            b.fill(x, 1, z, x, 3, z, Blocks.BIRCH_FENCE);
            b.set(x, 4, z, Blocks.WHITE_BANNER);
        }
        if (b.chance(0.5F)) {
            b.elf(0, 1, -REACH, ModEntityTypes.ELF_WARDEN);
        }
    }
}
