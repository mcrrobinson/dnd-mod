package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;

/**
 * The puzzle room: four plinths where the rune pillars will stand, round the middle. Empty for
 * now: the rune-pillar puzzle comes in dungeons ticket 4.
 */
public class PuzzleRoomPiece extends DungeonPiece {
    public PuzzleRoomPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.PUZZLE_ROOM, info, box, seed, roomId, role, height, doors);
    }

    public PuzzleRoomPiece(NbtCompound nbt) {
        super(DungeonStructures.PUZZLE_ROOM, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int mx = this.width() / 2;
        int mz = this.depth() / 2;
        for (int[] o : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            b.set(mx + o[0], 1, mz + o[1], b.palette.trim());
            b.set(mx + o[0], 2, mz + o[1], b.palette.accent());
        }
        b.light(mx, this.height, mz, true);
    }
}
