package mattonfire.dnd.world.gen.temple;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

/**
 * Blends the terrace into its mountain. Built before everything else, it reshapes the ground round the terrace,
 * column by column, from what's there:
 * <ul>
 * <li>Over the terrace the slope is cleared away above the floor.</li>
 * <li>Along the back and sides, where the ground stands higher than the floor, a retaining wall of rough
 * natural stone rises 3 blocks from the terrace edge, with a ledge 2 blocks deep behind it.</li>
 * <li>Beyond the ledge the ground is cut back to a slope that climbs about a block per block, steepening
 * further out, with a little noise so it doesn't look machined. Each cut column keeps its own surface (snow,
 * grass, stone) on top, and the cut fades out towards the edge of the piece, so no sheer face is left.</li>
 * <li>Where the ground falls away below the sides and front it's built up into a sloping embankment of the
 * column's own ground, leaving a few blocks of the terrace wall showing.</li>
 * </ul>
 * Vegetation on cut columns goes with them. Natural generation lays snow on the new surfaces afterwards (the
 * temple places in the top-layer step, before the freeze). This piece's box is only ground: spawns and raids
 * ignore it.
 */
public class SlopePiece extends TemplePiece {
    /** How far the slope is reshaped beyond the terrace's back and sides, and in front of it. */
    static final int MARGIN = 20;
    static final int FRONT_MARGIN = 10;
    /** How far below and above the floor the box reaches. */
    static final int DOWN = 24;
    static final int UP = 46;
    /** The retaining wall's height above the floor, and the ledge behind it. */
    static final int WALL = 3;
    static final float LEDGE = 3.5F;

    static final int WIDTH = DragonTemplePlanner.WIDTH + 2 * MARGIN;
    static final int DEPTH = FRONT_MARGIN + DragonTemplePlanner.DEPTH + MARGIN;

    public SlopePiece(BlockPos front, Direction facing, long seed) {
        super(DragonTempleStructures.SLOPE, boxFrom(front, facing, WIDTH, DOWN + UP + 1, DEPTH), facing, seed);
    }

    public SlopePiece(NbtCompound nbt) {
        super(DragonTempleStructures.SLOPE, nbt);
    }

    @Override
    protected void build(Builder b) {
        int floor = DOWN;
        int top = DOWN + UP;
        int tx0 = MARGIN;
        int tx1 = MARGIN + DragonTemplePlanner.WIDTH - 1;
        int tz0 = FRONT_MARGIN;
        int tz1 = FRONT_MARGIN + DragonTemplePlanner.DEPTH - 1;
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                if (!b.inChunk(x, floor, z)) {
                    continue;
                }
                int dx = Math.max(Math.max(tx0 - x, x - tx1), 0);
                int dz = Math.max(Math.max(tz0 - z, z - tz1), 0);
                if (dx == 0 && dz == 0) {
                    // On the terrace: clear the slope above the floor.
                    b.air(x, floor + 1, z, x, top, z);
                    continue;
                }
                this.column(b, x, z, floor, top, (float) Math.sqrt(dx * dx + dz * dz), z < tz0);
            }
        }
    }

    /** Reshapes one column {@code d} blocks out from the terrace (in front of it if {@code front}). */
    private void column(Builder b, int x, int z, int floor, int top, float d, boolean front) {
        // The natural ground: the highest solid block, under any trees, plants and snow.
        int ground = -1;
        for (int y = top; y >= 0; y--) {
            BlockState state = b.get(x, y, z);
            if (state != null && isGround(state)) {
                ground = y;
                break;
            }
            if (state != null && !state.getFluidState().isEmpty()) {
                return; // a lake or a stream: leave it be
            }
        }
        BlockState surface = ground >= 0 ? b.get(x, ground, z) : Blocks.STONE.getDefaultState();
        BlockState under = ground >= 1 ? b.get(x, ground - 1, z) : surface;
        if (under == null || !isGround(under)) {
            under = surface;
        }
        if (under.getBlock() instanceof FallingBlock) {
            under = Blocks.STONE.getDefaultState();
        }
        // How far the reshaping may move this column: it fades out towards the box's edges.
        int edge = Math.min(Math.min(x, WIDTH - 1 - x), Math.min(z, DEPTH - 1 - z));
        BlockPos world = b.pos(x, 0, z);
        float noise = this.noise(world.getX(), world.getZ());
        boolean wall = !front && d <= 1.5F;

        // Cut: the retaining wall and ledge, then the slope climbing away.
        int cut;
        if (d <= LEDGE) {
            cut = floor + WALL;
        } else {
            float t = d - LEDGE;
            cut = floor + WALL + Math.round(t + 0.06F * t * t + 1.5F * noise);
        }
        cut = Math.max(cut, ground - 3 * edge / 2);
        if (ground > cut) {
            b.air(x, cut + 1, z, x, top, z);
            if (!wall) {
                b.set(x, cut, z, surface);
                b.set(x, cut - 1, z, under);
            }
        }
        if (wall && ground > floor) {
            // The retaining wall along the terrace's back and sides, as high as the ground behind it.
            for (int y = floor + 1; y <= Math.min(ground, floor + WALL); y++) {
                BlockPos p = b.pos(x, y, z);
                b.set(x, y, z, rubble(hash(p.getX(), p.getY(), p.getZ(), this.seed)));
            }
            return;
        }

        // Fill: an embankment where the ground drops away below the terrace.
        int fill = floor - (front ? 5 : WALL) - Math.round(d + noise);
        fill = Math.min(fill, ground + 3 * edge / 2);
        if (ground < fill) {
            for (int y = Math.max(ground + 1, 0); y < fill; y++) {
                b.set(x, y, z, under);
            }
            b.set(x, fill, z, surface);
        }
    }

    /** Solid natural ground: not air, plants, snow layers, logs or leaves. */
    private static boolean isGround(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.getBlock() instanceof LeavesBlock
                || state.isIn(BlockTags.LOGS) || state.isOf(Blocks.SNOW) || state.isOf(Blocks.POWDER_SNOW)) {
            return false;
        }
        return state.isOf(Blocks.ICE) || state.isOpaque() && !state.isReplaceable() && state.getMaterial().isSolid();
    }

    /** Smooth value noise in -1..1 over the world's x/z, about 7 blocks across. */
    private float noise(int wx, int wz) {
        float fx = wx / 7.0F;
        float fz = wz / 7.0F;
        int x0 = MathHelper.floor(fx);
        int z0 = MathHelper.floor(fz);
        float tx = smooth(fx - x0);
        float tz = smooth(fz - z0);
        float a = MathHelper.lerp(tx, this.corner(x0, z0), this.corner(x0 + 1, z0));
        float c = MathHelper.lerp(tx, this.corner(x0, z0 + 1), this.corner(x0 + 1, z0 + 1));
        return MathHelper.lerp(tz, a, c);
    }

    private float corner(int x, int z) {
        return (Math.floorMod(hash(x, 0, z, this.seed), 2001L) - 1000) / 1000.0F;
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }
}
