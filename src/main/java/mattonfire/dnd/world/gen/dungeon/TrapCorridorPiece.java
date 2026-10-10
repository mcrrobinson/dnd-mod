package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;

/**
 * A long, narrow hall the path runs straight through, 3 wide inside. Empty for now: traps arrive
 * in dungeons ticket 3.
 */
public class TrapCorridorPiece extends DungeonPiece {
    public TrapCorridorPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.TRAP_CORRIDOR, info, box, seed, roomId, role, height, doors);
    }

    public TrapCorridorPiece(NbtCompound nbt) {
        super(DungeonStructures.TRAP_CORRIDOR, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        boolean alongX = this.width() > this.depth();
        int length = alongX ? this.width() : this.depth();
        for (int i = 4; i < length - 3; i += 7) {
            if (alongX) {
                b.light(i, this.height, this.depth() / 2, true);
            } else {
                b.light(this.width() / 2, this.height, i, true);
            }
        }
    }
}
