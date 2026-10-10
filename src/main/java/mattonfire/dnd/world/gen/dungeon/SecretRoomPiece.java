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
        if (CryptDressing.applies(this)) {
            // Whoever hid this didn't get out
            int[] c = CryptDressing.corners(this.width(), this.depth())[b.dressing.nextInt(4)];
            CryptDressing.bonePile(b, c[0], c[1]);
        } else if (WarrenDressing.applies(this)) {
            // A goblin's private stash, and its bed
            int[] c = CryptDressing.corners(this.width(), this.depth())[b.dressing.nextInt(4)];
            WarrenDressing.bedroll(b, c[0], c[1], Direction.EAST);
        } else if (RuinDressing.applies(this)) {
            // A dwarf's last hiding place: their bones and their anvil
            int[][] corners = CryptDressing.corners(this.width(), this.depth());
            int i = b.dressing.nextInt(4);
            CryptDressing.bonePile(b, corners[i][0], corners[i][1]);
            RuinDressing.anvil(b, corners[(i + 2) % 4][0], corners[(i + 2) % 4][1], Direction.fromHorizontal(b.dressing.nextInt(4)));
        }
    }
}
