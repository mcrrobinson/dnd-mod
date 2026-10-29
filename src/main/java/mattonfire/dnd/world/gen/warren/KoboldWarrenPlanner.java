package mattonfire.dnd.world.gen.warren;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.world.gen.OrientedPiece;
import mattonfire.dnd.world.gen.TerrainColumns;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * Lays out a kobold warren (design 10, 4.3), in the warren's local frame (front, out of the hill, towards +b; y = 0
 * the mouth floor):
 * <pre>
 *   mouth (b 1..-6) - tunnel (12-18 long, down 1 in 3) - den (13 x 9, its middle D)
 *   round D:  shrine (west), nursery (east), workshop (back west), larder (back east),
 *             chief's room (straight back, behind a covered pit), guard hall (great warrens, front east)
 *   bolt-holes: den - shrine, larder - chief, tunnel - the hillside
 * </pre>
 */
public final class KoboldWarrenPlanner {
    /** Where the mouth is cut, which way is into the hill, whether it's a great warren, and the slope (rise in 10). */
    public record Site(BlockPos mouth, Direction in, boolean great, int rise) {
    }

    public static final int TUNNEL_MIN = 12;
    public static final int TUNNEL_MAX = 18;
    /** The tunnel starts this far in from the mouth's front. */
    public static final int TUNNEL_START = 7;
    /** Longest the surface bolt-hole may run before it gives up on reaching daylight. */
    private static final int SURFACE_HOLE_MAX = 48;

    private KoboldWarrenPlanner() {
    }

    /** The den's middle for a tunnel of {@code length}, in the mouth's local frame (a, y, b). */
    static int[] denOffset(int length) {
        return new int[] {0, -((length - 1) / 3), -(TUNNEL_START + 4 + length)};
    }

    static void plan(StructurePiecesCollector collector, long seed, Site site, @Nullable TerrainColumns columns) {
        Random random = Random.create(seed);
        Direction front = site.in().getOpposite();
        int tier = site.great() ? 2 : 1;
        int length = TUNNEL_MIN + random.nextInt(TUNNEL_MAX - TUNNEL_MIN + 1);
        BlockPos mouth = site.mouth();
        int[] d = denOffset(length);
        BlockPos den = OrientedPiece.at(mouth, front, d[0], d[1], d[2]);

        collector.addPiece(new WarrenMouthPiece(mouth, front, random.nextLong(), tier, site.great()));
        collector.addPiece(new WarrenTunnelPiece(mouth, front, random.nextLong(), tier, length));
        // The den first: the rooms dig their corridors into its edge afterwards.
        collector.addPiece(new WarrenDenPiece(den, front, random.nextLong(), tier, false, site.great()));
        if (site.great()) {
            collector.addPiece(new WarrenDenPiece(OrientedPiece.at(den, front, WarrenDenPiece.GUARD_A, 0,
                    WarrenDenPiece.GUARD_B), front, random.nextLong(), tier, true, true));
        }
        collector.addPiece(new WarrenShrinePiece(den, front, random.nextLong(), tier));
        collector.addPiece(new WarrenNurseryPiece(den, front, random.nextLong(), tier));
        collector.addPiece(new WarrenWorkshopPiece(den, front, random.nextLong(), tier));
        collector.addPiece(new WarrenLarderPiece(den, front, random.nextLong(), tier));
        collector.addPiece(new WarrenChiefPiece(den, front, random.nextLong(), tier));

        // Bolt-holes last, so nothing walls them up.
        List<BlockPos> shrineHole = new ArrayList<>();
        for (int a = -5; a >= -10; a--) {
            shrineHole.add(OrientedPiece.at(den, front, a, 1, 3));
        }
        collector.addPiece(new BoltHolePiece(front, random.nextLong(), shrineHole, null));
        List<BlockPos> chiefHole = new ArrayList<>();
        for (int b = -11; b >= -16; b--) {
            chiefHole.add(OrientedPiece.at(den, front, 8, 1, b));
        }
        chiefHole.add(OrientedPiece.at(den, front, 7, 1, -16));
        chiefHole.add(OrientedPiece.at(den, front, 6, 1, -16));
        collector.addPiece(new BoltHolePiece(front, random.nextLong(), chiefHole, null));
        if (columns != null) {
            List<BlockPos> surface = new ArrayList<>();
            BlockPos exit = surfaceHole(columns, mouth, front, surface);
            if (!surface.isEmpty()) {
                collector.addPiece(new BoltHolePiece(front, random.nextLong(), surface, exit));
            }
        }
    }

    /**
     * The bolt-hole from the tunnel's side (3 blocks in) up to the hillside: west and out by turns, one up every
     * second block, until it comes out above the ground ({@code columns}). Returns the opening (where a bush goes),
     * or null if it never got there (then it ends blind).
     */
    @Nullable
    private static BlockPos surfaceHole(TerrainColumns columns, BlockPos mouth, Direction front, List<BlockPos> cells) {
        int a = -2;
        int y = 1;
        int b = -(TUNNEL_START + 2);
        for (int i = 0; i < SURFACE_HOLE_MAX; i++) {
            BlockPos cell = OrientedPiece.at(mouth, front, a, y, b);
            if (cell.getY() > columns.ground(cell.getX(), cell.getZ())) {
                return cell;
            }
            cells.add(cell);
            if (i % 2 == 1) {
                // A step up: headroom over this cell so a kobold can hop up
                cells.add(cell.up());
                y++;
            }
            if (i < 2 || i % 2 == 0) {
                a--;
            } else {
                b++;
            }
        }
        return null;
    }
}
