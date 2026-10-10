package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

/**
 * A passage between two rooms' doorways: 3 wide and 4 high inside, walled like a room and open at
 * both ends, where it meets the rooms' walls. Not a room, so it has no ward.
 */
public class DungeonCorridorPiece extends DungeonPiece {
    public static final int WIDTH = 7;
    public static final int HEIGHT = 4;

    private final Direction.Axis axis;

    public DungeonCorridorPiece(Info info, BlockBox box, Direction.Axis axis, long seed) {
        super(DungeonStructures.CORRIDOR, info, box, seed, -1, null, HEIGHT, List.of());
        this.axis = axis;
    }

    public DungeonCorridorPiece(NbtCompound nbt) {
        super(DungeonStructures.CORRIDOR, nbt);
        this.axis = nbt.getString("Axis").equals("z") ? Direction.Axis.Z : Direction.Axis.X;
    }

    @Override
    protected void writeNbt(net.minecraft.structure.StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putString("Axis", this.axis == Direction.Axis.Z ? "z" : "x");
    }

    /**
     * The corridor running {@code axis} between two room boxes facing each other across a gap,
     * centred on {@code line} (the other horizontal coordinate). Null if the rooms touch.
     */
    static DungeonCorridorPiece between(Info info, BlockBox a, BlockBox b, Direction.Axis axis, int line, long seed) {
        int y1 = info.floorY() - 1;
        int y2 = info.floorY() + HEIGHT + 2;
        if (axis == Direction.Axis.X) {
            int from = Math.min(a.getMaxX(), b.getMaxX()) + 1;
            int to = Math.max(a.getMinX(), b.getMinX()) - 1;
            return to < from ? null : new DungeonCorridorPiece(info, new BlockBox(from, y1, line - 3, to, y2, line + 3), axis, seed);
        }
        int from = Math.min(a.getMaxZ(), b.getMaxZ()) + 1;
        int to = Math.max(a.getMinZ(), b.getMinZ()) - 1;
        return to < from ? null : new DungeonCorridorPiece(info, new BlockBox(line - 3, y1, from, line + 3, y2, to), axis, seed);
    }

    @Override
    protected void build(Builder b) {
        int w = this.width();
        int d = this.depth();
        int length = this.axis == Direction.Axis.X ? w : d;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                int lateral = Math.abs((this.axis == Direction.Axis.X ? z : x) - 3);
                b.foundation(x, z);
                for (int y = -1; y <= HEIGHT + 2; y++) {
                    BlockState state;
                    if (y == -1 || y == HEIGHT + 2 || lateral == 3) {
                        state = b.palette.shell();
                    } else if (y == 0) {
                        state = lateral == 2 ? b.palette.trim() : b.palette.floor(b.random);
                    } else if (lateral == 2 || y == HEIGHT + 1) {
                        state = b.palette.wall(b.random);
                    } else {
                        state = Blocks.AIR.getDefaultState();
                    }
                    b.set(x, y, z, state);
                }
            }
        }
        if (length >= 5) {
            int mid = length / 2;
            if (this.axis == Direction.Axis.X) {
                b.light(mid, HEIGHT, 3, true);
            } else {
                b.light(3, HEIGHT, mid, true);
            }
        }
    }
}
