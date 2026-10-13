package mattonfire.dnd.world.gen.stronghold;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A hide yurt: round, 9 across and 7 high, a wall of brown and white hides under a conical roof round a
 * centre pole, the door facing the middle of the hold. Inside: straw beds, a chest of camp supplies and a
 * cauldron.
 */
public class YurtPiece extends StrongholdPiece {
    static final double RADIUS = 4.5D;
    static final int HEIGHT = 7;
    private static final Block[] HIDES = {Blocks.BROWN_WOOL, Blocks.BROWN_WOOL, Blocks.WHITE_WOOL, Blocks.LIGHT_GRAY_WOOL,
            Blocks.BROWN_TERRACOTTA};

    public YurtPiece(BlockPos center, Direction gate, long seed, long hold, int ya, int yb) {
        super(OrcStrongholdStructures.YURT, center, gate, seed, hold, ya - 5, 0, yb - 5, ya + 5, HEIGHT + 1, yb + 5);
        this.ya = ya;
        this.yb = yb;
    }

    public YurtPiece(NbtCompound nbt) {
        super(OrcStrongholdStructures.YURT, nbt);
        this.ya = nbt.getInt("YurtA");
        this.yb = nbt.getInt("YurtB");
    }

    private final int ya;
    private final int yb;

    @Override
    protected void writeNbt(net.minecraft.structure.StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("YurtA", this.ya);
        nbt.putInt("YurtB", this.yb);
    }

    @Override
    protected void build(Builder b) {
        // The door faces the middle of the hold.
        double len = Math.sqrt(this.ya * this.ya + this.yb * this.yb);
        double da = -this.ya / len;
        double db = -this.yb / len;
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                Block hide = b.pick(HIDES);
                double d = Math.sqrt(x * x + z * z);
                if (d > RADIUS) {
                    continue;
                }
                int a = this.ya + x;
                int bb = this.yb + z;
                int top = HEIGHT - (int) Math.round(d * 4.0D / RADIUS);
                b.set(a, 0, bb, Blocks.COARSE_DIRT);
                if (d < 2.5D) {
                    b.set(a, 1, bb, Blocks.BROWN_CARPET);
                }
                boolean wall = d > RADIUS - 1.0D;
                boolean door = wall && (x * da + z * db) / Math.max(d, 0.01D) > 0.85D;
                for (int y = d < 2.5D ? 2 : 1; y < top; y++) {
                    b.set(a, y, bb, wall && !(door && y <= 2) ? hide.getDefaultState() : Blocks.AIR.getDefaultState());
                }
                b.set(a, top, bb, hide);
            }
        }
        // The centre pole and the smoke hole round its top.
        for (int y = 1; y < HEIGHT; y++) {
            b.set(this.ya, y, this.yb, Blocks.SPRUCE_FENCE);
        }
        b.set(this.ya, HEIGHT, this.yb, pillar(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y));
        // Beds of straw and a chest of supplies at the back, a cauldron by the door.
        int backA = this.ya - (int) Math.round(da * 3);
        int backB = this.yb - (int) Math.round(db * 3);
        b.container(backA, 1, backB, facing(Blocks.CHEST, Direction.getFacing(da, 0.0D, db)), YURT_LOOT);
        int sideA = this.ya + (int) Math.round(db * 2);
        int sideB = this.yb - (int) Math.round(da * 2);
        b.set(sideA, 1, sideB, pillar(Blocks.HAY_BLOCK, Direction.Axis.Y));
        b.set(this.ya - (int) Math.round(db * 2), 1, this.yb + (int) Math.round(da * 2), pillar(Blocks.HAY_BLOCK, Direction.Axis.Y));
        b.set(this.ya + (int) Math.round(da * 2), 1, this.yb + (int) Math.round(db * 2), Blocks.CAULDRON);
    }
}
