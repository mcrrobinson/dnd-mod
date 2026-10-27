package mattonfire.dnd.world.underdark.lair;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A Troll Bridge (design 9, section 10): a Zurkhwood bridge across a channel of the Darklake.
 * <p>
 * The site check reads the noise columns (before carvers and features) round the chunk's centre, on a 5 x 5 grid of
 * points {@value #STEP} blocks apart, nearest first. A point is open water when its column holds water at the lake's surface (the sea level
 * less one) with air above it and at least {@value #MIN_DEPTH} deep. From there it walks east-west and north-south:
 * each way it must reach a bank within {@value #MAX_REACH} blocks, a column solid at the surface with at most
 * {@value #MAX_BANK} blocks of rock above it and {@value #BANK_AIR} of air over that. The channel between the banks
 * must be {@value #MIN_SPAN} to {@value #MAX_SPAN} blocks wide, with {@value #HEADROOM} blocks of air over the water
 * in its middle. The first point and way that fit win; the bridge is centred on the channel.
 */
public class TrollBridgeStructure extends UnderdarkLairStructure {
    public static final Codec<TrollBridgeStructure> CODEC = createCodec(TrollBridgeStructure::new);

    static final int STEP = 4;
    static final int MIN_DEPTH = 2;
    static final int MAX_REACH = 16;
    static final int MAX_BANK = 6;
    static final int BANK_AIR = 4;
    static final int MIN_SPAN = 5;
    static final int MAX_SPAN = 18;
    static final int HEADROOM = 7;
    /** Blocks of deck carried onto each bank past the water. */
    static final int ONTO_BANK = 2;

    public TrollBridgeStructure(Config config) {
        super(config);
    }

    /** A channel found: its middle, the way across, and the banks. */
    record Crossing(BlockPos middle, Direction across, int nearWater, int farWater, int nearBank, int farBank) {
    }

    @Override
    protected Optional<StructurePosition> site(Context context) {
        int surface = context.chunkGenerator().getSeaLevel() - 1;
        Map<Long, VerticalBlockSample> columns = new HashMap<>();
        ChunkPos chunk = context.chunkPos();
        int x0 = chunk.getCenterX();
        int z0 = chunk.getCenterZ();
        // A 5 x 5 grid of points round the chunk's centre, nearest first.
        java.util.List<int[]> points = new java.util.ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                points.add(new int[] {i * STEP, j * STEP});
            }
        }
        points.sort(java.util.Comparator.comparingInt(p -> p[0] * p[0] + p[1] * p[1]));
        for (int[] point : points) {
            int x = x0 + point[0];
            int z = z0 + point[1];
            if (!water(context, columns, x, z, surface, MIN_DEPTH)) {
                continue;
            }
            for (Direction across : new Direction[] {Direction.EAST, Direction.SOUTH}) {
                Crossing crossing = crossing(context, columns, x, z, surface, across);
                if (crossing != null) {
                    long seed = context.random().nextLong();
                    BlockPos deck = crossing.middle();
                    return Optional.of(new StructurePosition(deck, collector -> {
                        Random random = Random.create(seed);
                        // The front is one way along the bridge, at random.
                        boolean flip = random.nextBoolean();
                        Direction front = flip ? crossing.across().getOpposite() : crossing.across();
                        int near = flip ? crossing.farWater() : crossing.nearWater();
                        int far = flip ? crossing.nearWater() : crossing.farWater();
                        int nearBank = flip ? crossing.farBank() : crossing.nearBank();
                        int farBank = flip ? crossing.nearBank() : crossing.farBank();
                        collector.addPiece(new TrollBridgePiece(deck, front, random.nextLong(), near, far,
                                nearBank, farBank, surface - deck.getY()));
                    }));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * The channel through x, z along {@code across}, if it fits. The middle is at standing height on the deck
     * ({@link TrollBridgePiece#deckY}); the water and bank figures are blocks from the middle (towards -across
     * first) and bank tops relative to the surface.
     */
    private static Crossing crossing(Context context, Map<Long, VerticalBlockSample> columns, int x, int z,
                                     int surface, Direction across) {
        int dx = across.getOffsetX();
        int dz = across.getOffsetZ();
        int[] reach = new int[2];
        int[] bank = new int[2];
        for (int side = 0; side < 2; side++) {
            int sign = side == 0 ? -1 : 1;
            int d = 1;
            while (d <= MAX_REACH && water(context, columns, x + sign * d * dx, z + sign * d * dz, surface, 1)) {
                d++;
            }
            if (d > MAX_REACH) {
                return null;
            }
            int top = bankTop(context, columns, x + sign * d * dx, z + sign * d * dz, surface);
            if (top < 0) {
                return null;
            }
            reach[side] = d;
            bank[side] = top;
        }
        int span = reach[0] + reach[1] - 1;
        if (span < MIN_SPAN || span > MAX_SPAN) {
            return null;
        }
        // Centre the bridge on the channel: shift by half the difference.
        int shift = (reach[1] - reach[0]) / 2;
        int mx = x + shift * dx;
        int mz = z + shift * dz;
        if (!water(context, columns, mx, mz, surface, 1) || air(context, columns, mx, mz, surface + 1) < HEADROOM) {
            return null;
        }
        int deckY = TrollBridgePiece.deckY(surface, Math.max(bank[0], bank[1]));
        // Water runs from -(reach[0] - 1 + shift) to reach[1] - 1 - shift along +across.
        return new Crossing(new BlockPos(mx, deckY + 1, mz), across, reach[0] + shift, reach[1] - shift, bank[0],
                bank[1]);
    }

    private static VerticalBlockSample column(Context context, Map<Long, VerticalBlockSample> columns, int x, int z) {
        return columns.computeIfAbsent(ChunkPos.toLong(x, z), key -> context.chunkGenerator().getColumnSample(x, z,
                context.world(), context.noiseConfig()));
    }

    /** Whether column x, z is water at the surface with air above, at least {@code depth} deep. */
    private static boolean water(Context context, Map<Long, VerticalBlockSample> columns, int x, int z, int surface,
                                 int depth) {
        VerticalBlockSample column = column(context, columns, x, z);
        if (!column.getState(surface + 1).isAir()) {
            return false;
        }
        for (int y = surface; y > surface - depth; y--) {
            if (column.getState(y).getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * How far above the surface a bank's top block lies (0 when level with the water), or -1 if column x, z isn't a
     * bank: solid at the surface, its top within {@value #MAX_BANK} blocks, {@value #BANK_AIR} of air over it.
     */
    private static int bankTop(Context context, Map<Long, VerticalBlockSample> columns, int x, int z, int surface) {
        VerticalBlockSample column = column(context, columns, x, z);
        if (!solid(column.getState(surface))) {
            return -1;
        }
        int top = surface;
        while (top < surface + MAX_BANK + 1 && solid(column.getState(top + 1))) {
            top++;
        }
        if (top > surface + MAX_BANK) {
            return -1;
        }
        return air(context, columns, x, z, top + 1) >= BANK_AIR ? top - surface : -1;
    }

    /** Air blocks from y upwards in column x, z (up to 16). */
    private static int air(Context context, Map<Long, VerticalBlockSample> columns, int x, int z, int y) {
        VerticalBlockSample column = column(context, columns, x, z);
        int n = 0;
        while (n < 16 && column.getState(y + n).isAir()) {
            n++;
        }
        return n;
    }

    private static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    @Override
    public StructureType<?> getType() {
        return UnderdarkLairs.TROLL_BRIDGE;
    }
}
