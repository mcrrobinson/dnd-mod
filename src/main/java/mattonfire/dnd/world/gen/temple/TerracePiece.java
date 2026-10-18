package mattonfire.dnd.world.gen.temple;

import mattonfire.dnd.classes.Registry.ModTempleBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The terrace the temple stands on, cut into the slope: the whole footprint is levelled to one floor, the
 * rock above it is cut away and the drops below are built up with cobbled deepslate. A low parapet runs
 * along the open edges. The other pieces build on top of it.
 */
public class TerracePiece extends TemplePiece {
    static final int WIDTH = DragonTemplePlanner.WIDTH;
    static final int DEPTH = DragonTemplePlanner.DEPTH;
    /** How high above the floor the slope is cut away. */
    static final int CLEAR = 13;
    /** The gap in the front parapet where the grand stair arrives (local x). */
    static final int GAP_MIN = WIDTH / 2 - 3;
    static final int GAP_MAX = WIDTH / 2 + 3;

    public TerracePiece(BlockPos front, Direction facing, long seed) {
        super(DragonTempleStructures.TERRACE, boxFrom(front, facing, WIDTH, CLEAR + 1, DEPTH), facing, seed);
    }

    public TerracePiece(NbtCompound nbt) {
        super(DragonTempleStructures.TERRACE, nbt);
    }

    @Override
    protected void build(Builder b) {
        BlockState fill = Blocks.COBBLED_DEEPSLATE.getDefaultState();
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                b.foundation(x, 0, z, fill);
                boolean edge = x == 0 || z == 0 || x == WIDTH - 1 || z == DEPTH - 1;
                b.set(x, 0, z, edge ? Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState()
                        : (x + z) % 7 == 0 ? Blocks.CHISELED_DEEPSLATE.getDefaultState() : Blocks.POLISHED_DEEPSLATE.getDefaultState());
                b.air(x, 1, z, x, CLEAR, z);
            }
        }
        // A parapet along the open edges: the front (but for the stair) and the yard's outer side and back.
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState();
        for (int x = 0; x < WIDTH; x++) {
            if (x < GAP_MIN || x > GAP_MAX) {
                b.set(x, 1, 0, x % 6 == 0 ? Blocks.GILDED_BLACKSTONE.getDefaultState() : wall);
            }
        }
        for (int z = 1; z < DEPTH; z++) {
            b.set(0, 1, z, z % 6 == 0 ? Blocks.GILDED_BLACKSTONE.getDefaultState() : wall);
        }
        for (int x = 1; x < DragonTemplePlanner.SIDE_WIDTH; x++) {
            b.set(x, 1, DEPTH - 1, wall);
        }
        // Incense braziers at the front corners, so the temple shows from the valley at night.
        for (int x : new int[]{0, WIDTH - 1}) {
            b.set(x, 1, 0, Blocks.GILDED_BLACKSTONE);
            b.set(x, 2, 0, ModTempleBlocks.EMBER_INCENSE_BRAZIER);
        }
    }
}
