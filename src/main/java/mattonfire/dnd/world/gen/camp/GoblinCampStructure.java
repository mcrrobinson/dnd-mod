package mattonfire.dnd.world.gen.camp;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.StructureProximity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeCoords;
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
    /**
     * Chunks between a camp and a hobbit village's start (villages reach about 6 chunks out), so camps
     * don't carve through smials.
     */
    private static final int VILLAGE_CLEARANCE = 8;

    public GoblinCampStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        if (StructureProximity.near(context, HOBBIT_VILLAGES, VILLAGE_CLEARANCE)) {
            return Optional.empty();
        }
        Columns columns = new Columns(context);
        if (columns.water(x, z) || !columns.biomeAllowed(x, z)) {
            return Optional.empty();
        }
        int y = columns.ground(x, z);
        int bad = 0;
        for (int ring = 0; ring < RINGS.length; ring++) {
            for (int i = 0; i < 8; i++) {
                double angle = (i + ring * 0.5D) * Math.PI / 4.0D;
                int sx = x + (int) Math.round(Math.cos(angle) * RINGS[ring]);
                int sz = z + (int) Math.round(Math.sin(angle) * RINGS[ring]);
                if (columns.water(sx, sz) || Math.abs(columns.ground(sx, sz) - y) > MAX_RISE[ring]
                        || !columns.biomeAllowed(sx, sz)) {
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
     * Terrain samples for one site check: each column's surface (and, if asked, ocean floor) is read
     * from the noise generator once, not again for every test that needs it.
     */
    private static final class Columns {
        private final Context context;
        private final Map<Long, Integer> ground = new HashMap<>();

        Columns(Context context) {
            this.context = context;
        }

        /** y of the top block, from the noise generator before anything is built. */
        int ground(int x, int z) {
            return this.ground.computeIfAbsent(ChunkPos.toLong(x, z), key -> this.context.chunkGenerator()
                    .getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, this.context.world(), this.context.noiseConfig()) - 1);
        }

        boolean water(int x, int z) {
            return this.context.chunkGenerator().getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, this.context.world(),
                    this.context.noiseConfig()) < this.ground(x, z) + 1;
        }

        boolean biomeAllowed(int x, int z) {
            return this.context.biomePredicate().test(this.context.biomeSource().getBiome(BiomeCoords.fromBlock(x),
                    BiomeCoords.fromBlock(this.ground(x, z)), BiomeCoords.fromBlock(z),
                    this.context.noiseConfig().getMultiNoiseSampler()));
        }
    }

    @Override
    public StructureType<?> getType() {
        return GoblinCampStructures.GOBLIN_CAMP;
    }
}
