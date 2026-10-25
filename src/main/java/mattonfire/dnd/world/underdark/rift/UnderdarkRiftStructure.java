package mattonfire.dnd.world.underdark.rift;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import mattonfire.dnd.world.gen.StructureProximity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * An Underdark Rift in the Overworld (design 9, 2.6): a jagged chasm deep in the deepslate with a pool of Rift Veil
 * at one end ({@link RiftChasmPiece}), and from its other end a ragged sinkhole up to the surface with ledges
 * spiralling round its walls ({@link RiftSinkholePiece}).
 * <p>
 * The chasm runs through the start chunk's centre, lengthways along a random axis; the sinkhole stands
 * {@link #SHAFT_OFFSET} blocks along it. It needs dry land at the sinkhole, no deep dark (ancient cities) at the
 * chasm, and keeps {@link #CLEARANCE} chunks from Beholder lairs and racial homes.
 */
public class UnderdarkRiftStructure extends Structure {
    public static final Codec<UnderdarkRiftStructure> CODEC = createCodec(UnderdarkRiftStructure::new);

    /** The chasm's floor (the veil pool's level); you stand a block above it. */
    public static final int FLOOR = -58;
    /** The chasm's ceiling, before its jagged rises of up to {@link #CEILING_JAG} blocks. */
    public static final int CEILING = -30;
    public static final int CEILING_JAG = 2;
    /** How far along the chasm (from its centre, away from the pool) the sinkhole's axis is. */
    public static final int SHAFT_OFFSET = 7;
    /** Chunks kept clear of Beholder lairs and racial homes. */
    public static final int CLEARANCE = 6;
    /** The sinkhole has to come up at least this high, so there's rock between it and the chasm's roof. */
    private static final int MIN_SURFACE = CEILING + 12;

    /** Structure sets a rift keeps {@link #CLEARANCE} chunks from. */
    private static final List<Identifier> AVOID = List.of(id("beholder_lairs"), id("hobbit_villages"),
            id("dwarven_fortresses"), id("elven_enclaves"), id("dragon_temples"), id("orc_strongholds"), id("cogsdeeps"));

    public UnderdarkRiftStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        if (FLOOR - 3 <= context.world().getBottomY()) {
            return Optional.empty();
        }
        boolean alongX = context.random().nextBoolean();
        int sign = context.random().nextBoolean() ? 1 : -1;
        int ux = alongX ? sign : 0;
        int uz = alongX ? 0 : sign;
        int shaftX = x + ux * SHAFT_OFFSET;
        int shaftZ = z + uz * SHAFT_OFFSET;
        // Dry land at the sinkhole (the Beholder lair's rule).
        int surface = context.chunkGenerator().getHeight(shaftX, shaftZ, Heightmap.Type.WORLD_SURFACE_WG, context.world(),
                context.noiseConfig()) - 1;
        int seabed = context.chunkGenerator().getHeight(shaftX, shaftZ, Heightmap.Type.OCEAN_FLOOR_WG, context.world(),
                context.noiseConfig()) - 1;
        if (surface != seabed || surface < context.chunkGenerator().getSeaLevel() || surface < MIN_SURFACE) {
            return Optional.empty();
        }
        // Not through an ancient city.
        RegistryEntry<Biome> deep = context.biomeSource().getBiome(BiomeCoords.fromBlock(x), BiomeCoords.fromBlock(FLOOR + 8),
                BiomeCoords.fromBlock(z), context.noiseConfig().getMultiNoiseSampler());
        if (deep.matchesKey(BiomeKeys.DEEP_DARK)) {
            return Optional.empty();
        }
        // The surface biome is checked against the structure's tag by vanilla, at the position returned below.
        RegistryEntry<Biome> top = context.biomeSource().getBiome(BiomeCoords.fromBlock(shaftX), BiomeCoords.fromBlock(surface),
                BiomeCoords.fromBlock(shaftZ), context.noiseConfig().getMultiNoiseSampler());
        if (!context.biomePredicate().test(top)) {
            return Optional.empty();
        }
        for (Identifier set : AVOID) {
            if (StructureProximity.nearGenerating(context, set, CLEARANCE)) {
                return Optional.empty();
            }
        }
        BlockPos center = new BlockPos(x, FLOOR, z);
        BlockPos shaft = new BlockPos(shaftX, FLOOR, shaftZ);
        long seed = context.random().nextLong();
        return Optional.of(new StructurePosition(new BlockPos(shaftX, surface, shaftZ), collector -> {
            collector.addPiece(new RiftChasmPiece(center, ux, uz, seed));
            collector.addPiece(new RiftSinkholePiece(shaft, surface, ux, uz, seed));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return UnderdarkRiftStructures.UNDERDARK_RIFT;
    }

    private static Identifier id(String path) {
        return UnderdarkRiftStructures.id(path);
    }
}
