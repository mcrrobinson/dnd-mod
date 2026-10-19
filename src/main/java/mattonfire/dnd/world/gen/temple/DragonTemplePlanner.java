package mattonfire.dnd.world.gen.temple;

import mattonfire.dnd.classes.Race.DragonAncestry;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Lays out a Dragon Temple from its stair foot, climbing into the mountain:
 * <pre>
 *            back (into the mountain)
 *   +-------+---------------------+-------+
 *   |       |       SANCTUM       |library|
 *   | yard  +---------------------+-------+
 *   |       |  F      E      S    | cells |
 *   |       |      COURTYARD      |       |
 *   +-------+--------+   +--------+-------+
 *                    |   |  grand stair
 *                     foot
 * </pre>
 * The terrace is {@link #WIDTH} wide and {@link #DEPTH} deep, {@code rise} (8-12) blocks above the foot.
 */
public final class DragonTemplePlanner {
    public static final int MIN_RISE = 8;
    public static final int MAX_RISE = 12;
    public static final int WIDTH = 39;
    public static final int HALF_WIDTH = WIDTH / 2;
    public static final int DEPTH = 30;
    static final int SIDE_WIDTH = 9;
    static final int COURT_WIDTH = 21;
    static final int COURT_DEPTH = 15;
    static final int SANCTUM_DEPTH = DEPTH - COURT_DEPTH;

    /**
     * Where a temple goes: its stair foot (on the ground), the direction it climbs, how high the terrace is
     * above the foot, and how much cut and fill it needs (for picking the best site).
     */
    public record Site(BlockPos foot, Direction facing, int rise, int score) {
        /** The world position {@code sx} to the right of the centre line, {@code sy} up and {@code sz} in from the foot. */
        public BlockPos at(int sx, int sy, int sz) {
            Direction right = TemplePiece.lateral(this.facing);
            return this.foot.add(right.getOffsetX() * sx + this.facing.getOffsetX() * sz, sy,
                    right.getOffsetZ() * sx + this.facing.getOffsetZ() * sz);
        }
    }

    private DragonTemplePlanner() {
    }

    static void plan(StructurePiecesCollector collector, Random random, Site site) {
        Direction in = site.facing();
        int rise = site.rise();
        int terraceZ = rise + 1;
        int side = HALF_WIDTH - SIDE_WIDTH / 2;

        // The ground first, so the rest builds on the reshaped slope.
        collector.addPiece(new SlopePiece(site.at(0, rise - SlopePiece.DOWN, terraceZ - SlopePiece.FRONT_MARGIN), in, random.nextLong()));
        collector.addPiece(new TerracePiece(site.at(0, rise, terraceZ), in, random.nextLong()));
        collector.addPiece(new GrandStairPiece(site.at(0, 0, 0), in, rise, random.nextLong()));
        collector.addPiece(new CourtyardPiece(site.at(0, rise, terraceZ), in, random.nextLong()));
        for (DragonAncestry kind : new DragonAncestry[]{DragonAncestry.FROST, DragonAncestry.EMBER, DragonAncestry.STORM}) {
            int[] spot = CourtyardPiece.statueSpot(kind);
            collector.addPiece(new StatuePiece(site.at(spot[0], rise + 1, terraceZ + spot[1]), in, kind, random.nextLong()));
        }
        collector.addPiece(new SanctumPiece(site.at(0, rise, terraceZ + COURT_DEPTH), in, random.nextLong()));
        collector.addPiece(new CloisterPiece(site.at(side, rise, terraceZ), in, random.nextLong()));
        collector.addPiece(new TrainingYardPiece(site.at(-side, rise, terraceZ), in, random.nextLong()));
    }
}
