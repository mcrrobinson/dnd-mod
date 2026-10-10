package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;

/**
 * A fighting room ({@link RoomRole#ENCOUNTER_SMALL} or {@link RoomRole#ENCOUNTER_LARGE}): four
 * columns, hanging lights and spawn points round the edges for its encounter.
 */
public class EncounterRoomPiece extends DungeonPiece {
    public EncounterRoomPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.ENCOUNTER_ROOM, info, box, seed, roomId, role, height, doors);
    }

    public EncounterRoomPiece(NbtCompound nbt) {
        super(DungeonStructures.ENCOUNTER_ROOM, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int w = this.width();
        int d = this.depth();
        int top = this.height;
        int in = 4;
        b.column(in, in, top);
        b.column(w - 1 - in, in, top);
        b.column(in, d - 1 - in, top);
        b.column(w - 1 - in, d - 1 - in, top);
        b.light(w / 2, top, d / 2, true);
        for (int x : new int[]{3, w / 2, w - 4}) {
            b.spawnPoint(x, 1, 3);
            b.spawnPoint(x, 1, d - 4);
        }
    }
}
