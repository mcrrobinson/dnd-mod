package mattonfire.dnd.world.gen.enclave;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.random.Random;

/**
 * Lays out an elven enclave: the Heart Tree (with the Speaker's Hall) in the middle, 3-5 talans on a ring
 * round it, all with their platforms at one height ({@link #DECK} above the Heart Tree's foot) so rope
 * bridges can run level between them: one from each talan to the Heart Tree, and one between neighbouring
 * talans when they're close enough and the bridge wouldn't pass the Heart Tree. Then the Moonwell next to
 * the Heart Tree, the archery glade and 1-2 flower gardens on the forest floor, on dry, gentle ground clear
 * of the trunks.
 */
final class ElvenEnclavePlanner {
    /** Height of the bridge deck above the Heart Tree's foot: every talan platform is at this height. */
    static final int DECK = 16;
    /** A talan's platform must be this high above its own ground, at least and at most. */
    private static final int MIN_PLATFORM = 10;
    private static final int MAX_PLATFORM = 22;
    private static final int TALAN_MIN_REACH = 19;
    /** Talan-to-talan bridges no longer than this (centre to centre). */
    private static final int MAX_LINK = 30;
    private static final int GAP = 2;

    private ElvenEnclavePlanner() {
    }

    private record Talan(int x, int z, int y) {
    }

    static void plan(StructurePiecesCollector collector, EnclaveTerrain terrain, Random random, int cx, int cy, int cz) {
        int deck = cy + DECK;
        HeartTreePiece heart = new HeartTreePiece(cx, cy, cz, random.nextLong());
        SpeakersHallPiece hall = new SpeakersHallPiece(cx, cy, cz, random.nextLong());

        // Ground the trunks take up, which the ground pieces keep off.
        List<BlockBox> taken = new ArrayList<>();
        int f = HeartTreePiece.FOOTPRINT;
        taken.add(new BlockBox(cx - f, cy, cz - f, cx + f, cy, cz + f));

        List<Talan> talans = new ArrayList<>();
        int count = 3 + random.nextInt(3);
        double base = random.nextDouble() * Math.PI * 2.0D;
        for (int i = 0; i < count; i++) {
            Talan talan = placeTalan(terrain, random, talans, cx, cz, deck, base + i * Math.PI * 2.0D / count);
            if (talan != null) {
                talans.add(talan);
                int t = TalanPiece.FOOTPRINT;
                taken.add(new BlockBox(talan.x - t, talan.y, talan.z - t, talan.x + t, talan.y, talan.z + t));
            }
        }

        List<EnclavePiece> trees = new ArrayList<>();
        trees.add(heart);
        List<EnclavePiece> bridges = new ArrayList<>();
        for (Talan talan : talans) {
            trees.add(new TalanPiece(talan.x, talan.y, talan.z, deck - talan.y, random.nextLong()));
            bridges.add(bridge(cx, cz, talan.x, talan.z, cx, cz, talans, deck, random));
        }
        // Neighbouring talans (in angle order round the Heart Tree).
        for (int i = 0; i < talans.size() && talans.size() > 2; i++) {
            Talan a = talans.get(i);
            Talan b = talans.get((i + 1) % talans.size());
            double length = Math.hypot(a.x - b.x, a.z - b.z);
            if (length <= MAX_LINK && distanceToSegment(cx, cz, a.x, a.z, b.x, b.z) > HeartTreePiece.REACH) {
                bridges.add(bridge(a.x, a.z, b.x, b.z, cx, cz, talans, deck, random));
            }
        }

        // The forest floor: the Moonwell close by the Heart Tree, then the glade and the gardens further out.
        List<EnclavePiece> ground = new ArrayList<>();
        EnclavePiece well = placeGround(terrain, random, taken, cx, cy, cz, MoonwellPiece.REACH, 14, 3, 2,
                (x, y, z) -> new MoonwellPiece(x, y, z, random.nextLong()));
        addIfPlaced(ground, taken, well);
        addIfPlaced(ground, taken, placeGround(terrain, random, taken, cx, cy, cz, ArcheryGladePiece.REACH, 15, 14, 3,
                (x, y, z) -> new ArcheryGladePiece(x, y, z, random.nextLong())));
        int gardens = 1 + random.nextInt(2);
        for (int i = 0; i < gardens; i++) {
            addIfPlaced(ground, taken, placeGround(terrain, random, taken, cx, cy, cz, EnclaveGardenPiece.REACH, 13, 16, 3,
                    (x, y, z) -> new EnclaveGardenPiece(x, y, z, random.nextLong())));
        }

        // The grounds first (they turn lava into water), then the trees, the hall and bridges over them,
        // and the ground pieces last so they're laid over any stray roots.
        List<BlockBox> all = new ArrayList<>();
        trees.forEach(piece -> all.add(piece.getBoundingBox()));
        ground.forEach(piece -> all.add(piece.getBoundingBox()));
        BlockBox area = BlockBox.encompass(all).orElseThrow().expand(4);
        collector.addPiece(new EnclaveGroundsPiece(area.getMinX(), area.getMinZ(), area.getMaxX(), area.getMaxZ(), cy));
        trees.forEach(collector::addPiece);
        collector.addPiece(hall);
        bridges.forEach(collector::addPiece);
        ground.forEach(collector::addPiece);
    }

    private static void addIfPlaced(List<EnclavePiece> ground, List<BlockBox> taken, EnclavePiece piece) {
        if (piece != null) {
            ground.add(piece);
            taken.add(piece.getBoundingBox());
        }
    }

