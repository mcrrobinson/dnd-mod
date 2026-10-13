package mattonfire.dnd.world.gen.stronghold;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Optional;
import mattonfire.dnd.world.gen.TerrainColumns;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * An Orc Stronghold ("Bloodtusk Hold"): a walled warcamp in the badlands. A levelled clearing of radius
 * {@link #CLEAR} inside a palisade of radius {@link #PALISADE}, with a gatehouse, the Great Longhouse (the
 * hearth), the Fighting Pit, a smithy and hide yurts. The Half-Orc home.
 *
 * <p>Local layout (gate to the south, b grows towards it):
 * <pre>
 *            longhouse   a -10..10, b -18..-8   (seat and hoard at the back)
 *   yurt (-14,0)   pit (0,3) r7   smithy a 12..19, b -5..5
 *   yurt (-11,12)               yurt (11,12)
 *            gatehouse at b = 22, towers at a = +-4
 * </pre>
 * The site check samples rings of terrain round the middle: a few samples may be off (wet, steep or out of
 * the biome) and the floor is the median height, so the clearing is cut and filled as little as possible.
 */
public class OrcStrongholdStructure extends Structure {
    public static final Codec<OrcStrongholdStructure> CODEC = createCodec(OrcStrongholdStructure::new);

    /** Radius of the palisade ring. */
    public static final int PALISADE = 22;
    /** How far out the ground is levelled. */
    public static final int CLEAR = 25;
    private static final int[] RINGS = {8, 14, 20, 24};
    private static final int[] MAX_RISE = {4, 6, 8, 10};
    /** How many of the 32 samples may be wet, too steep or in the wrong biome. */
    private static final int MAX_BAD = 5;

    /** Yurt centres in the hold's frame. */
    static final int[][] YURTS = {{-14, 0}, {-11, 12}, {11, 12}};
    /** Pit centre in the hold's frame. */
    static final int PIT_A = 0;
    static final int PIT_B = 3;

    public OrcStrongholdStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        TerrainColumns columns = new TerrainColumns(context);
        if (columns.water(x, z) || !columns.biomeAllowed(x, z)) {
            return Optional.empty();
        }
        int y0 = columns.ground(x, z);
        int[] heights = new int[RINGS.length * 8 + 1];
        heights[0] = y0;
        int n = 1;
        int bad = 0;
        for (int ring = 0; ring < RINGS.length; ring++) {
            for (int i = 0; i < 8; i++) {
                double angle = (i + ring * 0.5D) * Math.PI / 4.0D;
                int sx = x + (int) Math.round(Math.cos(angle) * RINGS[ring]);
                int sz = z + (int) Math.round(Math.sin(angle) * RINGS[ring]);
                int h = columns.ground(sx, sz);
                heights[n++] = h;
                if (columns.water(sx, sz) || Math.abs(h - y0) > MAX_RISE[ring] || !columns.biomeAllowed(sx, sz)) {
                    bad++;
                }
            }
        }
        if (bad > MAX_BAD) {
            return Optional.empty();
        }
        Arrays.sort(heights);
        BlockPos center = new BlockPos(x, heights[heights.length / 2], z);
        long hold = chunk.toLong();
        return Optional.of(new StructurePosition(center, collector -> plan(collector, context.random(), center, hold)));
    }

    /** Places the pieces. Order matters: the grounds level and clear everything the buildings then stand on. */
    static void plan(StructurePiecesCollector collector, Random random, BlockPos center, long hold) {
        Direction gate = Direction.fromHorizontal(random.nextInt(4));
        collector.addPiece(new GroundsPiece(center, gate, random.nextLong(), hold));
        collector.addPiece(new FightingPitPiece(center, gate, random.nextLong(), hold));
        collector.addPiece(new LonghousePiece(center, gate, random.nextLong(), hold));
        collector.addPiece(new SmithyPiece(center, gate, random.nextLong(), hold));
        for (int[] yurt : YURTS) {
            collector.addPiece(new YurtPiece(center, gate, random.nextLong(), hold, yurt[0], yurt[1]));
        }
        collector.addPiece(new GatehousePiece(center, gate, random.nextLong(), hold));
    }

    @Override
    public StructureType<?> getType() {
        return OrcStrongholdStructures.ORC_STRONGHOLD;
    }
}
