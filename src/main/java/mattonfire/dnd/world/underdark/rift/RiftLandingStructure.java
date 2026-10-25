package mattonfire.dnd.world.underdark.rift;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * A Rift Landing in the Underdark (design 9, 2.6): where a rift's veil drops you, straight below it. Its structure
 * set has the same spacing, separation and salt as the rifts', so one starts in every rift's chunk (and in rift
 * slots whose rift didn't fit, which does no harm). The floor is the first open cavern floor down the centre
 * column from y {@link #SCAN_TOP} to {@link #SCAN_BOTTOM}, or y {@link #CARVE_Y} carved out of the rock.
 */
public class RiftLandingStructure extends Structure {
    public static final Codec<RiftLandingStructure> CODEC = createCodec(RiftLandingStructure::new);

    public static final int SCAN_TOP = -80;
    public static final int SCAN_BOTTOM = -200;
    public static final int CARVE_Y = -150;
    /** Air over a floor that makes it a cavern floor and not a crawlway. */
    private static final int HEADROOM = 3;

    public RiftLandingStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        if (context.world().getBottomY() > SCAN_BOTTOM) {
            return Optional.empty(); // not the Underdark's generator
        }
        VerticalBlockSample column = context.chunkGenerator().getColumnSample(x, z, context.world(), context.noiseConfig());
        int floor = CARVE_Y;
        boolean found = false;
        for (int y = SCAN_TOP; y >= SCAN_BOTTOM; y--) {
            if (solid(column.getState(y)) && open(column, y + 1)) {
                floor = y;
                found = true;
                break;
            }
        }
        BlockPos center = new BlockPos(x, floor, z);
        long seed = context.random().nextLong();
        boolean carved = !found;
        return Optional.of(new StructurePosition(center.up(), collector ->
                collector.addPiece(new RiftLandingPiece(center, carved, seed))));
    }

    private static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    private static boolean open(VerticalBlockSample column, int from) {
        for (int y = from; y < from + HEADROOM; y++) {
            if (!column.getState(y).isAir()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StructureType<?> getType() {
        return UnderdarkRiftStructures.RIFT_LANDING;
    }
}
