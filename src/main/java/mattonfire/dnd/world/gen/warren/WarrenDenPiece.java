package mattonfire.dnd.world.gen.warren;

import mattonfire.dnd.entity.kobold.Kobolds;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The den (design 10, 4.3): an oval hall 13 x 9 with a domed roof 5 high, the warren's hub. A campfire in the middle,
 * straw bedding round the walls, bones, {@value #KOBOLDS_MIN}-{@value #KOBOLDS_MAX} kobolds and two snares hidden at
 * generation. In a great warren a second den, the guard hall, opens off its front east side through a short passage
 * ({@link #guard}).
 */
public class WarrenDenPiece extends WarrenPiece {
    public static final int HALF_A = 6;
    public static final int HALF_B = 4;
    public static final int HEIGHT = 5;
    public static final int KOBOLDS_MIN = 3;
    public static final int KOBOLDS_MAX = 5;
    /** The guard hall's middle, in the den's frame. */
    public static final int GUARD_A = 12;
    public static final int GUARD_B = 9;
    private final boolean guard;
    private final boolean great;

    public WarrenDenPiece(BlockPos center, Direction front, long seed, int tier, boolean guard, boolean great) {
        super(KoboldWarrens.DEN, center, front, seed, tier, guard ? -9 : -8, -2, guard ? -7 : -6, 8, HEIGHT + 2, 6);
        this.guard = guard;
        this.great = great;
    }

    public WarrenDenPiece(NbtCompound nbt) {
        super(KoboldWarrens.DEN, nbt);
        this.guard = nbt.getBoolean("Guard");
        this.great = nbt.getBoolean("Great");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putBoolean("Guard", this.guard);
        nbt.putBoolean("Great", this.great);
    }

    /** The guard hall of a great warren (not the den itself). */
    public boolean guard() {
        return this.guard;
    }

    static boolean inside(int a, int b) {
        double x = a / (HALF_A + 0.5D);
        double z = b / (HALF_B + 0.5D);
        return x * x + z * z <= 1.0D;
    }

    @Override
    protected void build(Builder b) {
        b.seal(-8, -1, -6, 8, HEIGHT + 1, 6);
        for (int a = -HALF_A; a <= HALF_A; a++) {
            for (int bb = -HALF_B; bb <= HALF_B; bb++) {
                if (!inside(a, bb)) {
                    continue;
                }
                double x = a / (HALF_A + 0.5D);
                double z = bb / (HALF_B + 0.5D);
                int h = 2 + (int) Math.round(3.0D * Math.sqrt(Math.max(0.0D, 1.0D - x * x - z * z)));
                b.floor(a, 0, bb, b.dirtFloor());
                b.carve(a, 1, bb, a, Math.min(HEIGHT, h), bb);
            }
        }
        if (this.guard) {
            // The passage back to the den (its far end is in the den's front east side)
            for (int a = -8; a <= -3; a++) {
                for (int bb = -6; bb <= -4; bb++) {
                    b.floor(a, 0, bb, b.dirtFloor());
                }
            }
            b.carve(-8, 1, -6, -3, 3, -4);
        }

        // The hearth
        b.set(0, 0, 0, Blocks.COBBLESTONE);
        b.set(0, 1, 0, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
        for (Direction d : Direction.Type.HORIZONTAL) {
            b.set(d.getOffsetX(), 0, -d.getOffsetZ(), Blocks.COBBLESTONE);
        }
        // Straw bedding round the walls
        int[][] beds = {{-5, -1}, {-5, 1}, {-4, 2}, {5, -1}, {5, 1}, {4, -2}, {-3, -3}, {3, 3}};
        for (int[] bed : beds) {
            if (b.chance(0.75F)) {
                b.set(bed[0], 0, bed[1], Blocks.HAY_BLOCK);
                b.set(bed[0], 1, bed[1], b.chance(0.5F) ? Blocks.BROWN_CARPET.getDefaultState() : Blocks.AIR.getDefaultState());
            }
        }
        // Gnawed bones
        b.set(-2, 1, 3, b.chance(0.5F) ? Blocks.BONE_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState());
        if (b.chance(0.5F)) {
            b.set(2, 1, -3, Blocks.SKELETON_SKULL.getDefaultState().with(SkullBlock.ROTATION, b.random.nextInt(16)));
        }

        int count = KOBOLDS_MIN + b.random.nextInt(KOBOLDS_MAX - KOBOLDS_MIN + 1) + (this.guard ? 1 : 0);
        for (int i = 0; i < count; i++) {
            double angle = i * 2.0D * Math.PI / count;
            int a = (int) Math.round(Math.cos(angle) * 3.0D);
            int bb = (int) Math.round(Math.sin(angle) * 2.0D);
            b.koboldOrSlinger(a, 1, bb, 10);
        }
        if (this.guard) {
            b.kobold(0, 1, -3, Kobolds.KOBOLD_TRAPMAKER, 12);
        } else {
            // Two snares hidden in the straw by the walls
            int side = b.chance(0.5F) ? 1 : -1;
            b.snare(-3 * side, 1, 2);
            b.snare(3 * side, 1, -1);
        }
    }

    /** Where to stand to see the den: by the tunnel, looking at the fire. */
    public BlockPos lookout() {
        return this.at(0, 1, 3);
    }
}
