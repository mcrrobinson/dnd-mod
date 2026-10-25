package mattonfire.dnd.classes.Commands;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/** What the {@code /dndhome} structure commands share: finding the nearest structure, and the acceptance surveys. */
final class HomeCommandUtil {
    /** How far (in chunks) the commands look for a structure. */
    static final int SEARCH_CHUNKS = 100;

    private HomeCommandUtil() {
    }

    /**
     * The start of the nearest structure tagged {@code tag} to {@code from} (within {@link #SEARCH_CHUNKS} chunks),
     * as the structure {@code key}, or null if there's none or it has no pieces.
     */
    @Nullable
    static StructureStart nearestStart(ServerWorld world, TagKey<Structure> tag, RegistryKey<Structure> key, BlockPos from) {
        BlockPos found = world.locateStructure(tag, from, SEARCH_CHUNKS, false);
        Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(key);
        if (found == null || structure == null) {
            return null;
        }
        StructureStart start = world.getChunk(found.getX() >> 4, found.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS)
                .getStructureStart(structure);
        return start != null && start.hasChildren() ? start : null;
    }

    /** One structure's survey result: whether it passes, and the line logged and sent for it. */
    record Check(boolean ok, String line) {
    }

    /**
     * An acceptance survey: finds the nearest structure ({@code find}) from each of a {@code grid} x {@code grid} of
     * points {@code spacing} blocks apart round the source, checks each one once ({@code key} tells them apart;
     * {@code check} returns null to leave one out), logs and sends a line for each and a summary
     * {@code "<tag> n <plural>, p pass, f fail"}. Returns the number that pass.
     */
    static <T> int survey(ServerCommandSource source, int grid, int spacing, String tag, String plural,
                          Function<BlockPos, T> find, ToLongFunction<T> key, Function<T, Check> check) {
        BlockPos center = BlockPos.ofFloored(source.getPosition());
        Set<Long> seen = new HashSet<>();
        int pass = 0;
        int fail = 0;
        int half = (grid - 1) / 2;
        for (int gx = -half; gx <= grid - 1 - half; gx++) {
            for (int gz = -half; gz <= grid - 1 - half; gz++) {
                T found = find.apply(center.add(gx * spacing, 0, gz * spacing));
                if (found == null || !seen.add(key.applyAsLong(found))) {
                    continue;
                }
                Check result = check.apply(found);
                if (result == null) {
                    continue;
                }
                if (result.ok()) {
                    pass++;
                } else {
                    fail++;
                }
                DnDClasses.LOGGER.info(result.line());
                source.sendFeedback(Text.literal(result.line()), false);
            }
        }
        String summary = tag + " " + (pass + fail) + " " + plural + ", " + pass + " pass, " + fail + " fail";
        DnDClasses.LOGGER.info(summary);
        source.sendFeedback(Text.literal(summary), false);
        return pass;
    }

    /**
     * For each seed, finds the start of {@code structure} nearest 0, 0 (within {@code radius} blocks) that would
     * generate in {@code world}'s dimension, without making a world: the structure set's grid slots, each asked for a
     * valid position with that seed's noise. Logs and sends {@code "<tag> seed S: nearest at x y z in <biome>, d blocks
     * from 0 0 (n slots tried): PASS|FAIL"}. Returns how many seeds had one. Shared by the Underdark checks.
     */
    static int seeds(ServerCommandSource source, ServerWorld world, RegistryKey<Structure> structureKey,
                     net.minecraft.util.Identifier setId, java.util.List<Long> seeds, int radius, String tag) {
        net.minecraft.world.gen.chunk.ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(structureKey);
        net.minecraft.structure.StructureSet set = world.getRegistryManager().get(RegistryKeys.STRUCTURE_SET).get(setId);
        if (!(generator instanceof net.minecraft.world.gen.chunk.NoiseChunkGenerator noise) || structure == null || set == null
                || !(set.placement() instanceof net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement spread)) {
            source.sendError(Text.literal(tag + " No noise generator or structure set " + setId));
            return 0;
        }
        if (seeds.isEmpty()) {
            seeds = java.util.List.of(world.getSeed(), 1L, 20261025L);
        }
        int found = 0;
        for (long seed : seeds) {
            net.minecraft.world.gen.noise.NoiseConfig config = net.minecraft.world.gen.noise.NoiseConfig.create(
                    noise.getSettings().value(), world.getRegistryManager().getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS), seed);
            int slots = radius / 16 / spread.getSpacing() + 1;
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            int tried = 0;
            for (int gx = -slots; gx <= slots; gx++) {
                for (int gz = -slots; gz <= slots; gz++) {
                    net.minecraft.util.math.ChunkPos chunk = spread.getStartChunk(seed, gx * spread.getSpacing(), gz * spread.getSpacing());
                    double d = Math.sqrt(chunk.getCenterX() * (double) chunk.getCenterX() + chunk.getCenterZ() * (double) chunk.getCenterZ());
                    if (d > radius || d >= bestDistance) {
                        continue;
                    }
                    tried++;
                    Structure.Context there = new Structure.Context(world.getRegistryManager(), generator, generator.getBiomeSource(),
                            config, world.getStructureTemplateManager(), seed, chunk, world, structure.getValidBiomes()::contains);
                    var position = structure.getValidStructurePosition(there);
                    if (position.isPresent()) {
                        best = position.get().position();
                        bestDistance = d;
                    }
                }
            }
            String biome = "-";
            if (best != null) {
                var entry = generator.getBiomeSource().getBiome(net.minecraft.world.biome.source.BiomeCoords.fromBlock(best.getX()),
                        net.minecraft.world.biome.source.BiomeCoords.fromBlock(best.getY()),
                        net.minecraft.world.biome.source.BiomeCoords.fromBlock(best.getZ()), config.getMultiNoiseSampler());
                biome = entry.getKey().map(key -> key.getValue().toString()).orElse("?");
            }
            String line = tag + " seed " + seed + ": " + (best == null ? "none within " + radius + " blocks"
                    : "nearest at " + best.getX() + " " + best.getY() + " " + best.getZ() + " in " + biome + ", "
                    + (int) bestDistance + " blocks from 0 0") + " (" + tried + " slots tried): " + (best == null ? "FAIL" : "PASS");
            DnDClasses.LOGGER.info(line);
            source.sendFeedback(Text.literal(line), false);
            if (best != null) {
                found++;
            }
        }
        return found;
    }
}
