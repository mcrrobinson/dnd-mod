package mattonfire.dnd.world.gen.dungeon;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.TrapBlocks;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.classes.Blocks.TrapTriggerBlockEntity;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.enums.Thickness;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * A long, narrow hall the path runs straight through, 3 wide inside, with 3-5 traps along it (at least two
 * kinds; see {@link TrapTriggerBlockEntity}):
 * <ul>
 * <li><b>Dart</b>: a hidden pressure tile, and a launcher in the side wall two rows ahead (or behind).</li>
 * <li><b>Flame</b>: a row of trigger + two flame vents across the hall.</li>
 * <li><b>Pit</b>: a trigger tile in the middle of a row, then two rows of crumbling floor over a 4-deep pit
 * of pointed dripstone, lined in the waterproof shell block, with a ladder up its far wall.</li>
 * </ul>
 * Traps go on rows 3-12 of the 16 (counted from the entrance door), with at least one plain row between two
 * traps, so the doorways are always safe to stand in.
 */
public class TrapCorridorPiece extends DungeonPiece {
    /** First and last trap row, counted from the entrance end of the box. */
    private static final int FIRST_ROW = 3;
    private static final int LAST_ROW = 12;
    /** Chance that a locked chest at Challenge II or higher has a poison needle. */
    static final float NEEDLE_CHANCE = 0.35F;
    private static final TrapKind[] FLOOR_TRAPS = {TrapKind.DART, TrapKind.FLAME, TrapKind.PIT};

