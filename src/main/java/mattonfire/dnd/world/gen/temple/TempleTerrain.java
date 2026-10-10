package mattonfire.dnd.world.gen.temple;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

/**
 * Terrain heights from the noise generator, before anything is built, cached per column, and the search for
 * a temple site: a stair foot with the mountain rising 6-14 blocks behind it to a slope gentle enough to cut
 * a terrace into.
 */
final class TempleTerrain {
    /** How far apart candidate stair feet are tried, round the middle of the chunk. */
    private static final int SITE_STEP = 8;
    /** The terrace may sit this much above or below the median ground under it, at most. */
    private static final int MAX_CUT = 12;
    private static final int MAX_FILL = 14;
    /** How far the ground under the terrace may rise above the stair foot (the climb), at least and at most. */
    private static final int MIN_CLIMB = 6;
    private static final int MAX_CLIMB = 18;

    private final ChunkGenerator generator;
    private final HeightLimitView world;
    private final NoiseConfig noise;
    private final BiomeSource biomes;
    private final Predicate<RegistryEntry<Biome>> biomeAllowed;
    private final Map<Long, int[]> cache = new HashMap<>();

    TempleTerrain(Structure.Context context) {
        this.generator = context.chunkGenerator();
        this.world = context.world();
        this.noise = context.noiseConfig();
        this.biomes = context.biomeSource();
        this.biomeAllowed = context.biomePredicate();
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

    boolean biomeAllowed(int x, int z) {
        RegistryEntry<Biome> biome = this.biomes.getBiome(BiomeCoords.fromBlock(x), BiomeCoords.fromBlock(this.ground(x, z)),
                BiomeCoords.fromBlock(z), this.noise.getMultiNoiseSampler());
        return this.biomeAllowed.test(biome);
    }

    /** The best site near the middle of the chunk, or empty. */
    Optional<DragonTemplePlanner.Site> findSite(int cx, int cz) {
        DragonTemplePlanner.Site best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int dx = -SITE_STEP; dx <= SITE_STEP; dx += SITE_STEP) {
            for (int dz = -SITE_STEP; dz <= SITE_STEP; dz += SITE_STEP) {
                int x = cx + dx;
                int z = cz + dz;
                if (this.water(x, z)) {
                    continue;
                }
                for (Direction in : Direction.Type.HORIZONTAL) {
                    DragonTemplePlanner.Site site = this.score(x, z, in);
                    if (site != null && site.score() < bestScore) {
                        bestScore = site.score();
                        best = site;
                    }
                }
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        BlockPos middle = best.at(0, 0, best.rise() + 1 + DragonTemplePlanner.DEPTH / 2);
        if (!this.biomeAllowed(best.foot().getX(), best.foot().getZ()) || !this.biomeAllowed(middle.getX(), middle.getZ())) {
            return Optional.empty();
        }
        return Optional.of(best);
    }

    /**
     * A site with its stair foot at x/z climbing {@code in}, or null if the slope won't do. The score is the
     * total cut and fill under the terrace (lower is better).
     */
    private DragonTemplePlanner.Site score(int x, int z, Direction in) {
        int y0 = this.ground(x, z);
        DragonTemplePlanner.Site probe = new DragonTemplePlanner.Site(new BlockPos(x, y0, z), in, DragonTemplePlanner.MAX_RISE, 0);
        // Open ground in front of the foot: the stair looks out over the valley, not into a gully.
        for (int d = 3; d <= 9; d += 3) {
            BlockPos p = probe.at(0, 0, -d);
            if (this.ground(p.getX(), p.getZ()) > y0 + 2 || this.water(p.getX(), p.getZ())) {
                return null;
            }
        }
        // The ground the terrace will stand on, over its whole footprint.
        int half = DragonTemplePlanner.HALF_WIDTH;
        int[] heights = new int[35];
        int n = 0;
        for (int sx = -half; sx <= half; sx += half / 3 + 1) {
            for (int sz = 0; sz < 5; sz++) {
                BlockPos p = probe.at(sx, 0, 10 + sz * 7);
                if (this.water(p.getX(), p.getZ())) {
                    return null;
                }
                if (n < heights.length) {
                    heights[n++] = this.ground(p.getX(), p.getZ());
                }
            }
        }
        int[] sorted = Arrays.copyOf(heights, n);
        Arrays.sort(sorted);
        int median = sorted[n / 2];
        int climb = median - y0;
        if (climb < MIN_CLIMB || climb > MAX_CLIMB) {
            return null;
        }
        int rise = Math.max(DragonTemplePlanner.MIN_RISE, Math.min(DragonTemplePlanner.MAX_RISE, climb));
        int floor = y0 + rise;
        int score = 0;
        for (int i = 0; i < n; i++) {
            int h = heights[i];
            if (h > floor + MAX_CUT || h < floor - MAX_FILL) {
                return null;
            }
            score += Math.abs(h - floor);
        }
        // The stair mustn't tunnel through a ridge.
        for (int sz = 2; sz <= rise; sz += 2) {
            BlockPos p = probe.at(0, 0, sz);
            if (this.ground(p.getX(), p.getZ()) > y0 + sz + 4) {
                return null;
            }
        }
        return new DragonTemplePlanner.Site(new BlockPos(x, y0, z), in, rise, score);
    }
}
