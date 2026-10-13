package mattonfire.dnd.world.gen.stronghold;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import net.minecraft.block.BannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The Fighting Pit in the middle of the hold: round, radius {@link #RADIUS}, sunk {@link #DEPTH} blocks, with
 * a sand floor, mud-brick walls, a fence ring round the rim, steps down on the gate side, and the war drum on
 * the far rim (a note block on a hay bale under a red banner). Right-clicking the drum from inside the pit
 * starts the Trial of Strength ({@link mattonfire.dnd.entity.OrcTrial}).
 */
public class FightingPitPiece extends StrongholdPiece {
    public static final int RADIUS = 7;
    public static final int DEPTH = 3;
    private static final int PA = OrcStrongholdStructure.PIT_A;
    private static final int PB = OrcStrongholdStructure.PIT_B;

    public FightingPitPiece(BlockPos center, Direction gate, long seed, long hold) {
        super(OrcStrongholdStructures.PIT, center, gate, seed, hold, PA - RADIUS - 2, -DEPTH - 1, PB - RADIUS - 2,
                PA + RADIUS + 2, 4, PB + RADIUS + 2);
    }

    public FightingPitPiece(NbtCompound nbt) {
        super(OrcStrongholdStructures.PIT, nbt);
    }

    /** The war drum's note block, in the world. */
    public BlockPos drum() {
        return this.at(PA, 2, PB - RADIUS - 1);
    }

    /** Where a fighter stands on the pit floor, {@code offset} blocks from the middle towards the gate. */
    public BlockPos floor(int offset) {
        return this.at(PA, -DEPTH + 1, PB + offset);
    }

    /** Whether {@code pos} is down in the pit (inside the wall, below the rim). */
    public boolean inPit(BlockPos pos) {
        BlockPos middle = this.floor(0);
        double dx = pos.getX() - middle.getX();
        double dz = pos.getZ() - middle.getZ();
        return dx * dx + dz * dz <= (RADIUS + 0.5D) * (RADIUS + 0.5D) && pos.getY() < middle.getY() + DEPTH;
    }

    @Override
    protected void build(Builder b) {
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                float r = b.random.nextFloat();
                double d = Math.sqrt(x * x + z * z);
                int a = PA + x;
                int bb = PB + z;
                if (d <= RADIUS + 0.5D) {
                    b.air(a, -DEPTH + 1, bb, a, 0, bb);
                    b.set(a, -DEPTH, bb, r < 0.85F ? Blocks.SAND : Blocks.RED_SAND);
                    b.foundation(a, -DEPTH, bb, Blocks.SANDSTONE.getDefaultState(), 8);
                } else if (d <= RADIUS + 1.5D) {
                    Block wall = r < 0.7F ? Blocks.MUD_BRICKS : r < 0.85F ? Blocks.PACKED_MUD : Blocks.RED_TERRACOTTA;
                    b.fill(a, -DEPTH, bb, a, 0, bb, wall);
                    boolean entry = Math.abs(x) <= 1 && z > 0;
                    boolean drum = x == 0 && z < 0;
                    if (!entry && !drum) {
                        b.set(a, 1, bb, Blocks.DARK_OAK_FENCE);
                        if (r > 0.93F) {
                            b.set(a, 2, bb, skull(b.random));
                        }
                    }
                }
            }
        }
        // Steps down on the gate side.
        for (int x = -1; x <= 1; x++) {
            b.fill(PA + x, -DEPTH + 1, PB + RADIUS, PA + x, -DEPTH + 1, PB + RADIUS, Blocks.MUD_BRICKS);
            b.set(PA + x, -DEPTH + 2, PB + RADIUS, stairs(Blocks.MUD_BRICK_STAIRS, Direction.SOUTH, false));
            b.set(PA + x, -DEPTH + 1, PB + RADIUS - 1, stairs(Blocks.MUD_BRICK_STAIRS, Direction.SOUTH, false));
            b.set(PA + x, 0, PB + RADIUS + 1, stairs(Blocks.MUD_BRICK_STAIRS, Direction.SOUTH, false));
        }
        // The war drum on the far rim.
        int drumB = PB - RADIUS - 1;
        b.set(PA, 1, drumB, pillar(Blocks.HAY_BLOCK, Direction.Axis.Y));
        b.set(PA, 2, drumB, Blocks.NOTE_BLOCK);
        b.set(PA, 3, drumB, Blocks.RED_BANNER.getDefaultState().with(BannerBlock.ROTATION, 0));
        b.set(PA - 1, 1, drumB, Blocks.DARK_OAK_FENCE);
        b.set(PA + 1, 1, drumB, Blocks.DARK_OAK_FENCE);
        // Torches on posts round the rim.
        for (int[] post : new int[][]{{-RADIUS - 2, 0}, {RADIUS + 2, 0}}) {
            b.set(PA + post[0], 1, PB + post[1], Blocks.DARK_OAK_FENCE);
            b.set(PA + post[0], 2, PB + post[1], Blocks.TORCH);
        }
    }
}
