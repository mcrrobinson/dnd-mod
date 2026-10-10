package mattonfire.dnd.world.gen.enclave;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

/** Terrain heights from the noise generator, before anything (trees included) is built, cached per column. */
final class EnclaveTerrain {
    /** Rings of samples round the middle: radius 8, 16 and 24. */
    private static final int RING_STEP = 8;
    private static final int RINGS = 3;
    /** How far a sample may rise or fall from the middle. */
    static final int MAX_RISE = 6;
    /** How many of the 24 samples may be wet, too steep or out of the forest. */
    private static final int MAX_BAD = 4;

    private final ChunkGenerator generator;
    private final HeightLimitView world;
    private final NoiseConfig noise;
    private final BiomeSource biomes;
    private final Predicate<RegistryEntry<Biome>> biomeAllowed;
    private final Map<Long, int[]> cache = new HashMap<>();

    EnclaveTerrain(Structure.Context context) {
        this.generator = context.chunkGenerator();
        this.world = context.world();
        this.noise = context.noiseConfig();
        this.biomes = context.biomeSource();
        this.biomeAllowed = context.biomePredicate();
    }

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

    /** Dry, fairly level (within {@link #MAX_RISE}) and forested out to radius 24. */
    boolean isGoodSite(int x, int z) {
        if (this.water(x, z) || !this.biomeAllowed(x, z)) {
            return false;
        }
        int y = this.ground(x, z);
        int bad = 0;
        for (int ring = 1; ring <= RINGS; ring++) {
            int radius = ring * RING_STEP;
            for (int i = 0; i < 8; i++) {
                double angle = (i + ring * 0.5D) * Math.PI / 4.0D;
                int sx = x + (int) Math.round(Math.cos(angle) * radius);
                int sz = z + (int) Math.round(Math.sin(angle) * radius);
                if (this.water(sx, sz) || Math.abs(this.ground(sx, sz) - y) > MAX_RISE || !this.biomeAllowed(sx, sz)) {
                    if (++bad > MAX_BAD) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** The median ground height over a square of half-size {@code r} round x/z, or MIN_VALUE if wet or steeper than {@code maxSlope}. */
    int siteHeight(int x, int z, int r, int maxSlope) {
        int[] heights = new int[9];
        int i = 0;
        for (int dx = -r; dx <= r; dx += r) {
            for (int dz = -r; dz <= r; dz += r) {
                if (this.water(x + dx, z + dz)) {
                    return Integer.MIN_VALUE;
                }
                heights[i++] = this.ground(x + dx, z + dz);
            }
        }
        java.util.Arrays.sort(heights);
        return heights[8] - heights[0] > maxSlope ? Integer.MIN_VALUE : heights[4];
    }
}
