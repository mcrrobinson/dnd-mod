package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

/**
 * The treasure vault behind the boss ({@link RoomRole#VAULT}), or a side vault off the main path
 * ({@link RoomRole#SIDE_VAULT}), with one thing on a plinth in the middle. The treasure vault holds
 * the Hoard Coffer (a roll per player once the dungeon is cleared); the side vault a locked chest of
 * {@code side_vault} loot (lock DC +2). The side vault's class gate comes in dungeons ticket 4.
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
        if (this.role() == RoomRole.VAULT) {
            b.hoardCoffer(mx, 2, mz, toDoor);
        } else {
            b.chest(mx, 2, mz, toDoor, this.loot("side_vault"));
        }
        b.light(mx - 2, this.height, mz, true);
        b.light(mx + 2, this.height, mz, true);
    }
}
