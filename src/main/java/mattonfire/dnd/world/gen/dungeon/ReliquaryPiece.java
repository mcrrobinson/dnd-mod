package mattonfire.dnd.world.gen.dungeon;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import mattonfire.dnd.classes.Registry.CryptBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

/**
 * The Crypt's reliquary ({@link RoomRole#RELIQUARY}): a side room through a door in the boss room's
 * wall, where the Lich keeps its phylactery. Four pillars round a Phylactery Pedestal in the middle, on
 * polished deepslate steps in a ring of Lich Sigil Floor; the phylactery appears on the pedestal when the boss room calls up the Lich (see
 * {@link #phylacterySpot}), about 25 blocks from the boss's dais, inside the Lich's leash. Grave candle
 * clusters on the diagonals leave the four straight ways to the altar clear.
 */
public class ReliquaryPiece extends DungeonPiece {
    public static final int SIZE = 13;
    public static final int HEIGHT = 6;

    public ReliquaryPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.RELIQUARY, info, box, seed, roomId, role, height, doors);
    }

    public ReliquaryPiece(NbtCompound nbt) {
        super(DungeonStructures.RELIQUARY, nbt);
    }

    /**
     * Where the phylactery stands in a reliquary with this box: on the pedestal in the middle, one
     * block above the floor ({@code floorY + 2}).
     */
    public static BlockPos phylacterySpot(BlockBox box, int floorY) {
        return new BlockPos(box.getMinX() + box.getBlockCountX() / 2, floorY + 2, box.getMinZ() + box.getBlockCountZ() / 2);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int w = this.width();
        int d = this.depth();
        int mx = w / 2;
        int mz = d / 2;
        b.set(mx, 1, mz, CryptBlocks.PHYLACTERY_PEDESTAL.getDefaultState());
        for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            b.set(mx + o[0], 1, mz + o[1], stairs(Blocks.POLISHED_DEEPSLATE_STAIRS,
                    net.minecraft.util.math.Direction.fromVector(-o[0], 0, -o[1]), false));
        }
        for (int[] o : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            b.column(mx + o[0], mz + o[1], this.height);
        }
        b.light(mx, this.height, mz - 3, true);
        b.light(mx, this.height, mz + 3, true);
        b.light(mx - 3, this.height, mz, true);
        b.light(mx + 3, this.height, mz, true);
        if (CryptDressing.applies(this)) {
            CryptDressing.sigilRing(b, mx, mz, 1.5D, 2.9D);
            for (int[] o : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
                CryptDressing.candleCluster(b, mx + o[0], 1, mz + o[1]);
            }
            CryptDressing.wallReliefs(b, w, d, 3, new int[]{-3, 3}, 0.75F);
            CryptDressing.mossPatches(b, w, d, 2);
            for (int[] c : CryptDressing.corners(w, d)) {
                CryptDressing.bonePile(b, c[0], c[1]);
            }
            CryptDressing.cornerCobwebs(b, w, d, this.height, 0.8F);
            for (int[] o : new int[][]{{-2, -4}, {2, -4}, {-2, 4}, {2, 4}, {-4, -2}, {-4, 2}, {4, -2}, {4, 2}}) {
                if (b.dressing.nextFloat() < 0.3F) {
                    CryptDressing.skull(b, mx + o[0], 1, mz + o[1]);
                }
            }
        }
    }
}
