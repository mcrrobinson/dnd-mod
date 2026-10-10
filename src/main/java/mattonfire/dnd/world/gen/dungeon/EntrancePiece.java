package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;

/**
 * What shows of a dungeon at the surface: a ring of weathered standing stones round the top of
 * the stair. Not a room. (Each theme dresses its entrance properly in its own ticket.)
 */
public class EntrancePiece extends DungeonPiece {
    private static final int RADIUS = 5;
    private static final int[][] STONES = {{5, 0}, {-5, 0}, {0, 5}, {0, -5}, {4, 4}, {4, -4}, {-4, 4}, {-4, -4}};

    private final int top;

    public EntrancePiece(Info info, int cx, int cz, int top, long seed) {
        super(DungeonStructures.ENTRANCE, info, new BlockBox(cx - RADIUS, top - 2, cz - RADIUS, cx + RADIUS, top + 5, cz + RADIUS),
                seed, -1, null, 0, List.of());
        this.top = top;
    }

    public EntrancePiece(NbtCompound nbt) {
        super(DungeonStructures.ENTRANCE, nbt);
        this.top = nbt.getInt("Top");
    }

    @Override
    protected void writeNbt(net.minecraft.structure.StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Top", this.top);
    }

    @Override
    protected void build(Builder b) {
        int ground = this.top - this.info.floorY();
        for (int[] stone : STONES) {
            int x = RADIUS + stone[0];
            int z = RADIUS + stone[1];
            int height = 2 + b.random.nextInt(2);
            boolean fallen = b.chance(0.2F);
            // Footing, so a stone on a slope doesn't float
            b.set(x, ground - 1, z, b.palette.surface(b.random));
            b.set(x, ground, z, b.palette.surface(b.random));
            int standing = fallen ? 1 : height;
            for (int y = 1; y <= standing; y++) {
                b.set(x, ground + y, z, y == standing ? b.palette.accent() : b.palette.surface(b.random));
            }
            if (!fallen && b.chance(0.4F)) {
                b.set(x, ground + standing + 1, z, Blocks.SKELETON_SKULL.getDefaultState());
            }
        }
    }
}
