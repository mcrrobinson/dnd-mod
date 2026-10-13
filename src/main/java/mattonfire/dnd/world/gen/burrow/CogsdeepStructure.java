package mattonfire.dnd.world.gen.burrow;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.StructureProximity;
import net.minecraft.block.BlockState;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Cogsdeep, the gnomes' burrow and the Gnome home. On the surface a round observatory with a weathering copper
 * dome, a telescope and a lightning rod; inside it a spiral stair down a sealed shaft to the burrow at y
 * {@link #MIN_FLOOR}-{@link #MAX_FLOOR} (its tallest room tops out at y 30): the Commons (the hearth) round the
 * foot of the stair, two workshop halls full of unpowered contraptions, the clocktower room, the gem-polishing
 * room, and a lush grotto beyond the east workshop.
 *
 * <p>Local layout of the burrow (door of the observatory to the south, b grows towards it):
 * <pre>
 *                     clocktower  a -7..7, b -26..-8 (14 high)
 *   west workshop a -26..-8   Commons a -8..8, b -8..8   east workshop a 8..26   grotto a 27..47
 *                     gem room    a -7..7, b 8..22
 * </pre>
 *
 * <p>The site check: the ground round the stair (rings at 3 and 7 blocks) must be dry and within
 * {@link #MAX_RISE} blocks of the middle, between y {@link #MIN_SURFACE} and {@link #MAX_SURFACE}; the rock where
 * the burrow goes (noise columns, before carvers) must be mostly solid, so it isn't hung in a huge cave; and no
 * other settlement, dungeon or underground lair whose ground or shaft could meet the burrow may start nearby
 * ({@link #KEEP_CLEAR_OF}).
 */
public class CogsdeepStructure extends Structure {
    public static final Codec<CogsdeepStructure> CODEC = createCodec(CogsdeepStructure::new);

    /** The burrow's floor (where you stand) lies between these heights. */
    public static final int MIN_FLOOR = 11;
    public static final int MAX_FLOOR = 16;
    /** The ground round the observatory may rise or fall this much from the middle. */
    public static final int MAX_RISE = 3;
    public static final int MIN_SURFACE = 58;
    public static final int MAX_SURFACE = 150;
    /** The share of sampled burrow blocks that may be open (cave air or aquifer water) in the noise. */
    public static final double MAX_OPEN = 0.3D;
    /** Other structure sets that must not generate within {@link #CLEARANCE} chunks (only real starts count). */
    static final List<Identifier> KEEP_CLEAR_OF = List.of(id("hobbit_villages"), id("elven_enclaves"),
            id("goblin_camps"), id("crypts"), id("goblin_warrens"), id("dwarven_ruins"), id("dwarven_fortresses"),
            id("beholder_lairs"));
    public static final int CLEARANCE = 5;

    /** Burrow sample columns (a, b) for the rock check: the middle of each room. */
    private static final int[][] ROCK_SAMPLES = {{0, 0}, {17, 0}, {-17, 0}, {0, -17}, {0, 15}, {37, 0}, {12, 12}, {-12, -12}};

    public CogsdeepStructure(Config config) {
        super(config);
    }

    private static Identifier id(String path) {
        return new Identifier(DnDClasses.MOD_ID, path);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        Optional<Integer> ground = ground(context, x, z);
        if (ground.isEmpty()) {
            return Optional.empty();
        }
        Random random = context.random();
        Direction door = Direction.fromHorizontal(random.nextInt(4));
        int floor = MIN_FLOOR + random.nextInt(MAX_FLOOR - MIN_FLOOR + 1);
        if (!solidRock(context, x, z, floor, door)) {
            return Optional.empty();
        }
        for (Identifier set : KEEP_CLEAR_OF) {
            if (StructureProximity.nearGenerating(context, set, CLEARANCE)) {
                return Optional.empty();
            }
        }
        int surface = ground.get();
        BlockPos top = new BlockPos(x, surface, z);
        long burrow = chunk.toLong();
        long seed = random.nextLong();
        return Optional.of(new StructurePosition(top, collector -> plan(collector, Random.create(seed), x, z, surface, floor,
                door, burrow)));
    }

    /**
     * The height you stand at on the ground in the middle (the first air above the surface), if the ground out to 7
     * blocks is dry, within {@link #MAX_RISE} of it and between {@link #MIN_SURFACE} and {@link #MAX_SURFACE}.
     */
    static Optional<Integer> ground(Context context, int x, int z) {
        int middle = surface(context, x, z);
        if (middle < 0 || middle < MIN_SURFACE || middle > MAX_SURFACE) {
            return Optional.empty();
        }
        for (int radius : new int[]{3, 7}) {
            for (int i = 0; i < 8; i++) {
                double angle = (i + radius * 0.5D) * Math.PI / 4.0D;
                int sx = x + (int) Math.round(Math.cos(angle) * radius);
                int sz = z + (int) Math.round(Math.sin(angle) * radius);
                int y = surface(context, sx, sz);
                if (y < 0 || Math.abs(y - middle) > MAX_RISE) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(middle);
    }

    /** The first air above dry ground at x/z, or -1 where water lies on top. */
    private static int surface(Context context, int x, int z) {
        int surface = context.chunkGenerator().getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig());
        int floor = context.chunkGenerator().getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig());
        return floor < surface ? -1 : surface;
    }

    /**
     * Whether the rock where the burrow goes is mostly solid: in the noise columns at the middle of each room, at
     * most {@link #MAX_OPEN} of the blocks from the floor to the Commons' ceiling are air or water.
     */
    private static boolean solidRock(Context context, int x, int z, int floor, Direction door) {
        int open = 0;
        int total = 0;
        BlockPos center = new BlockPos(x, floor, z);
        for (int[] sample : ROCK_SAMPLES) {
            BlockPos pos = CogsdeepPiece.at(center, door, sample[0], 0, sample[1]);
            VerticalBlockSample column = context.chunkGenerator().getColumnSample(pos.getX(), pos.getZ(), context.world(),
                    context.noiseConfig());
            for (int y = floor - 1; y <= floor + 6; y++) {
                BlockState state = column.getState(y);
                total++;
                if (state.isAir() || !state.getFluidState().isEmpty()) {
                    open++;
                }
            }
        }
        return open <= total * MAX_OPEN;
    }

    /** Places the pieces. Order matters: the rooms first, then the observatory, then the shaft cut through them all. */
    static void plan(StructurePiecesCollector collector, Random random, int x, int z, int surface, int floor, Direction door,
                     long burrow) {
        BlockPos below = new BlockPos(x, floor, z);
        BlockPos above = new BlockPos(x, surface, z);
        collector.addPiece(new CommonsPiece(below, door, random.nextLong(), burrow));
        collector.addPiece(new WorkshopPiece(below, door, random.nextLong(), burrow, 1));
        collector.addPiece(new WorkshopPiece(below, door, random.nextLong(), burrow, -1));
        collector.addPiece(new ClocktowerPiece(below, door, random.nextLong(), burrow));
        collector.addPiece(new GemRoomPiece(below, door, random.nextLong(), burrow));
        collector.addPiece(new GrottoPiece(below, door, random.nextLong(), burrow));
        collector.addPiece(new ObservatoryPiece(above, door, random.nextLong(), burrow));
        collector.addPiece(new ShaftPiece(below, door, random.nextLong(), burrow, surface - 1 - floor));
    }

    @Override
    public StructureType<?> getType() {
        return CogsdeepStructures.COGSDEEP;
    }
}
