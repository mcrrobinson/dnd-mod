package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

/**
 * The boss's hall, two cells by two: a tall pillared chamber with a raised dais in the middle,
 * where the boss appears when the party walks in. In the Crypt, a reliquary off one wall holds the
 * Lich's phylactery ({@link ReliquaryPiece}). In the Goblin Warren it's the Warlord's war hall. In
 * the Dwarven Ruin it's a <b>dome</b> ({@link #DOME_RADIUS}, up to the full height in the middle) cut
 * out of the solid rock, with short passages in from its doorways: the Beholder's lair.
 */
public class BossRoomPiece extends DungeonPiece {
    public BossRoomPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.BOSS_ROOM, info, box, seed, roomId, role, height, doors);
    }

    public BossRoomPiece(NbtCompound nbt) {
        super(DungeonStructures.BOSS_ROOM, nbt);
    }

    /** The Ruin's dome: its radius, and how high its ceiling is at the rim (the middle is the room's full height). */
    public static final int DOME_RADIUS = 14;
    private static final int DOME_RIM = 4;

    @Override
    protected void build(Builder b) {
        if (this.theme().deep()) {
            this.dome(b);
            return;
        }
        this.room(b);
        int w = this.width();
        int d = this.depth();
        int mx = w / 2;
        int mz = d / 2;
        // Two rings of columns, leaving the lines through the doorways clear
        for (int x = 5; x < w - 4; x += 5) {
            for (int z = 5; z < d - 4; z += 5) {
                boolean ring = x == 5 || z == 5 || x >= w - 6 || z >= d - 6;
                boolean nearMiddle = Math.abs(x - mx) <= 3 || Math.abs(z - mz) <= 3;
                if (ring && !nearMiddle) {
                    b.column(x, z, this.height);
                }
            }
        }
        // The dais
        b.fill(mx - 3, 1, mz - 3, mx + 3, 1, mz + 3, b.palette.trim());
        // The Lich's dais is topped with its sigil
        b.fill(mx - 1, 2, mz - 1, mx + 1, 2, mz + 1, CryptDressing.applies(this)
                ? mattonfire.dnd.classes.Registry.CryptBlocks.LICH_SIGIL_FLOOR.getDefaultState() : b.palette.accent());
        for (int[] o : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
            b.light(mx + o[0], this.height, mz + o[1], true);
        }
        for (int[] o : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}, {0, -9}, {0, 9}, {-9, 0}, {9, 0}}) {
            b.spawnPoint(mx + o[0], 1, mz + o[1]);
        }
        if (CryptDressing.applies(this)) {
            this.tomb(b, w, d, mx, mz);
        } else if (WarrenDressing.applies(this)) {
            this.warHall(b, w, d, mx, mz);
        }
    }

    /**
     * The Lich's tomb: sarcophagi along the middle of each wall (the doorways are a cell's middle,
     * well to either side) with a Funerary Relief over the gap between each pair, grave candle clusters on
     * the dais corners, bones in the corners, grave moss.
     */
    private void tomb(Builder b, int w, int d, int mx, int mz) {
        CryptDressing.wallSarcophagi(b, w, d, 11, 0.75F);
        CryptDressing.wallReliefs(b, w, d, 3, new int[]{0}, 1.0F);
        for (int[] o : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            CryptDressing.candleCluster(b, mx + o[0], 2, mz + o[1]);
        }
        CryptDressing.mossPatches(b, w, d, 5);
        for (int[] c : CryptDressing.corners(w, d)) {
            CryptDressing.bonePile(b, c[0], c[1]);
        }
        for (int[] o : new int[][]{{-6, -5}, {6, -5}, {-6, 5}, {6, 5}, {-5, -6}, {5, -6}, {-5, 6}, {5, 6}}) {
            if (b.dressing.nextFloat() < 0.4F) {
                CryptDressing.skull(b, mx + o[0], 1, mz + o[1]);
            }
        }
        CryptDressing.cornerCobwebs(b, w, d, this.height, 0.9F);
    }

    /**
     * The Goblin Warlord's war hall: cages and hide lean-tos along the middle of each wall (between
     * the doorways), skull totems at the dais corners, four fire pits under the hanging lanterns,
     * bones and refuse pits.
     */
    private void warHall(Builder b, int w, int d, int mx, int mz) {
        WarrenDressing.wallProps(b, w, d, 11, 0.8F, true);
        for (int[] o : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            WarrenDressing.totem(b, mx + o[0], mz + o[1]);
        }
        for (int[] o : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
            WarrenDressing.firePit(b, mx + o[0], mz + o[1]);
        }
        for (int[] c : CryptDressing.corners(w, d)) {
            CryptDressing.bonePile(b, c[0], c[1]);
        }
        for (int[] o : new int[][]{{-12, -4}, {12, 3}, {-3, 12}, {4, -12}}) {
            if (b.dressing.nextFloat() < 0.6F) {
                WarrenDressing.refusePit(b, mx + o[0], mz + o[1]);
            }
        }
        CryptDressing.cornerCobwebs(b, w, d, this.height, 0.7F);
    }

    /**
     * The inner ceiling height (the top air block) of the dome at {@code r} blocks from its middle:
     * the room's full height in the middle, curving down to {@value #DOME_RIM} at {@link #DOME_RADIUS}.
     */
    int domeCeiling(double r) {
        double t = Math.min(1.0D, r / (DOME_RADIUS + 1.0D));
        return Math.max(DOME_RIM, Math.min(this.height, (int) Math.round(this.height * Math.sqrt(1.0D - t * t))));
    }

    /** The middle of the Ruin's dome, where the Beholder is at home: a few blocks over the dais. */
    public static net.minecraft.util.math.BlockPos domeCentre(net.minecraft.util.math.BlockBox box, int floorY) {
        return new net.minecraft.util.math.BlockPos(box.getMinX() + box.getBlockCountX() / 2, floorY + 5,
                box.getMinZ() + box.getBlockCountZ() / 2);
    }

    /**
     * The Dwarven Ruin's boss room, the Beholder's dome: the room's shell, then solid wall everywhere
     * outside a dome of radius {@link #DOME_RADIUS} (the corners, and over the curve of the roof),
     * except 3-wide, 4-high passages from each doorway in to the dome. A dais in the middle, lanterns
     * hanging from the curve, a ring of eight dwarven pillars (some broken), headless statues round the
     * dais and rubble.
     */
    private void dome(Builder b) {
        this.room(b);
        int w = this.width();
        int d = this.depth();
        int mx = w / 2;
        int mz = d / 2;
        for (int x = 2; x < w - 2; x++) {
            for (int z = 2; z < d - 2; z++) {
                double r = Math.sqrt((x - mx) * (x - mx) + (z - mz) * (z - mz));
                int ceiling;
                if (r <= DOME_RADIUS + 0.5D) {
                    ceiling = this.domeCeiling(r);
                } else if (this.inPassage(x, z, w, d)) {
                    ceiling = DOOR_HEIGHT + 1;
                } else {
                    ceiling = 0;
                }
                for (int y = ceiling + 1; y <= this.height; y++) {
                    b.set(x, y, z, y == 1 && ceiling == 0 ? b.palette.trim() : b.palette.wall(b.random));
                }
            }
        }
        // The dais
        b.fill(mx - 3, 1, mz - 3, mx + 3, 1, mz + 3, b.palette.trim());
        b.fill(mx - 1, 2, mz - 1, mx + 1, 2, mz + 1, b.palette.accent());
        for (int[] o : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            b.light(mx + o[0], this.domeCeiling(Math.sqrt(72)), mz + o[1], true);
        }
        for (int[] o : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}, {0, -9}, {0, 9}, {-9, 0}, {9, 0}}) {
            b.spawnPoint(mx + o[0], 1, mz + o[1]);
        }
        // Eight pillars round the dome, between the lines in from the doorways
        int[][] pillars = new int[8][];
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI / 8.0D + i * Math.PI / 4.0D;
            int x = mx + (int) Math.round(Math.cos(angle) * 11.0D);
            int z = mz + (int) Math.round(Math.sin(angle) * 11.0D);
            pillars[i] = new int[]{x, z};
            b.column(x, z, this.domeCeiling(11.0D));
        }
        if (RuinDressing.applies(this)) {
            for (int[] p : pillars) {
                if (b.dressing.nextFloat() < 0.4F) {
                    RuinDressing.brokenColumn(b, p[0], p[1], this.domeCeiling(11.0D));
                }
            }
            for (int[] o : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
                RuinDressing.statue(b, mx + o[0], mz + o[1]);
            }
            for (int i = 0; i < 10; i++) {
                double angle = b.dressing.nextDouble() * Math.PI * 2.0D;
                double r = 12.0D + b.dressing.nextDouble() * 1.5D;
                RuinDressing.rubble(b, mx + (int) Math.round(Math.cos(angle) * r), mz + (int) Math.round(Math.sin(angle) * r));
            }
            CryptDressing.cornerCobwebs(b, w, d, DOOR_HEIGHT + 1, 0.5F);
        }
    }

    /** Whether (x, z) is in the 3-wide passage from one of this room's doorways in to the dome. */
    private boolean inPassage(int x, int z, int w, int d) {
        for (Door door : this.doors) {
            boolean alongX = door.side().getAxis() == Direction.Axis.Z;
            int centre = alongX ? door.along() - this.boundingBox.getMinX() : door.along() - this.boundingBox.getMinZ();
            int lateral = alongX ? x : z;
            if (Math.abs(lateral - centre) > DOOR_WIDTH / 2) {
                continue;
            }
            boolean onSide = switch (door.side()) {
                case NORTH -> z < d / 2;
                case SOUTH -> z > d / 2;
                case WEST -> x < w / 2;
                default -> x > w / 2;
            };
            if (onSide) {
                return true;
            }
        }
        return false;
    }
}
