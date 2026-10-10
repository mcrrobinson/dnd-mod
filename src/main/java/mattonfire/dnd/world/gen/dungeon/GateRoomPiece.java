package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

/**
 * A room split across the path by a wall with a 3-wide gateway. The gateway is open for now; the
 * class-check obstacle that bars it comes in dungeons ticket 4.
 */
public class GateRoomPiece extends DungeonPiece {
    public GateRoomPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.GATE_ROOM, info, box, seed, roomId, role, height, doors);
    }

    public GateRoomPiece(NbtCompound nbt) {
        super(DungeonStructures.GATE_ROOM, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int w = this.width();
        int d = this.depth();
        // The path runs between the two doors; the gate wall stands across it, halfway.
        boolean alongX = !this.doors.isEmpty() && this.doors.get(0).side().getAxis() == Direction.Axis.X;
        int across = alongX ? d : w;
        int mid = (alongX ? w : d) / 2;
        int centre = across / 2;
        for (int t = 2; t < across - 2; t++) {
            boolean gateway = Math.abs(t - centre) <= 1;
            boolean jamb = Math.abs(t - centre) == 2;
            for (int y = 1; y <= this.height; y++) {
                if (gateway && y <= DOOR_HEIGHT) {
                    continue;
                }
                var state = jamb || (gateway && y == DOOR_HEIGHT + 1) ? b.palette.accent() : b.palette.wall(b.random);
                if (alongX) {
                    b.set(mid, y, t, state);
                } else {
                    b.set(t, y, mid, state);
                }
            }
        }
        b.light(alongX ? mid - 2 : centre, this.height, alongX ? centre : mid - 2, true);
        b.light(alongX ? mid + 2 : centre, this.height, alongX ? centre : mid + 2, true);
    }
}
