package mattonfire.dnd.world.underdark.duergar;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.world.gen.OrientedPiece;
import mattonfire.dnd.world.underdark.UnderdarkSite;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Lays out a Duergar Hold (design 9, 8.2), about 58 blocks square round the site's centre. In the hold's own
 * coordinates (a across, b towards the gate, turned to a random {@code front}; the whole plan is mirrored left to
 * right half the time):
 * <ul>
 * <li>the {@link DuergarGroundsPiece grounds}: the levelled floor (half-size {@value #HALF}), the lava lake at the back
 * (a 20 x 12 basin with a magma-block shore), the yard's road from the gate to the lake with paths to every door, its
 * braziers, and the yard guards;</li>
 * <li>the {@link DuergarWallPiece wall}: 6 high round the land side, open over the lake, with the gatehouse in the
 * middle of the front;</li>
 * <li>the {@link DuergarForgePiece Forge Hall} at the back on one side, the {@link DuergarTowerPiece Taskmaster's
 * tower} at the back on the other by the lake, the {@link DuergarMinePiece mine} in front of the tower, the
 * {@link DuergarTradePiece Trade Hall} by the gate on the tower's side and 1-2 {@link DuergarBarracksPiece barracks}
 * opposite it, all facing the road. A second barracks joins the first by a bridge between their turrets.</li>
 * </ul>
 * The buildings share one {@link DuergarMasonry.Style} (roof material, ridge, trim) drawn per hold, and in a mirrored
 * hold every building is mirrored too ({@link DuergarPiece}), so their wings and turrets keep to the same side of the
 * road.
 */
public final class DuergarHoldPlanner {
    /** The levelled square's half-size. */
    public static final int HALF = 29;
    /** The inside of the wall: |a| up to this... */
    public static final int INNER = 24;
    /** ...b from the back wall to the front wall. */
    public static final int BACK = -14;
    public static final int FRONT = 24;
    /** The wall's two rows: the front at b 25-26, the sides at |a| 25-26, the back at b -16 to -15. */
    public static final int WALL_FRONT = 25;
    public static final int WALL_SIDE = 25;
    public static final int WALL_BACK = -16;
    /** The lava basin (inside the shore): a from -10 to 9, b from -27 to -16. */
    public static final int LAKE_A1 = -10;
    public static final int LAKE_A2 = 9;
    public static final int LAKE_B1 = -27;
    public static final int LAKE_B2 = -16;

    /** A building's spot in the hold: its centre and the way its door faces, unmirrored. */
    record Spot(int a, int b, Direction face, int depth) {
        /** Just outside its door. */
        int[] entrance(int m) {
            return new int[] {(this.a + this.face.getOffsetX() * this.depth) * m, this.b + this.face.getOffsetZ() * this.depth};
        }
    }

    static final Spot FORGE = new Spot(-15, -8, Direction.SOUTH, DuergarForgePiece.DOOR);
    static final Spot TOWER = new Spot(15, -10, Direction.SOUTH, DuergarTowerPiece.DOOR);
    static final Spot MINE = new Spot(14, 0, Direction.WEST, DuergarMinePiece.DOOR);
    static final Spot TRADE = new Spot(14, 13, Direction.WEST, DuergarTradePiece.DOOR);
    static final Spot[] BARRACKS = {new Spot(-15, 7, Direction.EAST, DuergarBarracksPiece.DOOR),
            new Spot(-15, 18, Direction.EAST, DuergarBarracksPiece.DOOR)};

    private DuergarHoldPlanner() {
    }

    static void plan(StructurePiecesCollector collector, Random random, UnderdarkSite.Site site) {
        BlockPos center = site.center();
        Direction front = Direction.Type.HORIZONTAL.random(random);
        int m = random.nextBoolean() ? 1 : -1;
        boolean mirror = m < 0;
        int barracks = 1 + random.nextInt(2);
        int style = DuergarMasonry.Style.random(random);

        List<Spot> spots = new ArrayList<>(List.of(FORGE, TOWER, MINE, TRADE));
        for (int i = 0; i < barracks; i++) {
            spots.add(BARRACKS[i]);
        }
        List<int[]> entrances = new ArrayList<>();
        for (Spot spot : spots) {
            entrances.add(spot.entrance(m));
        }
        collector.addPiece(new DuergarGroundsPiece(center, front, random.nextLong(), entrances));
        collector.addPiece(new DuergarForgePiece(pos(center, front, FORGE, m), face(front, FORGE, m), random.nextLong(),
                mirror, style));
        collector.addPiece(new DuergarTowerPiece(pos(center, front, TOWER, m), face(front, TOWER, m), random.nextLong(),
                mirror, style));
        collector.addPiece(new DuergarMinePiece(pos(center, front, MINE, m), face(front, MINE, m), random.nextLong(),
                mirror, style));
        collector.addPiece(new DuergarTradePiece(pos(center, front, TRADE, m), face(front, TRADE, m), random.nextLong(),
                mirror, style));
        // Two barracks put their turrets at the ends facing each other (towards +b for the first, -b for the
        // second: local -a and +a) and the second builds the bridge between them; a lone one's turret is either end.
        int lone = random.nextBoolean() ? 1 : -1;
        for (int i = 0; i < barracks; i++) {
            int turret = barracks == 1 ? lone : i == 0 ? -1 : 1;
            int bridge = barracks == 1 ? 0 : i == 0 ? 1 : 2;
            collector.addPiece(new DuergarBarracksPiece(pos(center, front, BARRACKS[i], m), face(front, BARRACKS[i], m),
                    random.nextLong(), mirror, style, turret, bridge));
        }
        collector.addPiece(new DuergarWallPiece(center, front, random.nextLong()));
    }

    /** A spot's centre in the world. */
    static BlockPos pos(BlockPos center, Direction front, Spot spot, int m) {
        return OrientedPiece.at(center, front, spot.a() * m, 0, spot.b());
    }

    /** The way a spot's door faces in the world. */
    static Direction face(Direction front, Spot spot, int m) {
        Direction local = spot.face();
        if (m < 0 && local.getAxis() == Direction.Axis.X) {
            local = local.getOpposite();
        }
        return OrientedPiece.rotation(front).rotate(local);
    }
}
