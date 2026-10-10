package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;

/** The mid-boss's hall: the biggest single room, ringed with columns. The champion arrives in dungeons ticket 2. */
public class ChampionRoomPiece extends DungeonPiece {
    public ChampionRoomPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.CHAMPION_ROOM, info, box, seed, roomId, role, height, doors);
    }

    public ChampionRoomPiece(NbtCompound nbt) {
        super(DungeonStructures.CHAMPION_ROOM, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int w = this.width();
        int d = this.depth();
        for (int x : new int[]{3, w - 4}) {
            for (int z : new int[]{3, d / 2, d - 4}) {
                if (z == d / 2) {
                    continue;
                }
                b.column(x, z, this.height);
            }
        }
        b.column(w / 2, 3, this.height);
        b.column(w / 2, d - 4, this.height);
        b.light(w / 2, this.height, d / 2, true);
        for (int[] p : new int[][]{{4, 5}, {w - 5, 5}, {4, d - 6}, {w - 5, d - 6}, {w / 2, 5}, {w / 2, d - 6}, {5, d / 2}, {w - 6, d / 2}}) {
            b.spawnPoint(p[0], 1, p[1]);
        }
    }
}
