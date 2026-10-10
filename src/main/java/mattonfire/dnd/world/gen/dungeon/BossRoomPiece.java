package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;

/**
 * The boss's hall, two cells by two: a tall pillared chamber with a raised dais in the middle,
 * where the boss will be spawned (dungeons ticket 2).
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
    }
}
