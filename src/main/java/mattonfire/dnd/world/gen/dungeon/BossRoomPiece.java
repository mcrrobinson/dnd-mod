package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;

/**
 * The boss's hall, two cells by two: a tall pillared chamber with a raised dais in the middle,
 * where the boss appears when the party walks in. In the Crypt, a reliquary off one wall holds the
 * Lich's phylactery ({@link ReliquaryPiece}). In the Goblin Warren it's the Warlord's war hall.
 */
public class BossRoomPiece extends DungeonPiece {
    public BossRoomPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.BOSS_ROOM, info, box, seed, roomId, role, height, doors);
    }

    public BossRoomPiece(NbtCompound nbt) {
        super(DungeonStructures.BOSS_ROOM, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int w = this.width();
        int d = this.depth();
        int mx = w / 2;
        int mz = d / 2;
        // Two rings of columns, leaving the lines through the doorways clear
        for (int x = 5; x < w - 4; x += 5) {
            for (int z = 5; z < d - 4; z += 5) {
                boolean ring = x == 5 || z == 5 || x >= w - 6 || z >= d - 6;
                boolean nearMiddle = Math.abs(x - mx) <= 3 || Math.abs(z - mz) <= 3;
                if (ring && !nearMiddle) {
                    b.column(x, z, this.height);
                }
            }
        }
        // The dais
        b.fill(mx - 3, 1, mz - 3, mx + 3, 1, mz + 3, b.palette.trim());
        b.fill(mx - 1, 2, mz - 1, mx + 1, 2, mz + 1, b.palette.accent());
        for (int[] o : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
            b.light(mx + o[0], this.height, mz + o[1], true);
        }
        for (int[] o : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}, {0, -9}, {0, 9}, {-9, 0}, {9, 0}}) {
            b.spawnPoint(mx + o[0], 1, mz + o[1]);
        }
        if (CryptDressing.applies(this)) {
            this.tomb(b, w, d, mx, mz);
        } else if (WarrenDressing.applies(this)) {
            this.warHall(b, w, d, mx, mz);
        }
    }

    /**
     * The Lich's tomb: sarcophagi along the middle of each wall (the doorways are a cell's middle,
     * well to either side), candles on the dais corners, bones in the corners.
     */
    private void tomb(Builder b, int w, int d, int mx, int mz) {
        CryptDressing.wallSarcophagi(b, w, d, 11, 0.75F);
        for (int[] o : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            CryptDressing.candles(b, mx + o[0], 2, mz + o[1]);
        }
        for (int[] c : CryptDressing.corners(w, d)) {
            CryptDressing.bonePile(b, c[0], c[1]);
        }
        for (int[] o : new int[][]{{-6, -5}, {6, -5}, {-6, 5}, {6, 5}, {-5, -6}, {5, -6}, {-5, 6}, {5, 6}}) {
            if (b.dressing.nextFloat() < 0.4F) {
                CryptDressing.skull(b, mx + o[0], 1, mz + o[1]);
            }
        }
        CryptDressing.cornerCobwebs(b, w, d, this.height, 0.9F);
    }

    /**
     * The Goblin Warlord's war hall: cages and hide lean-tos along the middle of each wall (between
     * the doorways), skull totems at the dais corners, four fire pits under the hanging lanterns,
     * bones and refuse pits.
     */
    private void warHall(Builder b, int w, int d, int mx, int mz) {
        WarrenDressing.wallProps(b, w, d, 11, 0.8F, true);
        for (int[] o : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            WarrenDressing.totem(b, mx + o[0], mz + o[1]);
        }
        for (int[] o : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
            WarrenDressing.firePit(b, mx + o[0], mz + o[1]);
        }
        for (int[] c : CryptDressing.corners(w, d)) {
            CryptDressing.bonePile(b, c[0], c[1]);
        }
        for (int[] o : new int[][]{{-12, -4}, {12, 3}, {-3, 12}, {4, -12}}) {
            if (b.dressing.nextFloat() < 0.6F) {
                WarrenDressing.refusePit(b, mx + o[0], mz + o[1]);
            }
        }
        CryptDressing.cornerCobwebs(b, w, d, this.height, 0.7F);
    }
}
