package mattonfire.dnd.world.gen.dungeon;

import java.util.List;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The way down: a 7x7 shaft (two-block walls round a 3x3 well with a central pillar) from the
 * surface to the dungeon floor, with a spiral stair, one step up per block round. Its doorway at
 * the foot faces the first corridor. The entrance room: its ward sits under the pillar.
 */
public class StairPiece extends DungeonPiece {
    public static final int SIZE = 7;
    /** Trunks and low branches are cleared out of the shaft this far above the ground. */
    private static final int CLEAR_ABOVE = 7;
    /** The eight cells round the pillar, clockwise seen from above, starting at the north-west. */
    private static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};

    private final int top;

    public StairPiece(Info info, int cx, int cz, int top, long seed, int roomId, Direction doorSide) {
        super(DungeonStructures.STAIR, info, new BlockBox(cx - 3, info.floorY() - 1, cz - 3, cx + 3, top + CLEAR_ABOVE, cz + 3), seed, roomId,
                RoomRole.ENTRANCE, top - info.floorY(),
                List.of(new Door(doorSide, doorSide.getAxis() == Direction.Axis.Z ? cx : cz, false)));
        this.top = top;
    }

    public StairPiece(NbtCompound nbt) {
        super(DungeonStructures.STAIR, nbt);
        this.top = nbt.getInt("Top");
    }

    @Override
    protected void writeNbt(net.minecraft.structure.StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Top", this.top);
    }

    /** The top of the stair at the surface. */
    public BlockPos top() {
        return new BlockPos(this.boundingBox.getMinX() + 3, this.top + 1, this.boundingBox.getMinZ() + 3);
    }

    @Override
    protected void build(Builder b) {
        int topY = this.top - this.info.floorY();
        int start = this.firstStepCell();
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int dx = x - 3;
                int dz = z - 3;
                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                b.foundation(x, z);
                for (int y = -1; y <= topY + CLEAR_ABOVE; y++) {
                    b.set(x, y, z, this.blockAt(b, ring, dx, dz, y, topY, start));
                }
            }
        }
        b.doorways();
        b.ward(3, -1, 3);
    }

    /** The first step goes just round from the doorway side, so the stair doesn't block the way out. */
    private int firstStepCell() {
        Direction side = this.doors.isEmpty() ? Direction.WEST : this.doors.get(0).side();
        return switch (side) {
            case NORTH -> 3;
            case EAST -> 5;
            case SOUTH -> 7;
            default -> 1;
        };
    }

    private BlockState blockAt(Builder b, int ring, int dx, int dz, int y, int topY, int start) {
        BlockState air = Blocks.AIR.getDefaultState();
        if (y == -1) {
            return b.palette.shell();
        }
        if (y == 0) {
            return ring == 3 ? b.palette.shell() : ring == 2 ? b.palette.trim() : b.palette.floor(b.random);
        }
        if (ring == 3) {
            if (y <= topY) {
                return y > topY - 3 ? b.palette.surface(b.random) : b.palette.shell();
            }
            // A broken parapet round the top, with gaps to climb out through
            boolean post = (dx + dz) % 2 == 0;
            boolean standing = b.chance(0.7F);
            return y == topY + 1 && post && standing ? b.palette.surfaceWall() : air;
        }
        if (ring == 2) {
            return y <= topY ? b.palette.wall(b.random) : air;
        }
        if (ring == 0) {
            if (y <= topY) {
                return b.palette.trim();
            }
            return y == topY + 1 ? b.palette.light(false) : air;
        }
        int cell = cellOf(dx, dz);
        int step = y - 1;
        if (y <= topY && Math.floorMod(step + start, RING.length) == cell) {
            int[] from = RING[(cell + RING.length - 1) % RING.length];
            int[] to = RING[cell];
            Direction facing = Direction.fromVector(to[0] - from[0], 0, to[1] - from[1]);
            return Blocks.STONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, facing == null ? Direction.NORTH : facing);
        }
        if (y > 4 && (y - 4) % 12 == 0 && y < topY - 2 && Math.floorMod(start + 4, RING.length) == cell) {
            return b.palette.light(true);
        }
        return air;
    }

    private static int cellOf(int dx, int dz) {
        for (int i = 0; i < RING.length; i++) {
            if (RING[i][0] == dx && RING[i][1] == dz) {
                return i;
            }
        }
        return -1;
    }
}
