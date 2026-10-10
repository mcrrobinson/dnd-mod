package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import java.util.ArrayList;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The mid-boss's hall: the biggest single room, ringed with columns. In the Crypt it's the Ossuary
 * Cube's lair, with bones heaped round the walls; in the Goblin Warren, the pen where Grimclaw, the
 * goblins' owlbear, is chained; in the Dwarven Ruin, the Mimic Matriarch's gallery: eight chest
 * spots round the walls ({@link #gallerySpots}), five of them real chests of {@code gallery} loot and
 * three left empty for the mimics the room's ward puts there when the fight starts.
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
        } else if (RuinDressing.applies(this)) {
            this.gallery(b, w, d);
        }
    }

    /** How many of the gallery's {@link #GALLERY_SPOTS} spots hold mimics: the Matriarch and two more. */
    public static final int GALLERY_MIMICS = 3;
    /**
     * The gallery's chest spots (local x, z) and which way each chest faces: two along each wall,
     * clear of the doorways (a wall's middle), the columns and the spawn points.
     */
    private static final int[][] GALLERY_SPOTS = {{2, 4}, {2, 10}, {12, 4}, {12, 10}, {4, 2}, {10, 2}, {4, 12}, {10, 12}};

    /** One gallery spot: where the chest (or the mimic) sits, and the way it faces. */
    public record GallerySpot(BlockPos pos, Direction facing) {
    }

    /** The gallery spots of a mid-boss room with box {@code box}, at floor level {@code floorY} (+1). */
    public static List<GallerySpot> gallerySpots(BlockBox box, int floorY) {
        List<GallerySpot> spots = new ArrayList<>();
        int w = box.getBlockCountX();
        int d = box.getBlockCountZ();
        for (int[] s : GALLERY_SPOTS) {
            int x = s[0] * (w - 1) / 14;
            int z = s[1] * (d - 1) / 14;
            Direction facing = x <= 2 ? Direction.EAST : x >= w - 3 ? Direction.WEST : z <= 2 ? Direction.SOUTH : Direction.NORTH;
            spots.add(new GallerySpot(new BlockPos(box.getMinX() + x, floorY + 1, box.getMinZ() + z), facing));
        }
        return spots;
    }

    /**
     * The Mimic Matriarch's gallery: five of the eight spots get a real chest of {@code gallery} loot
     * (never a mimic: the mimics are placed by the ward when the fight starts, in the empty spots),
     * a red carpet in the middle, cobwebs.
     */
    private void gallery(Builder b, int w, int d) {
        List<Integer> order = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7));
        for (int i = order.size() - 1; i > 0; i--) {
            java.util.Collections.swap(order, i, b.random.nextInt(i + 1));
        }
        List<GallerySpot> spots = gallerySpots(this.boundingBox, this.floorY());
        for (int i = 0; i < order.size(); i++) {
            GallerySpot spot = spots.get(order.get(i));
            int x = spot.pos().getX() - this.boundingBox.getMinX();
            int z = spot.pos().getZ() - this.boundingBox.getMinZ();
            if (i >= GALLERY_MIMICS) {
                b.plainChest(x, 1, z, spot.facing(), this.loot("gallery"));
            }
        }
        int mx = w / 2;
        int mz = d / 2;
        for (int x = mx - 1; x <= mx + 1; x++) {
            for (int z = mz - 1; z <= mz + 1; z++) {
                b.setIfAir(x, 1, z, Blocks.RED_CARPET.getDefaultState());
            }
        }
        CryptDressing.cornerCobwebs(b, w, d, this.height, 0.8F);
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
