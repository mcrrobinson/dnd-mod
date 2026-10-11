package mattonfire.dnd.world.gen.sanctum;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A Brimstone Sanctum: the exiles' chapel-town in the Nether, the Tiefling home. A round platform of basalt
 * and blackstone of radius {@link #PLATFORM} on pillars down to the Nether floor, ringed by a polished
 * blackstone wall with an iron-bar gate, holding the Pact Hall (the hearth), the Brass Market, the Alchemist's
 * den and the Refuge with its respawn anchors.
 *
 * <p>Local layout (gate to the south, b grows towards it):
 * <pre>
 *                pact hall  a -5..5, b -14..-3
 *   den a -13..-7, b -9..-2          market stalls a 8..12 at b -6, 0, 6
 *   refuge a -13..-6, b 2..10
 *                gate at b = 17, three wide
 * </pre>
 *
 * <p>The site check can't use heightmaps: in the Nether they find the bedrock roof. It reads whole noise
 * columns ({@link NetherColumns}) instead and looks for a floor between y {@link #MIN_FLOOR} and
 * {@link #MAX_FLOOR} with {@link #HEADROOM} blocks of open air above it. Then rings of columns round it must
 * all stand on ground above the lava sea within {@link #MAX_DROP} blocks below the platform (so it never
 * floats over lava), have no rock in the way of the buildings ({@link #CEILING}), and be in a sanctum biome.
 */
public class BrimstoneSanctumStructure extends Structure {
    public static final Codec<BrimstoneSanctumStructure> CODEC = createCodec(BrimstoneSanctumStructure::new);

    /** Radius of the platform. */
    public static final int PLATFORM = 18;
    /** Radius of the wall ring. */
    public static final int WALL = 17;
    /** The floor must be between these heights. The lava sea's surface is at 31, so the floor keeps well above it. */
    public static final int MIN_FLOOR = 40;
    public static final int MAX_FLOOR = 100;
    /** Open air needed above the middle of the floor. */
    public static final int HEADROOM = 18;
    /** No rock may stand this many blocks or less above the platform at any sample: the tallest building. */
    public static final int CEILING = 14;
    /** Bumps up to this high above the platform are cut away; anything higher counts as ceiling. */
    public static final int MAX_BUMP = 5;
    /** The ground under each sample must be this close below the platform: the pillars' longest reach. */
    public static final int MAX_DROP = 14;
    private static final int[] RINGS = {5, 10, 14, PLATFORM};
    private static final int[] RING_SAMPLES = {8, 12, 16, 20};
    /** How many of the samples may stand off the biome (the sanctum biomes border wastes and deltas). */
    private static final int MAX_OFF_BIOME = 5;
    /** How many candidate floors in the middle column are tried, from the lowest up. */
    private static final int MAX_TRIES = 3;

    /** Market stall centres (a, b) in the sanctum's frame. */
    static final int[][] STALLS = {{10, -6}, {10, 0}, {10, 6}};

    public BrimstoneSanctumStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        NetherColumns columns = new NetherColumns(context);
        Optional<Integer> floor = findFloor(columns, x, z);
        if (floor.isEmpty()) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(x, floor.get(), z);
        long sanctum = chunk.toLong();
        return Optional.of(new StructurePosition(center, collector -> plan(collector, context.random(), center, sanctum)));
    }

    /** The platform height for a sanctum centred on x/z, or empty if no floor there works (see the class notes). */
    static Optional<Integer> findFloor(NetherColumns columns, int x, int z) {
        int tries = 0;
        for (int y = MIN_FLOOR; y <= MAX_FLOOR && tries < MAX_TRIES; y++) {
            if (!columns.solid(x, y, z) || !columns.clear(x, y + 1, y + HEADROOM, z)) {
                continue;
            }
            tries++;
            if (columns.biomeAllowed(x, y + 1, z) && siteFits(columns, x, y, z)) {
                return Optional.of(y);
            }
            y += HEADROOM;
        }
        return Optional.empty();
    }

    /** Whether every ring sample round x/z suits a platform at {@code y} (see the class notes). */
    private static boolean siteFits(NetherColumns columns, int x, int y, int z) {
        int offBiome = 0;
        for (int ring = 0; ring < RINGS.length; ring++) {
            int n = RING_SAMPLES[ring];
            for (int i = 0; i < n; i++) {
                double angle = (i + ring * 0.5D) * 2.0D * Math.PI / n;
                int sx = x + (int) Math.round(Math.cos(angle) * RINGS[ring]);
                int sz = z + (int) Math.round(Math.sin(angle) * RINGS[ring]);
                if (!columns.clear(sx, y + MAX_BUMP + 1, y + CEILING, sz) || !columns.groundBelow(sx, y + MAX_BUMP, sz)) {
                    return false;
                }
                if (!columns.biomeAllowed(sx, y + 1, sz) && ++offBiome > MAX_OFF_BIOME) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Places the pieces. Order matters: the platform clears and levels everything the buildings stand on. */
    static void plan(StructurePiecesCollector collector, Random random, BlockPos center, long sanctum) {
        Direction gate = Direction.fromHorizontal(random.nextInt(4));
        collector.addPiece(new PlatformPiece(center, gate, random.nextLong(), sanctum));
        collector.addPiece(new PactHallPiece(center, gate, random.nextLong(), sanctum));
        collector.addPiece(new DenPiece(center, gate, random.nextLong(), sanctum));
        collector.addPiece(new RefugePiece(center, gate, random.nextLong(), sanctum));
        for (int[] stall : STALLS) {
            collector.addPiece(new MarketPiece(center, gate, random.nextLong(), sanctum, stall[0], stall[1]));
        }
    }

    /**
     * Nether terrain for one site check: whole noise columns, read once each. A column sample holds the noise
     * blocks (netherrack, lava, air), before surface rules, which is all the check needs.
     */
    static final class NetherColumns {
        private final Context context;
        private final Map<Long, VerticalBlockSample> samples = new HashMap<>();

        NetherColumns(Context context) {
            this.context = context;
        }

        private BlockState state(int x, int y, int z) {
            return this.samples.computeIfAbsent(ChunkPos.toLong(x, z), key -> this.context.chunkGenerator()
                    .getColumnSample(x, z, this.context.world(), this.context.noiseConfig())).getState(y);
        }

        boolean solid(int x, int y, int z) {
            BlockState state = this.state(x, y, z);
            return !state.isAir() && state.getFluidState().isEmpty();
        }

        /** Whether every block from y1 to y2 is air. */
        boolean clear(int x, int y1, int y2, int z) {
            for (int y = y1; y <= y2; y++) {
                if (!this.state(x, y, z).isAir()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Whether solid ground lies within {@link #MAX_DROP} blocks below the platform (scanning down from
         * {@code top}), above the lava sea and with no lava on the way.
         */
        boolean groundBelow(int x, int top, int z) {
            int bottom = top - MAX_BUMP - MAX_DROP;
            for (int y = top; y >= bottom; y--) {
                BlockState state = this.state(x, y, z);
                if (!state.getFluidState().isEmpty()) {
                    return false;
                }
                if (!state.isAir()) {
                    return y >= MIN_FLOOR - 1;
                }
            }
            return false;
        }

        boolean biomeAllowed(int x, int y, int z) {
            return this.context.biomePredicate().test(this.context.biomeSource().getBiome(BiomeCoords.fromBlock(x),
                    BiomeCoords.fromBlock(y), BiomeCoords.fromBlock(z), this.context.noiseConfig().getMultiNoiseSampler()));
        }
    }

    @Override
    public StructureType<?> getType() {
        return BrimstoneSanctumStructures.BRIMSTONE_SANCTUM;
    }
}
