package mattonfire.dnd.world.gen.lair;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A Lightning Chaser's lair: a ring of standing stones crowned with lightning rods around a nest
 * and its hoard, levelled into the very top of a mountain peak.
 */
public class DragonLairStructure extends Structure {
    public static final Codec<DragonLairStructure> CODEC = createCodec(DragonLairStructure::new);

    /** How far from the middle of the chunk the summit is looked for, and the step between samples. */
    private static final int SEARCH = 16;
    private static final int STEP = 4;
    /** Nothing within this distance of the summit may stand higher: it has to be the top. */
    private static final int[] RINGS = {12, 20, 28};
    /** How far the mountain has to fall away, on average, at the outer ring: a peak, not a plateau. */
    private static final int MIN_DROP = 14;
    /** The lair floor is cut this far below the very tip of the peak. */
    private static final int CUT = 4;

    public DragonLairStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        if (!this.biomeAllowed(context, chunk.getCenterX(), chunk.getCenterZ())) {
            return Optional.empty();
        }
        // The highest point near the middle of the chunk...
        int bestX = 0;
        int bestZ = 0;
        int top = Integer.MIN_VALUE;
        for (int dx = -SEARCH; dx <= SEARCH; dx += STEP) {
            for (int dz = -SEARCH; dz <= SEARCH; dz += STEP) {
                int x = chunk.getCenterX() + dx;
                int z = chunk.getCenterZ() + dz;
                int y = ground(context, x, z);
                if (y > top) {
                    top = y;
                    bestX = x;
                    bestZ = z;
                }
            }
        }
        // ...has to be the summit of a real peak, with the mountain falling away all round it.
        int drop = 0;
        int outer = 0;
        for (int radius : RINGS) {
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * i / 4.0D;
                int y = ground(context, bestX + (int) Math.round(Math.cos(angle) * radius), bestZ + (int) Math.round(Math.sin(angle) * radius));
                if (y > top) {
                    return Optional.empty();
                }
                if (radius == RINGS[RINGS.length - 1]) {
                    drop += top - y;
                    outer++;
                }
            }
        }
        if (drop < MIN_DROP * outer || !this.biomeAllowed(context, bestX, bestZ)) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(bestX, top - CUT, bestZ);
        return Optional.of(new StructurePosition(center,
                collector -> collector.addPiece(new LairPiece(center, context.random().nextLong()))));
    }

    /** y of the top block, from the noise generator before anything is built. */
    private static int ground(Context context, int x, int z) {
        return context.chunkGenerator().getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig()) - 1;
    }

    private boolean biomeAllowed(Context context, int x, int z) {
        return context.biomePredicate().test(context.biomeSource().getBiome(BiomeCoords.fromBlock(x),
                BiomeCoords.fromBlock(ground(context, x, z)), BiomeCoords.fromBlock(z), context.noiseConfig().getMultiNoiseSampler()));
    }

    @Override
    public StructureType<?> getType() {
        return DragonLairStructures.DRAGON_LAIR;
    }
}
