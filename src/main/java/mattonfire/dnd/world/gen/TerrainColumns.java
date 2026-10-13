package mattonfire.dnd.world.gen;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.structure.Structure;

/**
 * Terrain samples for one structure site check, read from the noise generator (so before anything, trees
 * included, is built) and cached per column: a site check tests the same columns over and over.
 */
public final class TerrainColumns {
    private final Structure.Context context;
    private final Map<Long, Integer> ground = new HashMap<>();
    private final Map<Long, Boolean> water = new HashMap<>();

    public TerrainColumns(Structure.Context context) {
        this.context = context;
    }

    /** y of the top block (a water surface included). */
    public int ground(int x, int z) {
        return this.ground.computeIfAbsent(ChunkPos.toLong(x, z), key -> this.height(x, z, Heightmap.Type.WORLD_SURFACE_WG) - 1);
    }

    /** Whether the top of the column is water (or another liquid): the ocean floor lies below the surface. */
    public boolean water(int x, int z) {
        return this.water.computeIfAbsent(ChunkPos.toLong(x, z),
                key -> this.height(x, z, Heightmap.Type.OCEAN_FLOOR_WG) < this.ground(x, z) + 1);
    }

    /** Whether the biome at the top of the column is one the structure may stand in. */
    public boolean biomeAllowed(int x, int z) {
        return this.context.biomePredicate().test(this.context.biomeSource().getBiome(BiomeCoords.fromBlock(x),
                BiomeCoords.fromBlock(this.ground(x, z)), BiomeCoords.fromBlock(z),
                this.context.noiseConfig().getMultiNoiseSampler()));
    }

    private int height(int x, int z, Heightmap.Type type) {
        return this.context.chunkGenerator().getHeight(x, z, type, this.context.world(), this.context.noiseConfig());
    }
}
