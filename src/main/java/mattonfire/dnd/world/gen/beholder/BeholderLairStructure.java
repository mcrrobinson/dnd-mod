package mattonfire.dnd.world.gen.beholder;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A Beholder's lair: a great domed cavern deep in the deepslate (see {@link BeholderCavernPiece}),
 * reached by a spiral stair from the surface ({@link BeholderShaftPiece}).
 */
public class BeholderLairStructure extends Structure {
    public static final Codec<BeholderLairStructure> CODEC = createCodec(BeholderLairStructure::new);

    /** The cavern floor is somewhere in this range of y. */
    private static final int MIN_FLOOR = -38;
    private static final int FLOOR_RANGE = 20;

    public BeholderLairStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        int floor = MIN_FLOOR + context.random().nextInt(FLOOR_RANGE);
        if (floor - 4 <= context.world().getBottomY()) {
            return Optional.empty();
        }
        // Not under the deep dark (ancient cities), and the cavern's biome has to be allowed.
        RegistryEntry<Biome> biome = context.biomeSource().getBiome(BiomeCoords.fromBlock(x), BiomeCoords.fromBlock(floor + 6),
                BiomeCoords.fromBlock(z), context.noiseConfig().getMultiNoiseSampler());
        if (!context.biomePredicate().test(biome) || biome.matchesKey(BiomeKeys.DEEP_DARK)) {
            return Optional.empty();
        }
        // The stair comes up on dry land.
        int shaftX = x + BeholderShaftPiece.OFFSET;
        int surface = context.chunkGenerator().getHeight(shaftX, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig()) - 1;
        int seabed = context.chunkGenerator().getHeight(shaftX, z, Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig()) - 1;
        if (surface != seabed || surface < context.chunkGenerator().getSeaLevel() || surface - floor < 20) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(x, floor, z);
        long seed = context.random().nextLong();
        return Optional.of(new StructurePosition(center, collector -> {
            collector.addPiece(new BeholderCavernPiece(center, seed));
            collector.addPiece(new BeholderShaftPiece(new BlockPos(shaftX, floor, z), surface, seed));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return BeholderLairStructures.BEHOLDER_LAIR;
    }
}
