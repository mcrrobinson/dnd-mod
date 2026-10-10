package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;

/** The safe room at the foot of the stair: a cold campfire to rest by, before the first fight. */
public class AntechamberPiece extends DungeonPiece {
    public AntechamberPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.ANTECHAMBER, info, box, seed, roomId, role, height, doors);
    }

    public AntechamberPiece(NbtCompound nbt) {
        super(DungeonStructures.ANTECHAMBER, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int mx = this.width() / 2;
        int mz = this.depth() / 2;
        b.set(mx, 1, mz, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));
        int w = this.width();
        int d = this.depth();
        b.light(2, 1, 2, false);
        b.light(w - 3, 1, 2, false);
        b.light(2, 1, d - 3, false);
        b.light(w - 3, 1, d - 3, false);
    }
}
