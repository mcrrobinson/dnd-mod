package mattonfire.dnd.world.gen.village;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

/**
 * Lays out a hobbit village: the green in the middle, then each building or plot dropped at a
 * random spot around it (facing the green, on dry, gentle enough ground, not touching anything
 * else), then a winding lane from each one's entrance to the ring path round the green.
 */
final class HobbitVillagePlanner {
    private static final int GREEN = VillageGreenPiece.RADIUS;
    /** Minimum gap between plots. */
    private static final int GAP = 3;
    /** Lane cells per path piece. */
    private static final int SEGMENT = 10;

    private interface Factory {
        HobbitPiece create(Random random, int x, int y, int z, Direction facing);
    }

    private enum Kind {
        INN(InnPiece::create, 8, 4),
        SMIAL(SmialPiece::create, 15, 7),
        GARDEN(GardenPiece::create, 7, 4),
        ORCHARD(OrchardPiece::create, 7, 4),
        MARKET(MarketStallPiece::create, 5, 3),
        POND(PondPiece::create, 6, 3);

        final Factory factory;
        /** Rough half-size, to start looking far enough out from the green. */
        final int radius;
        /** Most the ground may rise and fall across the plot. */
        final int maxSlope;

        Kind(Factory factory, int radius, int maxSlope) {
            this.factory = factory;
            this.radius = radius;
            this.maxSlope = maxSlope;
        }
    }

    private HobbitVillagePlanner() {
    }

    static void plan(StructurePiecesCollector collector, Terrain terrain, Random random, int cx, int cy, int cz) {
        // Piece boxes start one above the ground (see HobbitPiece#applyYTransform).
        VillageGreenPiece green = new VillageGreenPiece(random, cx, cy + 1, cz);
        List<BlockBox> taken = new ArrayList<>();
        taken.add(green.getBoundingBox());

        List<HobbitPiece> plots = new ArrayList<>();
        for (Kind kind : wishList(random)) {
            HobbitPiece piece = place(kind, terrain, random, taken, cx, cy, cz);
            if (piece != null) {
                taken.add(piece.getBoundingBox());
                plots.add(piece);
            }
        }

        // The grounds go first (they turn lava lakes into ponds), then the lanes, so the buildings
        // are laid over any stray bits of them.
        BlockBox area = BlockBox.encompass(taken).orElseThrow().expand(6);
        collector.addPiece(new VillageGroundsPiece(area.getMinX(), area.getMinZ(), area.getMaxX(), area.getMaxZ(), cy));
        for (HobbitPiece plot : plots) {
            lane(collector, terrain, random, plot.entrance(), taken, cx, cz);
        }
        collector.addPiece(green);
        plots.forEach(collector::addPiece);
    }

    private static List<Kind> wishList(Random random) {
        List<Kind> kinds = new ArrayList<>();
        // Every village has its inn (the tavern with the innkeeper and the bounty board).
        kinds.add(Kind.INN);
        List<Kind> rest = new ArrayList<>();
        int smials = 4 + random.nextInt(4);
        for (int i = 0; i < smials; i++) {
            rest.add(Kind.SMIAL);
        }
        rest.add(Kind.GARDEN);
        if (random.nextBoolean()) {
            rest.add(Kind.GARDEN);
        }
        if (random.nextFloat() < 0.7F) {
            rest.add(Kind.ORCHARD);
        }
        rest.add(Kind.MARKET);
        if (random.nextBoolean()) {
            rest.add(Kind.MARKET);
        }
        if (random.nextFloat() < 0.6F) {
            rest.add(Kind.POND);
        }
        for (int i = rest.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Kind k = rest.get(i);
            rest.set(i, rest.get(j));
            rest.set(j, k);
        }
        kinds.addAll(rest);
        return kinds;
    }

