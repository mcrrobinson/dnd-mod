package mattonfire.dnd.world.underdark.drow;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.world.underdark.UnderdarkSite;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Lays out a Drow Outpost (design 9, 8.1), about 50 blocks across round the site's centre:
 * <ul>
 * <li>the {@link DrowShrinePiece spider shrine} in the middle, its back (the reliquary niche) towards the side farthest
 * from the pen and the barracks;</li>
 * <li>3-5 {@link DrowTowerPiece towers} on a ring {@value #TOWER_RING}-{@value #TOWER_RING_MAX} blocks out, evenly
 * spaced with a little jitter, doors facing the shrine, all with their platforms at the same height (the cavern's
 * headroom less 3, 10-13 blocks up) so the {@link DrowBridgePiece web bridges} between neighbours run level; 1-2 of
 * them are lookouts;</li>
 * <li>the {@link DrowPenPiece captive pen} {@value #PEN_RING} blocks out between the first two towers, and the
 * {@link DrowBarracksPiece barracks} {@value #BARRACKS_RING} out between the next two, both facing the shrine;</li>
 * <li>under it all the {@link DrowGroundsPiece grounds}: the levelled floor (radius {@value #GROUNDS_RADIUS}), paths
 * from the shrine to every door, and the cave spiders.</li>
 * </ul>
 * Pieces are added grounds first and bridges last: later pieces build over earlier ones.
 */
public final class DrowOutpostPlanner {
    public static final int TOWER_RING = 19;
    public static final int TOWER_RING_MAX = 20;
    public static final int PEN_RING = 12;
    public static final int BARRACKS_RING = 14;
    public static final int GROUNDS_RADIUS = 25;
    public static final int MIN_PLATFORM = 10;
    public static final int MAX_PLATFORM = 13;

    private DrowOutpostPlanner() {
    }

    static void plan(StructurePiecesCollector collector, Random random, UnderdarkSite.Site site) {
        BlockPos center = site.center();
        int platform = MathHelper.clamp(site.headroom() - 3, MIN_PLATFORM, MAX_PLATFORM);
        int towers = 3 + random.nextInt(3);
        double start = random.nextDouble() * Math.PI * 2.0D;
        double[] angles = new double[towers];
        BlockPos[] spots = new BlockPos[towers];
        for (int i = 0; i < towers; i++) {
            angles[i] = start + i * Math.PI * 2.0D / towers + (random.nextDouble() - 0.5D) * 0.3D;
            int ring = TOWER_RING + random.nextInt(TOWER_RING_MAX - TOWER_RING + 1);
            spots[i] = ring(center, angles[i], ring);
        }
        int lookouts = 1 + random.nextInt(2);
        int firstLookout = random.nextInt(towers);
        int mageTower = random.nextBoolean() ? random.nextInt(towers) : -1;

        double penAngle = mid(angles[0], angles[1]);
        double barracksAngle = mid(angles[1], angles[2]);
        BlockPos pen = ring(center, penAngle, PEN_RING);
        BlockPos barracks = ring(center, barracksAngle, BARRACKS_RING);
        Direction penFront = toward(pen, center);
        Direction barracksFront = toward(barracks, center);
        Direction shrineFront = shrineFront(penAngle, barracksAngle);

        // Where the paths from the shrine lead: just outside each door.
        List<BlockPos> entrances = new ArrayList<>();
        entrances.add(pen.offset(penFront, DrowPenPiece.DEPTH + 2));
        entrances.add(barracks.offset(barracksFront, DrowBarracksPiece.RADIUS + 2));
        Direction[] doors = new Direction[towers];
        for (int i = 0; i < towers; i++) {
            doors[i] = toward(spots[i], center);
            entrances.add(spots[i].offset(doors[i], DrowTowerPiece.DOOR_OUT));
        }

        collector.addPiece(new DrowGroundsPiece(center, random.nextLong(), entrances));
        collector.addPiece(new DrowPenPiece(pen, penFront, random.nextLong()));
        collector.addPiece(new DrowBarracksPiece(barracks, barracksFront, random.nextLong()));
        collector.addPiece(new DrowShrinePiece(center, shrineFront, random.nextLong()));
        for (int i = 0; i < towers; i++) {
            int tip = MathHelper.clamp(platform + 3 + random.nextInt(9), 14, 24);
            boolean lookout = Math.floorMod(i - firstLookout, towers) < lookouts;
            collector.addPiece(new DrowTowerPiece(spots[i], doors[i], random.nextLong(), platform, tip, lookout,
                    i == mageTower));
        }
        for (int i = 0; i < towers; i++) {
            collector.addPiece(new DrowBridgePiece(spots[i], spots[(i + 1) % towers], platform, random.nextLong()));
        }
    }

    /**
     * The shrine's front: the reliquary niche behind the idol reaches 9 blocks back, so its back points the way (of
     * the four) farthest from both the pen and the barracks.
     */
    static Direction shrineFront(double penAngle, double barracksAngle) {
        Direction best = Direction.SOUTH;
        double bestGap = -1.0D;
        for (Direction back : Direction.Type.HORIZONTAL) {
            double angle = Math.atan2(back.getOffsetZ(), back.getOffsetX());
            double gap = Math.min(apart(angle, penAngle), apart(angle, barracksAngle));
            if (gap > bestGap) {
                bestGap = gap;
                best = back.getOpposite();
            }
        }
        return best;
    }

    /** How far apart two angles are, in radians (0 to pi). */
    static double apart(double a, double b) {
        return Math.abs(Math.toRadians(MathHelper.wrapDegrees(Math.toDegrees(a - b))));
    }

    /** The spot {@code radius} blocks from {@code center} at {@code angle} (radians, 0 = east). */
    static BlockPos ring(BlockPos center, double angle, int radius) {
        return center.add((int) Math.round(Math.cos(angle) * radius), 0, (int) Math.round(Math.sin(angle) * radius));
    }

    /** The angle halfway from {@code a} to {@code b} going the short way round. */
    static double mid(double a, double b) {
        double d = MathHelper.wrapDegrees(Math.toDegrees(b - a));
        return a + Math.toRadians(d) / 2.0D;
    }

    /** The horizontal direction nearest the way from {@code from} to {@code to}. */
    static Direction toward(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (dx == 0 && dz == 0) {
            return Direction.SOUTH;
        }
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }
}
