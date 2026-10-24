package mattonfire.dnd.world.underdark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * {@code /underdark survey <radius>}: samples the Underdark's noise (before surface rules and features, so nothing
 * has to generate) on a grid of columns 4 blocks apart round a point, and reports
 * <ul>
 * <li>each biome's share of the columns,</li>
 * <li>the open floors (a solid block under air or water): how many per column, their median y, and the share under
 * water (the lakes),</li>
 * <li>the bedrock in the real chunk at the centre: the highest floor bedrock and the lowest roof bedrock,</li>
 * <li>the cost, in milliseconds per sampled column,</li>
 * <li>the climate noises' mean and spread (temperature and humidity), for tuning the biome points.</li>
 * </ul>
 */
public final class UnderdarkSurvey {
    public static final int STEP = 4;

    public record Result(Map<RegistryKey<Biome>, Integer> biomes, int columns, int floors, int wetFloors,
            int medianFloorY, int bedrockFloorTop, int bedrockRoofBottom, double msPerColumn, double[] climate) {

        public double share(RegistryKey<Biome> biome) {
            return this.columns == 0 ? 0 : this.biomes.getOrDefault(biome, 0) / (double) this.columns;
        }

        public double wetShare() {
            return this.floors == 0 ? 0 : this.wetFloors / (double) this.floors;
        }

        /** One line for the log and chat. */
        public String describe() {
            StringBuilder line = new StringBuilder();
            line.append(String.format("columns=%d", this.columns));
            for (RegistryKey<Biome> biome : Underdark.BIOMES) {
                line.append(String.format(" %s=%.1f%%", biome.getValue().getPath(), 100 * share(biome)));
            }
            line.append(String.format(" floorsPerColumn=%.2f medianFloorY=%d water=%.1f%% bedrockFloorTop=%d"
                    + " bedrockRoofBottom=%d msPerColumn=%.3f", this.columns == 0 ? 0 : this.floors / (double) this.columns,
                    this.medianFloorY, 100 * wetShare(), this.bedrockFloorTop, this.bedrockRoofBottom,
                    this.msPerColumn));
            line.append(String.format(" temperature=%.3f+-%.3f humidity=%.3f+-%.3f", this.climate[0], this.climate[1],
                    this.climate[2], this.climate[3]));
            return line.toString();
        }
    }

    private UnderdarkSurvey() {
    }

    public static Result run(ServerWorld world, int centreX, int centreZ, int radius) {
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        NoiseConfig noise = world.getChunkManager().getNoiseConfig();
        Map<RegistryKey<Biome>, Integer> biomes = new LinkedHashMap<>();
        List<Integer> floorYs = new ArrayList<>();
        int columns = 0;
        int wet = 0;
        double[] sums = new double[4];
        long start = System.nanoTime();
        for (int dx = -radius; dx <= radius; dx += STEP) {
            for (int dz = -radius; dz <= radius; dz += STEP) {
                int x = centreX + dx;
                int z = centreZ + dz;
                columns++;
                RegistryEntry<Biome> biome = generator.getBiomeSource().getBiome(BiomeCoords.fromBlock(x),
                        BiomeCoords.fromBlock(-160), BiomeCoords.fromBlock(z), noise.getMultiNoiseSampler());
                biome.getKey().ifPresent(key -> biomes.merge(key, 1, Integer::sum));
                MultiNoiseUtil.NoiseValuePoint point = noise.getMultiNoiseSampler().sample(BiomeCoords.fromBlock(x),
                        BiomeCoords.fromBlock(-160), BiomeCoords.fromBlock(z));
                double t = MultiNoiseUtil.toFloat(point.temperatureNoise());
                double h = MultiNoiseUtil.toFloat(point.humidityNoise());
                sums[0] += t;
                sums[1] += t * t;
                sums[2] += h;
                sums[3] += h * h;
                VerticalBlockSample column = generator.getColumnSample(x, z, world, noise);
                BlockState above = column.getState(Underdark.TOP_Y);
                for (int y = Underdark.TOP_Y - 1; y >= Underdark.BOTTOM_Y; y--) {
                    BlockState here = column.getState(y);
                    boolean solid = !here.isAir() && here.getFluidState().isEmpty();
                    boolean open = above.isAir() || !above.getFluidState().isEmpty();
                    if (solid && open) {
                        floorYs.add(y);
                        if (!above.getFluidState().isEmpty()) {
                            wet++;
                        }
                    }
                    above = here;
                }
            }
        }
        double ms = (System.nanoTime() - start) / 1e6 / Math.max(1, columns);
        Collections.sort(floorYs);
        int median = floorYs.isEmpty() ? 0 : floorYs.get(floorYs.size() / 2);
        int[] bedrock = bedrock(world, centreX, centreZ);
        double n = Math.max(1, columns);
        double[] climate = { sums[0] / n, Math.sqrt(Math.max(0, sums[1] / n - Math.pow(sums[0] / n, 2))),
                sums[2] / n, Math.sqrt(Math.max(0, sums[3] / n - Math.pow(sums[2] / n, 2))) };
        return new Result(biomes, columns, floorYs.size(), wet, median, bedrock[0], bedrock[1], ms, climate);
    }

    /** The highest floor bedrock and the lowest roof bedrock over the generated chunk at the point. */
    private static int[] bedrock(ServerWorld world, int x, int z) {
        Chunk chunk = world.getChunk(x >> 4, z >> 4);
        int floorTop = Underdark.BOTTOM_Y - 1;
        int roofBottom = Underdark.TOP_Y + 1;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int cx = 0; cx < 16; cx++) {
            for (int cz = 0; cz < 16; cz++) {
                int bx = chunk.getPos().getStartX() + cx;
                int bz = chunk.getPos().getStartZ() + cz;
                for (int y = Underdark.BOTTOM_Y; y < Underdark.BOTTOM_Y + 16; y++) {
                    if (chunk.getBlockState(pos.set(bx, y, bz)).isOf(Blocks.BEDROCK)) {
                        floorTop = Math.max(floorTop, y);
                    }
                }
                for (int y = Underdark.TOP_Y; y > Underdark.TOP_Y - 16; y--) {
                    if (chunk.getBlockState(pos.set(bx, y, bz)).isOf(Blocks.BEDROCK)) {
                        roofBottom = Math.min(roofBottom, y);
                    }
                }
            }
        }
        return new int[] { floorTop, roofBottom };
    }
}
