package mattonfire.dnd.world.gen.temple;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The grand stair from the foot of the slope up to the courtyard: 5 wide, one step up per block, deepslate
 * tile steps with polished blackstone at the edges between balustrades of polished blackstone with gilded
 * posts and lanterns every fourth step, and braziers at the foot. Built up from the ground beneath and cleared above.
 */
public class GrandStairPiece extends TemplePiece {
    static final int WIDTH = 7;
    private static final int HEADROOM = 5;
    private final int rise;

    public GrandStairPiece(BlockPos foot, Direction facing, int rise, long seed) {
        super(DragonTempleStructures.STAIR, boxFrom(foot, facing, WIDTH, rise + HEADROOM + 1, rise + 1), facing, seed);
        this.rise = rise;
    }

    public GrandStairPiece(NbtCompound nbt) {
        super(DragonTempleStructures.STAIR, nbt);
        this.rise = nbt.getInt("Rise");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Rise", this.rise);
    }

    @Override
    protected void build(Builder b) {
        BlockState fill = Blocks.COBBLED_DEEPSLATE.getDefaultState();
        BlockState rail = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState();
        for (int z = 0; z <= this.rise; z++) {
            for (int x = 0; x < WIDTH; x++) {
                b.foundation(x, 0, z, fill);
                boolean wall = x == 0 || x == WIDTH - 1;
                // Solid under the step, the step itself, and clear air above it.
                for (int y = 0; y < z; y++) {
                    b.set(x, y, z, wall ? Blocks.POLISHED_BLACKSTONE_BRICKS : masonry(b));
                }
                if (wall) {
                    b.set(x, z, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
                    boolean post = z % 4 == 0;
                    b.set(x, z + 1, z, post ? Blocks.GILDED_BLACKSTONE.getDefaultState() : rail);
                    b.air(x, z + 2, z, x, z + HEADROOM, z);
                    if (post && z > 0) {
                        b.set(x, z + 2, z, lantern(false));
                    }
                } else {
                    boolean edge = x == 1 || x == WIDTH - 2;
                    if (z == 0) {
                        b.set(x, 0, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
                    } else {
                        b.set(x, z, z, stairs(edge ? Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS : Blocks.DEEPSLATE_TILE_STAIRS, BACK, false));
                    }
                    b.air(x, z + 1, z, x, z + HEADROOM, z);
                }
            }
        }
        // Braziers either side of the foot.
        for (int x : new int[]{0, WIDTH - 1}) {
            b.set(x, 1, 0, Blocks.GILDED_BLACKSTONE);
            b.set(x, 2, 0, Blocks.CAMPFIRE);
        }
    }
}