    public TrapCorridorPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.TRAP_CORRIDOR, info, box, seed, roomId, role, height, doors);
    }

    public TrapCorridorPiece(NbtCompound nbt) {
        super(DungeonStructures.TRAP_CORRIDOR, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        boolean alongX = this.width() > this.depth();
        int length = alongX ? this.width() : this.depth();
        for (int i = 4; i < length - 3; i += 7) {
            if (alongX) {
                b.light(i, this.height, this.depth() / 2, true);
            } else {
                b.light(this.width() / 2, this.height, i, true);
            }
        }
        this.traps(b);
    }

    /** Local coordinates by row (along the hall from the entrance) and column (across it). */
    private final class Frame {
        final boolean alongX = TrapCorridorPiece.this.width() > TrapCorridorPiece.this.depth();
        final int length = this.alongX ? TrapCorridorPiece.this.width() : TrapCorridorPiece.this.depth();
        /** The wall the party comes in through (the first door is the one from the room before). */
        final Direction entry;
        final boolean reversed;

        Frame() {
            Direction side = TrapCorridorPiece.this.doors.isEmpty() ? null : TrapCorridorPiece.this.doors.get(0).side();
            if (side == null || (side.getAxis() == Direction.Axis.X) != this.alongX) {
                side = this.alongX ? Direction.WEST : Direction.NORTH;
            }
            this.entry = side;
            this.reversed = side == Direction.EAST || side == Direction.SOUTH;
        }

        int x(int row, int col) {
            return this.alongX ? (this.reversed ? this.length - 1 - row : row) : col;
        }

        int z(int row, int col) {
            return this.alongX ? col : (this.reversed ? this.length - 1 - row : row);
        }

        void set(Builder b, int row, int col, int y, BlockState state) {
            b.set(this.x(row, col), y, this.z(row, col), state);
        }

        BlockPos pos(Builder b, int row, int col, int y) {
            return b.pos(this.x(row, col), y, this.z(row, col));
        }

        /** The way a block in side wall column {@code col} (1 or 5) faces into the hall. */
        Direction inward(int col) {
            Direction plus = this.alongX ? Direction.SOUTH : Direction.EAST;
            return col <= 3 ? plus : plus.getOpposite();
        }
    }

    private static int footprint(TrapKind kind) {
        return kind == TrapKind.PIT ? 3 : 1;
    }

    private static int rows(List<TrapKind> kinds) {
        int rows = kinds.size() - 1;
        for (TrapKind kind : kinds) {
            rows += footprint(kind);
        }
        return rows;
    }

    private void traps(Builder b) {
        Frame f = new Frame();
        Random random = b.random;
        int count = 3 + random.nextInt(3);
        List<TrapKind> kinds = new ArrayList<>();
        int first = random.nextInt(FLOOR_TRAPS.length);
        kinds.add(FLOOR_TRAPS[first]);
        kinds.add(FLOOR_TRAPS[(first + 1 + random.nextInt(FLOOR_TRAPS.length - 1)) % FLOOR_TRAPS.length]);
        while (kinds.size() < count) {
            TrapKind kind = FLOOR_TRAPS[random.nextInt(FLOOR_TRAPS.length)];
            if (kind == TrapKind.PIT && kinds.contains(TrapKind.PIT)) {
                kind = random.nextBoolean() ? TrapKind.DART : TrapKind.FLAME;
            }
            kinds.add(kind);
        }
        int space = LAST_ROW - FIRST_ROW + 1;
        while (rows(kinds) > space) {
            kinds.remove(kinds.size() - 1);
        }
        for (int i = kinds.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            TrapKind t = kinds.get(i);
            kinds.set(i, kinds.get(j));
            kinds.set(j, t);
        }
        int slack = space - rows(kinds);
        int row = FIRST_ROW;
        for (TrapKind kind : kinds) {
            int extra = random.nextInt(Math.min(slack, 2) + 1);
            slack -= extra;
            row += extra;
            switch (kind) {
                case DART -> this.dart(b, f, row, random);
                case FLAME -> this.flame(b, f, row);
                case PIT -> this.pit(b, f, row);
                default -> {
                }
            }
            row += footprint(kind) + 1;
        }
    }

    private void dart(Builder b, Frame f, int row, Random random) {
        int col = 2 + random.nextInt(3);
        int wall = random.nextBoolean() ? 1 : 5;
        int launcherRow = row + 2 <= LAST_ROW ? row + 2 : row - 2;
        f.set(b, launcherRow, wall, 2, TrapBlocks.DART_LAUNCHER.getDefaultState().with(HorizontalFacingBlock.FACING, f.inward(wall)));
        place(b, f.pos(b, row, col, 0), TrapKind.DART, this.info, List.of(f.pos(b, launcherRow, wall, 2)), List.of(), null, f.entry);
    }

    private void flame(Builder b, Frame f, int row) {
        f.set(b, row, 2, 0, TrapBlocks.FLAME_VENT.getDefaultState());
        f.set(b, row, 4, 0, TrapBlocks.FLAME_VENT.getDefaultState());
        place(b, f.pos(b, row, 3, 0), TrapKind.FLAME, this.info, List.of(f.pos(b, row, 2, 0), f.pos(b, row, 4, 0)), List.of(),
                null, f.entry);
    }

    private void pit(Builder b, Frame f, int row) {
        BlockState shell = b.palette.shell();
        BlockState spike = Blocks.POINTED_DRIPSTONE.getDefaultState()
                .with(PointedDripstoneBlock.VERTICAL_DIRECTION, Direction.UP)
                .with(PointedDripstoneBlock.THICKNESS, Thickness.TIP);
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, f.entry);
        int ladderRow = row + 2;
        List<BlockPos> crumbles = new ArrayList<>();
        for (int r = row; r <= row + 3; r++) {
            for (int c = 1; c <= 5; c++) {
                boolean open = r > row && r < row + 3 && c >= 2 && c <= 4;
                for (int y = -5; y <= -1; y++) {
                    BlockState state;
                    if (!open || y == -5) {
                        state = shell;
                    } else if (r == ladderRow && c == 3) {
                        state = ladder;
                    } else {
                        state = y == -4 ? spike : Blocks.AIR.getDefaultState();
                    }
                    f.set(b, r, c, y, state);
                }
                if (open) {
                    f.set(b, r, c, 0, TrapBlocks.CRUMBLING_FLOOR.getDefaultState());
                    crumbles.add(f.pos(b, r, c, 0));
                }
            }
        }
        place(b, f.pos(b, row, 3, 0), TrapKind.PIT, this.info, List.of(), crumbles, f.pos(b, ladderRow, 3, 0), f.entry);
    }

    /** Puts down a trap's trigger tile and records the trap in it (if it's in this chunk). */
    private static void place(Builder b, BlockPos pos, TrapKind kind, Info info, List<BlockPos> linked,
                              List<BlockPos> crumbles, @Nullable BlockPos ladder, Direction ladderFacing) {
        if (!b.chunkBox.contains(pos)) {
            return;
        }
        b.world.setBlockState(pos, TrapBlocks.TRAP_TRIGGER.getDefaultState(), Block.NOTIFY_LISTENERS);
        if (b.world.getBlockEntity(pos) instanceof TrapTriggerBlockEntity trap) {
            trap.setup(kind, info.startKey(), info.tier(), linked, crumbles, ladder, ladderFacing);
        }
    }

    /**
     * A poison needle under the chest at local (x, y, z) (the trigger takes the floor block under it). Called
     * by {@link DungeonPiece.Builder#chest} for some locked chests from Challenge II up.
     */
    static void needle(DungeonPiece piece, Builder b, int x, int y, int z) {
        place(b, b.pos(x, y - 1, z), TrapKind.NEEDLE, piece.info, List.of(b.pos(x, y, z)), List.of(), null, Direction.NORTH);
    }
}
