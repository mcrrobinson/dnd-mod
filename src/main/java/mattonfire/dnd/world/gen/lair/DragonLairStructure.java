package mattonfire.dnd.world.gen.lair;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
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
 * A Lightning Chaser's lair: a ring of standing stones crowned with lightning rods around a nest
 * and its hoard, levelled into the very top of a mountain peak. The Frost Drake's frost lair is the
 * same structure in ice and snow ({@link LairPiece.Kind#FROST}), registered as its own type.
 */
public class DragonLairStructure extends Structure {
    public static final Codec<DragonLairStructure> CODEC = createCodec(config -> new DragonLairStructure(config, LairPiece.Kind.STORM));
    public static final Codec<DragonLairStructure> FROST_CODEC = createCodec(config -> new DragonLairStructure(config, LairPiece.Kind.FROST));

    /** How far from the middle of the chunk the summit is looked for, and the step between samples. */
    private static final int SEARCH = 16;
    private static final int STEP = 4;
    /** Nothing within this distance of the summit may stand higher: it has to be the top. */
    private static final int[] RINGS = {12, 20, 28};
    /** How far the mountain has to fall away, on average, at the outer ring: a peak, not a plateau. */
    private static final int MIN_DROP = 14;
    /** The lair floor is cut this far below the very tip of the peak. */
    private static final int CUT = 4;
    /**
     * A frost lair keeps this many chunks clear of these structures. Unlike a structure set's
     * exclusion_zone (one set only, and it counts every grid slot, which would rule out nearly every
     * site), this only counts ones that really generate there.
     */
    private static final int CLEARANCE = 6;
    private static final List<RegistryKey<StructureSet>> KEEP_CLEAR_OF = List.of(
            RegistryKey.of(RegistryKeys.STRUCTURE_SET, new Identifier(DnDClasses.MOD_ID, "dragon_lairs")),
            RegistryKey.of(RegistryKeys.STRUCTURE_SET, new Identifier(DnDClasses.MOD_ID, "dwarven_fortresses")));

    private final LairPiece.Kind kind;

    public DragonLairStructure(Config config, LairPiece.Kind kind) {
        super(config);
        this.kind = kind;
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        // Each column's surface is read from the noise generator once per site check
        Map<Long, Integer> heights = new HashMap<>();
        if (!this.biomeAllowed(context, heights, chunk.getCenterX(), chunk.getCenterZ())) {
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
                int y = ground(context, heights, x, z);
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
                int y = ground(context, heights, bestX + (int) Math.round(Math.cos(angle) * radius), bestZ + (int) Math.round(Math.sin(angle) * radius));
                if (y > top) {
                    return Optional.empty();
                }
                if (radius == RINGS[RINGS.length - 1]) {
                    drop += top - y;
                    outer++;
                }
            }
        }
        if (drop < MIN_DROP * outer || !this.biomeAllowed(context, heights, bestX, bestZ)) {
            return Optional.empty();
        }
        if (this.kind == LairPiece.Kind.FROST && nearOtherStructure(context)) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(bestX, top - CUT, bestZ);
        return Optional.of(new StructurePosition(center,
                collector -> collector.addPiece(new LairPiece(center, context.random().nextLong(), this.kind))));
    }

    /** Whether a dragon lair or dwarven fortress starts within {@link #CLEARANCE} chunks of this one. */
    private static boolean nearOtherStructure(Context context) {
        Registry<StructureSet> sets = context.dynamicRegistryManager().get(RegistryKeys.STRUCTURE_SET);
        ChunkPos chunk = context.chunkPos();
        for (RegistryKey<StructureSet> key : KEEP_CLEAR_OF) {
            StructureSet set = sets.get(key);
            if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement placement)) {
                continue;
            }
            for (int x = chunk.x - CLEARANCE; x <= chunk.x + CLEARANCE; x++) {
                for (int z = chunk.z - CLEARANCE; z <= chunk.z + CLEARANCE; z++) {
                    ChunkPos start = placement.getStartChunk(context.seed(), x, z);
                    if (start.x != x || start.z != z) {
                        continue;
                    }
                    for (StructureSet.WeightedEntry entry : set.structures()) {
                        Structure other = entry.structure().value();
                        Context there = new Context(context.dynamicRegistryManager(), context.chunkGenerator(),
                                context.biomeSource(), context.noiseConfig(), context.structureTemplateManager(),
                                context.seed(), start, context.world(), other.getValidBiomes()::contains);
                        if (other.getValidStructurePosition(there).isPresent()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** y of the top block, from the noise generator before anything is built. */
    private static int ground(Context context, Map<Long, Integer> heights, int x, int z) {
        return heights.computeIfAbsent(ChunkPos.toLong(x, z), key -> context.chunkGenerator()
                .getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig()) - 1);
    }

    private boolean biomeAllowed(Context context, Map<Long, Integer> heights, int x, int z) {
        return context.biomePredicate().test(context.biomeSource().getBiome(BiomeCoords.fromBlock(x),
                BiomeCoords.fromBlock(ground(context, heights, x, z)), BiomeCoords.fromBlock(z), context.noiseConfig().getMultiNoiseSampler()));
    }

    @Override
    public StructureType<?> getType() {
        return this.kind == LairPiece.Kind.FROST ? DragonLairStructures.FROST_LAIR : DragonLairStructures.DRAGON_LAIR;
    }
}