    private static HobbitPiece place(Kind kind, Terrain terrain, Random random, List<BlockBox> taken, int cx, int cy, int cz) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int reach = GREEN + kind.radius + GAP + random.nextInt(10) + attempt * 2;
            int x = cx + (int) Math.round(Math.cos(angle) * reach);
            int z = cz + (int) Math.round(Math.sin(angle) * reach);
            Direction towardGreen = Math.abs(cx - x) > Math.abs(cz - z)
                    ? (cx > x ? Direction.EAST : Direction.WEST)
                    : (cz > z ? Direction.SOUTH : Direction.NORTH);
            // A piece facing north has its front on its south side, so face away from the green.
            HobbitPiece piece = kind.factory.create(random, x, cy, z, towardGreen.getOpposite());
            BlockBox box = piece.getBoundingBox();
            if (taken.stream().anyMatch(other -> overlaps(box, other, GAP))) {
                continue;
            }
            int y = siteHeight(terrain, box, kind.maxSlope);
            if (y == Integer.MIN_VALUE || Math.abs(y - cy) > 10) {
                continue;
            }
            piece.translate(0, y + 1 - box.getMinY(), 0);
            return piece;
        }
        return null;
    }

    private static boolean overlaps(BlockBox a, BlockBox b, int gap) {
        return a.getMaxX() + gap >= b.getMinX() && a.getMinX() - gap <= b.getMaxX()
                && a.getMaxZ() + gap >= b.getMinZ() && a.getMinZ() - gap <= b.getMaxZ();
    }

    /** The median ground height over a plot, or MIN_VALUE if it's wet or too steep. */
    private static int siteHeight(Terrain terrain, BlockBox box, int maxSlope) {
        int[] xs = {box.getMinX(), box.getCenter().getX(), box.getMaxX()};
        int[] zs = {box.getMinZ(), box.getCenter().getZ(), box.getMaxZ()};
        int[] heights = new int[9];
        int i = 0;
        for (int x : xs) {
            for (int z : zs) {
                if (terrain.water(x, z)) {
                    return Integer.MIN_VALUE;
                }
                heights[i++] = terrain.ground(x, z);
            }
        }
        Arrays.sort(heights);
        return heights[8] - heights[0] > maxSlope ? Integer.MIN_VALUE : heights[4];
    }

    /** A lane from a plot's entrance to the ring path, bent once along the way, with lamp posts. */
    private static void lane(StructurePiecesCollector collector, Terrain terrain, Random random, BlockPos from,
                             List<BlockBox> taken, int cx, int cz) {
        double dx = from.getX() - cx;
        double dz = from.getZ() - cz;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < GREEN) {
            return;
        }
        int tx = cx + (int) Math.round(dx / length * (GREEN - 1));
        int tz = cz + (int) Math.round(dz / length * (GREEN - 1));
        // Bend: push the midpoint sideways a little.
        int bend = random.nextInt(7) - 3;
        int mx = (from.getX() + tx) / 2 + (int) Math.round(-dz / length * bend);
        int mz = (from.getZ() + tz) / 2 + (int) Math.round(dx / length * bend);
        List<int[]> line = new ArrayList<>();
        line(line, from.getX(), from.getZ(), mx, mz);
        line(line, mx, mz, tx, tz);

        boolean alongX = Math.abs(dx) > Math.abs(dz);
        List<Integer> cells = new ArrayList<>();
        List<Integer> lamps = new ArrayList<>();
        int count = 0;
        int lampSide = 1;
        for (int[] c : line) {
            float widen = random.nextFloat();
            if (blocked(c[0], c[1], taken, cx, cz)) {
                continue;
            }
            cells.add(c[0]);
            cells.add(c[1]);
            int sx = alongX ? 0 : 1;
            int sz = alongX ? 1 : 0;
            if (widen < 0.5F && !blocked(c[0] + sx, c[1] + sz, taken, cx, cz)) {
                cells.add(c[0] + sx);
                cells.add(c[1] + sz);
            }
            if (count % 9 == 4) {
                int lx = c[0] + sx * 2 * lampSide;
                int lz = c[1] + sz * 2 * lampSide;
                if (!blocked(lx, lz, taken, cx, cz)) {
                    lamps.add(lx);
                    lamps.add(lz);
                }
                lampSide = -lampSide;
            }
            count++;
            if (count % SEGMENT == 0) {
                flush(collector, terrain, random, cells, lamps);
            }
        }
        flush(collector, terrain, random, cells, lamps);
    }

    private static boolean blocked(int x, int z, List<BlockBox> taken, int cx, int cz) {
        double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
        if (d < GREEN - 1.5D) {
            return true;
        }
        for (int i = 1; i < taken.size(); i++) {
            BlockBox box = taken.get(i);
            if (x >= box.getMinX() && x <= box.getMaxX() && z >= box.getMinZ() && z <= box.getMaxZ()) {
                return true;
            }
        }
        return false;
    }

    private static void flush(StructurePiecesCollector collector, Terrain terrain, Random random,
                              List<Integer> cells, List<Integer> lamps) {
        if (cells.isEmpty()) {
            lamps.clear();
            return;
        }
        int mid = (cells.size() / 4) * 2;
        int y = terrain.ground(cells.get(mid), cells.get(mid + 1));
        collector.addPiece(new PathPiece(cells.stream().mapToInt(Integer::intValue).toArray(),
                lamps.stream().mapToInt(Integer::intValue).toArray(), y, random.nextLong()));
        cells.clear();
        lamps.clear();
    }

    /** Bresenham line from (x0, z0) to (x1, z1), both ends included. */
    private static void line(List<int[]> out, int x0, int z0, int x1, int z1) {
        int dx = Math.abs(x1 - x0);
        int dz = -Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int err = dx + dz;
        while (true) {
            out.add(new int[]{x0, z0});
            if (x0 == x1 && z0 == z1) {
                return;
            }
            int e2 = 2 * err;
            if (e2 >= dz) {
                err += dz;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                z0 += sz;
            }
        }
    }

    /** Terrain heights from the noise generator, before anything is built, cached per column. */
    static final class Terrain {
        private final ChunkGenerator generator;
        private final HeightLimitView world;
        private final NoiseConfig noise;
        private final BiomeSource biomes;
        private final Predicate<RegistryEntry<Biome>> biomeAllowed;
        private final Map<Long, int[]> cache = new HashMap<>();

        Terrain(Structure.Context context) {
            this.generator = context.chunkGenerator();
            this.world = context.world();
            this.noise = context.noiseConfig();
            this.biomes = context.biomeSource();
            this.biomeAllowed = context.biomePredicate();
        }

        /** Whether the village's biomes include the one at the surface here. */
        boolean biomeAllowed(int x, int z) {
            RegistryEntry<Biome> biome = this.biomes.getBiome(BiomeCoords.fromBlock(x), BiomeCoords.fromBlock(this.ground(x, z)),
                    BiomeCoords.fromBlock(z), this.noise.getMultiNoiseSampler());
            return this.biomeAllowed.test(biome);
        }

        private int[] sample(int x, int z) {
            return this.cache.computeIfAbsent(ChunkPos.toLong(x, z), key -> {
                int surface = this.generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, this.world, this.noise);
                int floor = this.generator.getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, this.world, this.noise);
                return new int[]{surface - 1, floor < surface ? 1 : 0};
            });
        }

        /** y of the top block (water surface included). */
        int ground(int x, int z) {
            return this.sample(x, z)[0];
        }

        boolean water(int x, int z) {
            return this.sample(x, z)[1] == 1;
        }

        /** Dry, fairly level and in the right sort of country across the middle of the village. */
        boolean isGoodSite(int x, int z) {
            if (this.water(x, z) || !this.biomeAllowed(x, z)) {
                return false;
            }
            int y = this.ground(x, z);
            int good = 0;
            for (int radius = 12; radius <= 36; radius += 12) {
                for (int i = 0; i < 8; i++) {
                    double angle = (i + radius / 24.0D) * Math.PI / 4.0D;
                    int sx = x + (int) Math.round(Math.cos(angle) * radius);
                    int sz = z + (int) Math.round(Math.sin(angle) * radius);
                    if (!this.water(sx, sz) && Math.abs(this.ground(sx, sz) - y) <= radius / 3 + 2 && this.biomeAllowed(sx, sz)) {
                        good++;
                    }
                }
            }
            return good >= 19;
        }
    }
}
