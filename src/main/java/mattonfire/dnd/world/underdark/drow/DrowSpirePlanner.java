package mattonfire.dnd.world.underdark.drow;

import mattonfire.dnd.world.underdark.UnderdarkSite;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Lays out a Drow Spire (design 9, 8.3) round the site's centre, its front (the guard hall's door) a random way:
 * <ul>
 * <li>the {@link DrowSpireCavernPiece cavern}: a dome of radius {@value DrowSpireCavernPiece#RADIUS} and height
 * {@value DrowSpireCavernPiece#HEIGHT} with a 2-block shell, its floor levelled, a path out of the door and the spider
 * pit beside the Spire;</li>
 * <li>the {@link DrowSpirePiece Spire} in the middle;</li>
 * <li>3 minor {@link DrowTowerPiece towers} {@value #TOWER_RING} blocks out, at 60 degrees either side of the front and
 * straight behind, their doors facing the Spire, platforms at {@value DrowSpirePiece#CELLS} (the cells' floor) and
 * tips 20-28 high; the first is a lookout;</li>
 * <li>a {@link DrowBridgePiece web bridge} from each tower's platform into the cells.</li>
 * </ul>
 * Pieces are added cavern first and bridges last: later pieces build over earlier ones.
 */
public final class DrowSpirePlanner {
    public static final int TOWER_RING = 18;
    public static final int MIN_TIP = 20;
    public static final int MAX_TIP = 28;
    /** The towers in the Spire's local a/b (front +b): 60 degrees either side of the front, and straight behind. */
    static final int[][] TOWERS = {{16, 9}, {-16, 9}, {0, -TOWER_RING}};

    private DrowSpirePlanner() {
    }

    static void plan(StructurePiecesCollector collector, Random random, UnderdarkSite.Site site) {
        BlockPos center = site.center();
        Direction front = Direction.Type.HORIZONTAL.random(random);
        collector.addPiece(new DrowSpireCavernPiece(center, front, random.nextLong()));
        DrowSpirePiece spire = new DrowSpirePiece(center, front, random.nextLong());
        collector.addPiece(spire);
        BlockPos[] spots = new BlockPos[TOWERS.length];
        for (int i = 0; i < TOWERS.length; i++) {
            spots[i] = spire.at(TOWERS[i][0], 0, TOWERS[i][1]);
            int tip = MIN_TIP + random.nextInt(MAX_TIP - MIN_TIP + 1);
            collector.addPiece(new DrowTowerPiece(spots[i], DrowOutpostPlanner.toward(spots[i], center), random.nextLong(),
                    DrowSpirePiece.CELLS, tip, i == 0, false));
        }
        for (BlockPos spot : spots) {
            collector.addPiece(new DrowBridgePiece(spot, center, DrowSpirePiece.CELLS, random.nextLong()));
        }
    }
}
