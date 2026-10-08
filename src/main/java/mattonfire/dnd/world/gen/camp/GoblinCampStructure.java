package mattonfire.dnd.world.gen.camp;

import com.mojang.serialization.Codec;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A goblin war camp: tents round a campfire inside a crude palisade, pitched on dry, fairly level
 * ground in forests and plains. Built by {@link GoblinCampPiece}.
 */
public class GoblinCampStructure extends Structure {
    public static final Codec<GoblinCampStructure> CODEC = createCodec(GoblinCampStructure::new);

    /** Rings of samples round the middle, and how far each may stray from the middle's height. */
    private static final int[] RINGS = {6, 10, 14};
    private static final int[] MAX_RISE = {2, 3, 5};
    /** How many samples may be wet, too steep or in the wrong biome. */
    private static final int MAX_BAD = 3;
    private static final Identifier HOBBIT_VILLAGES = new Identifier(DnDClasses.MOD_ID, "hobbit_villages");
    /** Chunks between a camp and a hobbit village's start (villages reach about 6 chunks out). */
    private static final int VILLAGE_CLEARANCE = 8;

    public GoblinCampStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        if (nearHobbitVillage(context)) {
            return Optional.empty();
        }
        if (water(context, x, z) || !this.biomeAllowed(context, x, z)) {
            return Optional.empty();
        }
        int y = ground(context, x, z);
        int bad = 0;
        for (int ring = 0; ring < RINGS.length; ring++) {
            for (int i = 0; i < 8; i++) {
                double angle = (i + ring * 0.5D) * Math.PI / 4.0D;
                int sx = x + (int) Math.round(Math.cos(angle) * RINGS[ring]);
                int sz = z + (int) Math.round(Math.sin(angle) * RINGS[ring]);
                if (water(context, sx, sz) || Math.abs(ground(context, sx, sz) - y) > MAX_RISE[ring]
                        || !this.biomeAllowed(context, sx, sz)) {
                    bad++;
                }
            }
        }
        if (bad > MAX_BAD) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(x, y, z);
        return Optional.of(new StructurePosition(center,
                collector -> collector.addPiece(new GoblinCampPiece(center, context.random().nextLong()))));
    }

    /**
     * Whether a hobbit village may start within {@link #VILLAGE_CLEARANCE} chunks. The structure set
     * can only exclude one other set (vanilla villages), so this keeps camps from carving through
     * smials: it asks the village set's spread placement which chunks it would start in.
     */
    private static boolean nearHobbitVillage(Context context) {
        StructureSet villages = context.dynamicRegistryManager().get(RegistryKeys.STRUCTURE_SET).get(HOBBIT_VILLAGES);
        if (villages == null || !(villages.placement() instanceof RandomSpreadStructurePlacement spread)) {
            return false;
        }
        ChunkPos chunk = context.chunkPos();
        for (int dx = -VILLAGE_CLEARANCE; dx <= VILLAGE_CLEARANCE; dx++) {
            for (int dz = -VILLAGE_CLEARANCE; dz <= VILLAGE_CLEARANCE; dz++) {
                int cx = chunk.x + dx;
                int cz = chunk.z + dz;
                ChunkPos start = spread.getStartChunk(context.seed(), cx, cz);
                if (start.x == cx && start.z == cz) {
                    return true;
                }
            }
        }
        return false;
    }

    /** y of the top block, from the noise generator before anything is built. */
    private static int ground(Context context, int x, int z) {
        return context.chunkGenerator().getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig()) - 1;
    }

    private static boolean water(Context context, int x, int z) {
        return context.chunkGenerator().getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig())
                < ground(context, x, z) + 1;
    }

    private boolean biomeAllowed(Context context, int x, int z) {
        return context.biomePredicate().test(context.biomeSource().getBiome(BiomeCoords.fromBlock(x),
                BiomeCoords.fromBlock(ground(context, x, z)), BiomeCoords.fromBlock(z), context.noiseConfig().getMultiNoiseSampler()));
    }

    @Override
    public StructureType<?> getType() {
        return GoblinCampStructures.GOBLIN_CAMP;
    }
}
