package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

/**
 * The treasure vault behind the boss ({@link RoomRole#VAULT}), or a side vault off the main path
 * ({@link RoomRole#SIDE_VAULT}). For now one chest of placeholder loot on a plinth; the Hoard
 * Coffer and tier loot come in dungeons ticket 5, the side vault's class gate in ticket 4.
 */
public class VaultPiece extends DungeonPiece {
    public VaultPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.VAULT, info, box, seed, roomId, role, height, doors);
    }

    public VaultPiece(NbtCompound nbt) {
        super(DungeonStructures.VAULT, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int mx = this.width() / 2;
        int mz = this.depth() / 2;
        Direction toDoor = this.doors.isEmpty() ? Direction.NORTH : this.doors.get(0).side();
        b.set(mx, 1, mz, b.palette.accent());
        b.chest(mx, 2, mz, toDoor, PLACEHOLDER_LOOT);
        b.light(mx - 2, this.height, mz, true);
        b.light(mx + 2, this.height, mz, true);
    }
}
