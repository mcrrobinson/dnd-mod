package mattonfire.dnd.world.gen.fortress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

/**
 * Lays out a dwarven fortress. The gate goes at the foot of a mountainside, facing out, and
 * everything else runs back into the rock along one axis: gate, great hall, throne room and the
 * treasury behind the throne. The hall has doorways down both sides, each leading through a
 * short corridor to a room picked at random.
 */
final class FortressPlanner {
    /** Rooms that can open off the great hall. */
    enum Room {
        FORGE(ForgePiece::create),
        BARRACKS(BarracksPiece::create),
        BREWHALL(BrewhallPiece::create),
        MINE(MinePiece::create);

        final Factory factory;

        Room(Factory factory) {
            this.factory = factory;
        }
    }

    interface Factory {
        FortressPiece create(Random random, BlockPos front, Direction facing);
    }

    record Site(BlockPos pos, Direction facing) {
    }

    private FortressPlanner() {
    }

    static void plan(StructurePiecesCollector collector, Random random, Site site) {
        Direction in = site.facing();
        GatePiece gate = GatePiece.create(random, site.pos(), in);
        HallPiece hall = HallPiece.create(random, gate.backCenter(), in);
        ThroneRoomPiece throne = ThroneRoomPiece.create(random, hall.backCenter(), in);
        // The treasury door is up on the throne dais, so its floor sits that much higher.
        TreasuryPiece treasury = TreasuryPiece.create(random, throne.backCenter().up(ThroneRoomPiece.DAIS_HEIGHT), in);

        List<FortressPiece> pieces = new ArrayList<>(List.of(hall, throne, treasury));
        List<HallPiece.Exit> exits = hall.exits();
        List<Room> rooms = roomList(random, exits.size());
        for (int i = 0; i < exits.size(); i++) {
            HallPiece.Exit exit = exits.get(i);
            CorridorPiece corridor = CorridorPiece.create(random, exit.front(), exit.facing());
            pieces.add(corridor);
            pieces.add(rooms.get(i).factory.create(random, corridor.backCenter(), exit.facing()));
        }

        collector.addPiece(gate);
        pieces.forEach(collector::addPiece);
        // Last, so it heaps rock over whatever the mountain doesn't already cover. The gate is
        // only banked up to its battlements, and nothing goes in front of its face.
        List<BlockBox> covered = new ArrayList<>(pieces.stream().map(FortressPiece::getBoundingBox).toList());
        BlockBox g = gate.getBoundingBox();
        covered.add(new BlockBox(g.getMinX(), g.getMinY(), g.getMinZ(), g.getMaxX(), g.getMaxY() - CladdingPiece.COVER, g.getMaxZ()));
        collector.addPiece(new CladdingPiece(covered, site.pos(), in, GatePiece.FACADE + 1));
    }

    /** Every fortress has a forge and barracks; the other rooms are a mix. */
    private static List<Room> roomList(Random random, int count) {
        List<Room> rooms = new ArrayList<>(List.of(Room.FORGE, Room.BARRACKS, Room.BREWHALL, Room.MINE));
        while (rooms.size() < count) {
            rooms.add(Room.values()[random.nextInt(Room.values().length)]);
        }
        for (int i = rooms.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Room r = rooms.get(i);
            rooms.set(i, rooms.get(j));
            rooms.set(j, r);
        }
        return rooms.subList(0, count);
    }

    /** Terrain heights from the noise generator, before anything is built, cached per column. */
    static final class Terrain {
        /** How far apart candidate gate sites are tried, around the middle of the chunk. */
        private static final int SITE_STEP = 8;
        /** Depths into the mountain (from the gate) at which the rock has to rise above the halls. */
        private static final int[] DEPTHS = {16, 30, 44, 58};
        /** Distances out in front of the gate where the ground mustn't rise. */
        private static final int[] FRONT = {8, 14, 20};
        /** How far above the gate floor the mountain must stand there: enough to bury the hall. */
        private static final int COVER = 12;

        private final ChunkGenerator generator;
        private final HeightLimitView world;
        private final NoiseConfig noise;
        private final BiomeSource biomes;
        private final Predicate<RegistryEntry<Biome>> biomeAllowed;
        private final Map<Long, int[]> cache = new HashMap<>();

        Terrain(Structure.Context context) {
            this.generator = context.chunkGenerator();
            this.world = context.world();
            this.noise = context.noiseConfig();
            this.biomes = context.biomeSource();
            this.biomeAllowed = context.biomePredicate();
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

        boolean biomeAllowed(int x, int z) {
            RegistryEntry<Biome> biome = this.biomes.getBiome(BiomeCoords.fromBlock(x), BiomeCoords.fromBlock(this.ground(x, z)),
                    BiomeCoords.fromBlock(z), this.noise.getMultiNoiseSampler());
            return this.biomeAllowed.test(biome);
        }

        /**
         * The best gate site near the middle of the chunk: dry, fairly level ground for the
         * terrace, with a mountain rising steeply behind it, high enough to carve the halls into.
         */
        Optional<Site> findSite(int cx, int cz) {
            // Cheap check first: most chunks aren't in the mountains at all.
            if (!this.biomeAllowed(cx, cz)) {
                return Optional.empty();
            }
            Site best = null;
            int bestScore = 0;
            for (int dx = -SITE_STEP; dx <= SITE_STEP; dx += SITE_STEP) {
                for (int dz = -SITE_STEP; dz <= SITE_STEP; dz += SITE_STEP) {
                    int x = cx + dx;
                    int z = cz + dz;
                    if (this.water(x, z)) {
                        continue;
                    }
                    int y = this.ground(x, z);
                    for (Direction in : Direction.Type.HORIZONTAL) {
                        int score = this.score(x, y, z, in);
                        if (score > bestScore) {
                            bestScore = score;
                            best = new Site(new BlockPos(x, y, z), in);
                        }
                    }
                }
            }
            if (best == null || !this.biomeAllowed(best.pos().getX(), best.pos().getZ())) {
                return Optional.empty();
            }
            return Optional.of(best);
        }

        /** How well the mountain buries a fortress whose gate is at x/y/z looking {@code in}; 0 if it won't do. */
        private int score(int x, int y, int z, Direction in) {
            int ox = in.getOffsetX();
            int oz = in.getOffsetZ();
            // The terrace in front of the gate: not a cliff edge, not a wall.
            for (int d = 1; d <= 4; d += 3) {
                int g = this.ground(x - ox * d, z - oz * d);
                if (g < y - 5 || g > y + 3 || this.water(x - ox * d, z - oz * d)) {
                    return 0;
                }
            }
            // And open country beyond it, so the gate looks out over the valley rather than into
            // the far side of a gully.
            for (int d : FRONT) {
                if (this.ground(x - ox * d, z - oz * d) > y + 4) {
                    return 0;
                }
            }
            int score = 0;
            int buried = 0;
            for (int depth : DEPTHS) {
                int rise = this.ground(x + ox * depth, z + oz * depth) - y;
                if (rise >= COVER) {
                    buried++;
                }
                score += Math.min(rise, COVER + 10);
            }
            // The first sample has to clear the hall; one of the deeper ones may dip.
            if (buried < DEPTHS.length - 1 || this.ground(x + ox * DEPTHS[0], z + oz * DEPTHS[0]) - y < COVER / 2) {
                return 0;
            }
            return score;
        }
    }
}