    private static Talan placeTalan(EnclaveTerrain terrain, Random random, List<Talan> talans, int cx, int cz, int deck,
                                    double angle) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double a = angle + (random.nextDouble() - 0.5D) * 0.6D;
            int reach = TALAN_MIN_REACH + random.nextInt(6) + attempt;
            int x = cx + (int) Math.round(Math.cos(a) * reach);
            int z = cz + (int) Math.round(Math.sin(a) * reach);
            if (talans.stream().anyMatch(t -> Math.hypot(t.x - x, t.z - z) < 2 * TalanPiece.REACH + 2)) {
                continue;
            }
            int y = terrain.siteHeight(x, z, 2, 3);
            if (y == Integer.MIN_VALUE || deck - y < MIN_PLATFORM || deck - y > MAX_PLATFORM || !terrain.biomeAllowed(x, z)) {
                continue;
            }
            return new Talan(x, z, y);
        }
        return null;
    }

    private interface Factory {
        EnclavePiece create(int x, int y, int z);
    }

    /**
     * A ground piece of half-size {@code half} at a random spot between {@code minReach} and
     * {@code minReach + spread} from the middle, on dry ground no steeper than {@code maxSlope}, clear of
     * everything taken. Null if there's no room.
     */
    private static EnclavePiece placeGround(EnclaveTerrain terrain, Random random, List<BlockBox> taken, int cx, int cy, int cz,
                                            int half, int minReach, int spread, int maxSlope, Factory factory) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int reach = minReach + random.nextInt(spread) + attempt / 2;
            int x = cx + (int) Math.round(Math.cos(angle) * reach);
            int z = cz + (int) Math.round(Math.sin(angle) * reach);
            BlockBox box = new BlockBox(x - half, cy, z - half, x + half, cy, z + half);
            if (taken.stream().anyMatch(other -> overlaps(box, other, GAP))) {
                continue;
            }
            int y = terrain.siteHeight(x, z, half, maxSlope);
            if (y == Integer.MIN_VALUE || Math.abs(y - cy) > EnclaveTerrain.MAX_RISE) {
                continue;
            }
            return factory.create(x, y, z);
        }
        return null;
    }

    private static boolean overlaps(BlockBox a, BlockBox b, int gap) {
        return a.getMaxX() + gap >= b.getMinX() && a.getMinX() - gap <= b.getMaxX()
                && a.getMaxZ() + gap >= b.getMinZ() && a.getMinZ() - gap <= b.getMaxZ();
    }

    private static double distanceToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double t = ((px - ax) * dx + (pz - az) * dz) / (dx * dx + dz * dz);
        t = Math.max(0.0D, Math.min(1.0D, t));
        return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
    }

    /** Whether world x/z is on a platform: the Heart Tree's deck or a talan's. */
    private static boolean onPlatform(int x, int z, int cx, int cz, List<Talan> talans) {
        if (Math.max(Math.abs(x - cx), Math.abs(z - cz)) <= HeartTreePiece.DECK_OUT + 1
                && (HeartTreePiece.isDeck(x - cx, z - cz) || Math.max(Math.abs(x - cx), Math.abs(z - cz)) < HeartTreePiece.DECK_IN)) {
            return true;
        }
        for (Talan t : talans) {
            if (Math.max(Math.abs(x - t.x), Math.abs(z - t.z)) <= TalanPiece.EDGE) {
                return true;
            }
        }
        return false;
    }

    /**
     * A bridge along the line from one platform's middle to the other's: the cells off both platforms are
     * the walkway, and the platform cells next to them are where it lands.
     */
    private static BridgePiece bridge(int x0, int z0, int x1, int z1, int cx, int cz, List<Talan> talans, int deck, Random random) {
        List<int[]> line = new ArrayList<>();
        line4(line, x0, z0, x1, z1);
        List<Integer> cells = new ArrayList<>();
        List<Integer> landings = new ArrayList<>();
        for (int i = 0; i < line.size(); i++) {
            int[] c = line.get(i);
            if (!onPlatform(c[0], c[1], cx, cz, talans)) {
                cells.add(c[0]);
                cells.add(c[1]);
            } else {
                boolean next = i + 1 < line.size() && !onPlatform(line.get(i + 1)[0], line.get(i + 1)[1], cx, cz, talans);
                boolean prev = i > 0 && !onPlatform(line.get(i - 1)[0], line.get(i - 1)[1], cx, cz, talans);
                if (next || prev) {
                    landings.add(c[0]);
                    landings.add(c[1]);
                }
            }
        }
        return new BridgePiece(cells.stream().mapToInt(Integer::intValue).toArray(),
                landings.stream().mapToInt(Integer::intValue).toArray(), deck, random.nextLong());
    }

    /** A line from (x0, z0) to (x1, z1) whose cells always share a side (never only a corner). */
    private static void line4(List<int[]> out, int x0, int z0, int x1, int z1) {
        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int x = x0;
        int z = z0;
        out.add(new int[]{x, z});
        // Step along whichever axis keeps closest to the true line.
        int ix = 0;
        int iz = 0;
        while (ix < dx || iz < dz) {
            if ((0.5D + ix) / Math.max(dx, 1) < (0.5D + iz) / Math.max(dz, 1) && ix < dx || iz >= dz) {
                x += sx;
                ix++;
            } else {
                z += sz;
                iz++;
            }
            out.add(new int[]{x, z});
        }
    }
}
