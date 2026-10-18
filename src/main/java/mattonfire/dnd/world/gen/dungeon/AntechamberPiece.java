package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;

/**
 * The safe room at the foot of the stair: a cold campfire to rest by, before the first fight. In
 * the Crypt, sarcophagi line its walls; in the Goblin Warren, hide lean-tos and the War Order Board;
 * in the Dwarven Ruin, cracked anvils, barrels and rubble.
 */
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
        if (CryptDressing.applies(this)) {
            // The dead laid out along the walls, either side of the doorways
            CryptDressing.wallSarcophagi(b, w, d, 3, 0.5F);
            CryptDressing.mossPatches(b, w, d, 2);
            CryptDressing.cornerCobwebs(b, w, d, this.height, 0.6F);
        } else if (WarrenDressing.applies(this)) {
            // The goblins' sleeping den: hide lean-tos along the walls
            WarrenDressing.wallProps(b, w, d, 3, 0.6F, false);
            WarrenDressing.warOrders(b, w);
            CryptDressing.cornerCobwebs(b, w, d, this.height, 0.4F);
        } else if (RuinDressing.applies(this)) {
            // The gate guards' hall: cracked anvils, barrels and fallen stone along the walls
            RuinDressing.wallProps(b, w, d, 3, 0.6F);
            CryptDressing.cornerCobwebs(b, w, d, this.height, 0.6F);
        }
    }
}
