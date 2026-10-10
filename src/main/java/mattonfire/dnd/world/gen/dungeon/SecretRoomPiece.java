package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

/**
 * A small hidden room off the main path, behind a bricked-up doorway of cracked stone that anyone
 * can break (finding it by Perception comes later). One chest of {@code secret} tier loot.
 */
public class SecretRoomPiece extends DungeonPiece {
    public SecretRoomPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.SECRET_ROOM, info, box, seed, roomId, role, height, doors);
    }

    public SecretRoomPiece(NbtCompound nbt) {
        super(DungeonStructures.SECRET_ROOM, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int mx = this.width() / 2;
        int mz = this.depth() / 2;
        Direction toDoor = this.doors.isEmpty() ? Direction.NORTH : this.doors.get(0).side();
        b.chest(mx, 1, mz, toDoor, this.loot("secret"));
        b.set(mx - 1, this.height, mz - 1, Blocks.COBWEB);
        b.set(mx + 1, 1, mz + 1, Blocks.COBWEB);
    }
}
