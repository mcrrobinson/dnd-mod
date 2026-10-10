package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;

/**
 * The mid-boss's hall: the biggest single room, ringed with columns. In the Crypt it's the Ossuary
 * Cube's lair, with bones heaped round the walls; in the Goblin Warren, the pen where Grimclaw, the
 * goblins' owlbear, is chained.
 */
public class ChampionRoomPiece extends DungeonPiece {
    public ChampionRoomPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.CHAMPION_ROOM, info, box, seed, roomId, role, height, doors);
    }

    public ChampionRoomPiece(NbtCompound nbt) {
        super(DungeonStructures.CHAMPION_ROOM, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int w = this.width();
        int d = this.depth();
        for (int x : new int[]{3, w - 4}) {
            for (int z : new int[]{3, d / 2, d - 4}) {
                if (z == d / 2) {
                    continue;
                }
                b.column(x, z, this.height);
            }
        }
        b.column(w / 2, 3, this.height);
        b.column(w / 2, d - 4, this.height);
        b.light(w / 2, this.height, d / 2, true);
        for (int[] p : new int[][]{{4, 5}, {w - 5, 5}, {4, d - 6}, {w - 5, d - 6}, {w / 2, 5}, {w / 2, d - 6}, {5, d / 2}, {w - 6, d / 2}}) {
            b.spawnPoint(p[0], 1, p[1]);
        }
        if (CryptDressing.applies(this)) {
            this.ossuary(b, w, d);
        } else if (WarrenDressing.applies(this)) {
            this.pen(b, w, d);
        }
    }

    /** The Ossuary Cube's hall: bones heaped in the corners and along the walls, skulls strewn about. */
    private void ossuary(Builder b, int w, int d) {
        int mx = w / 2;
        int mz = d / 2;
        for (int[] c : CryptDressing.corners(w, d)) {
            CryptDressing.bonePile(b, c[0], c[1]);
        }
        for (int[] p : new int[][]{{2, 5}, {2, d - 6}, {w - 3, 5}, {w - 3, d - 6}, {5, 2}, {w - 6, 2}, {5, d - 3}, {w - 6, d - 3}}) {
            if (b.dressing.nextFloat() < 0.5F) {
                CryptDressing.bonePile(b, p[0], p[1]);
            }
        }
        // Skulls on the floor, off the straight lines through the doorways
        for (int i = 0; i < 10; i++) {
            int x = 2 + b.dressing.nextInt(w - 4);
            int z = 2 + b.dressing.nextInt(d - 4);
            if (Math.abs(x - mx) > 1 && Math.abs(z - mz) > 1) {
                CryptDressing.skull(b, x, 1, z);
            }
        }
        CryptDressing.cornerCobwebs(b, w, d, this.height, 0.8F);
    }

    /**
     * Grimclaw's pen: four chains hang from the ceiling round the middle, where the beast is
     * chained; gnawed bones in the corners, straw and slops along the walls, a refuse pit.
     */
    private void pen(Builder b, int w, int d) {
        int mx = w / 2;
        int mz = d / 2;
        for (int[] o : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            WarrenDressing.chain(b, mx + o[0], mz + o[1], this.height);
        }
        for (int[] c : CryptDressing.corners(w, d)) {
            CryptDressing.bonePile(b, c[0], c[1]);
        }
        for (int[] p : new int[][]{{2, 5}, {2, d - 6}, {w - 3, 5}, {w - 3, d - 6}, {5, 2}, {w - 6, 2}, {5, d - 3}, {w - 6, d - 3}}) {
            float r = b.dressing.nextFloat();
            if (r < 0.35F) {
                b.setIfAir(p[0], 1, p[1], net.minecraft.block.Blocks.HAY_BLOCK.getDefaultState());
            } else if (r < 0.55F) {
                WarrenDressing.stores(b, p[0], p[1]);
            } else if (r < 0.75F) {
                CryptDressing.bonePile(b, p[0], p[1]);
            }
        }
        WarrenDressing.refusePit(b, 2 + b.dressing.nextInt(2), mz + 2 + b.dressing.nextInt(2));
        CryptDressing.cornerCobwebs(b, w, d, this.height, 0.6F);
    }
}
