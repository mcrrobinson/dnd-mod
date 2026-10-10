package mattonfire.dnd.world.gen.dungeon;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.RuneBlock;
import mattonfire.dnd.classes.Blocks.TrapBlocks;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.dungeon.PuzzleRooms;
import mattonfire.dnd.dungeon.PuzzleState;
import mattonfire.dnd.dungeon.RoomRole;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The rune-pillar puzzle room (design 2.7). A rune pillar stands in each corner of the floor, its
 * {@code dndclasses:rune_block} facing in, and the two walls of that corner carry a terracotta mural of
 * the glyph the pillar must show. One set pillar's murals are defaced (rough cobblestone): the riddle book
 * on the lectern in the middle names that glyph. At Challenge I one pillar is free (bare black murals,
 * any glyph will do). The far doorway is shut with ward seals until the puzzle is solved; flame vents in
 * front of the pillars punish a wrong answer. Runtime: {@link PuzzleRooms}; data: {@link PuzzleState} in
 * the room's ward.
 */
public class PuzzleRoomPiece extends DungeonPiece {
    /** How far the pillars stand from the middle, on both axes. */
    private static final int PILLAR_OFFSET = 3;
    /** Pillar corners in solution order: clockwise from the north-west. */
    private static final int[][] CORNERS = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};

    public PuzzleRoomPiece(Info info, BlockBox box, long seed, int roomId, RoomRole role, int height, List<Door> doors) {
        super(DungeonStructures.PUZZLE_ROOM, info, box, seed, roomId, role, height, doors);
    }

    public PuzzleRoomPiece(NbtCompound nbt) {
        super(DungeonStructures.PUZZLE_ROOM, nbt);
    }

    @Override
    protected void build(Builder b) {
        this.room(b);
        int w = this.width();
        int d = this.depth();
        int mx = w / 2;
        int mz = d / 2;
        int[] solution = PuzzleState.rollSolution(b.random, this.tier());
        int defaced = PuzzleState.rollDefaced(b.random, solution);

        List<BlockPos> pillars = new ArrayList<>();
        List<BlockPos> vents = new ArrayList<>();
        for (int i = 0; i < CORNERS.length; i++) {
            int sx = CORNERS[i][0];
            int sz = CORNERS[i][1];
            int px = mx + sx * PILLAR_OFFSET;
            int pz = mz + sz * PILLAR_OFFSET;
            b.set(px, 1, pz, b.palette.trim());
            pillars.add(b.pos(px, 2, pz));
            b.set(px, 3, pz, b.palette.accent());
            // A flame vent in the floor between the pillar and the middle.
            int vx = mx + sx * (PILLAR_OFFSET - 1);
            int vz = mz + sz * (PILLAR_OFFSET - 1);
            b.set(vx, 0, vz, TrapBlocks.FLAME_VENT.getDefaultState());
            vents.add(b.pos(vx, 0, vz));
            this.mural(b, sx, sz, solution[i], i == defaced);
        }

        // The far doorway (the main path's way on) is sealed until the runes are right.
        List<BlockPos> seal = new ArrayList<>();
        if (this.doors.size() > 1) {
            for (int[] column : b.doorwayColumns(this.doors.get(1), 1)) {
                for (int y = 1; y <= DOOR_HEIGHT; y++) {
                    b.set(column[0], y, column[1], ModBlocks.ARCANE_SEAL.getDefaultState());
                    seal.add(b.pos(column[0], y, column[1]));
                }
            }
        }

        Direction entry = this.doors.isEmpty() ? Direction.NORTH : this.doors.get(0).side();
        BlockPos lectern = b.pos(mx, 1, mz);
        PuzzleState puzzle = new PuzzleState(solution, defaced, pillars, seal, vents, lectern);
        int[] start = puzzle.shuffled(b.random);
        for (int i = 0; i < CORNERS.length; i++) {
            Direction facing = CORNERS[i][0] < 0 ? Direction.EAST : Direction.WEST;
            b.set(mx + CORNERS[i][0] * PILLAR_OFFSET, 2, mz + CORNERS[i][1] * PILLAR_OFFSET,
                    RuneBlock.state(ModBlocks.RUNE_BLOCK, facing, start[i]));
        }
        b.set(mx, 1, mz, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, entry).with(LecternBlock.HAS_BOOK, true));
        if (b.chunkBox.contains(lectern) && b.world.getBlockEntity(lectern) instanceof LecternBlockEntity be) {
            be.setBook(PuzzleRooms.riddleBook(this.theme(), puzzle));
        }
        b.light(mx, this.height, mz, true);
        b.puzzle = puzzle;
    }

    /**
     * The glyph painted on both walls of the corner at (sx, sz): a 3x3 terracotta pattern on black, 3-5
     * blocks up, running from the corner towards the middle of each wall. Defaced murals are rough
     * cobblestone; a free pillar's are bare black.
     */
    private void mural(Builder b, int sx, int sz, int glyph, boolean defaced) {
        int w = this.width();
        int d = this.depth();
        int wallX = sx < 0 ? 1 : w - 2;
        int wallZ = sz < 0 ? 1 : d - 2;
        int cornerX = sx < 0 ? 2 : w - 3;
        int cornerZ = sz < 0 ? 2 : d - 3;
        for (int row = 0; row < 3; row++) {
            int y = 5 - row;
            for (int col = 0; col < 3; col++) {
                BlockState paint = this.muralBlock(glyph, defaced, row, col);
                // On the west/east wall, running along z from the corner.
                b.set(wallX, y, cornerZ - sz * col, paint);
                // On the north/south wall, running along x.
                b.set(cornerX - sx * col, y, wallZ, paint);
            }
        }
    }

    private BlockState muralBlock(int glyph, boolean defaced, int row, int col) {
        if (defaced) {
            return ((row + col) % 2 == 0 ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE).getDefaultState();
        }
        if (glyph == PuzzleState.FREE) {
            return Blocks.BLACK_TERRACOTTA.getDefaultState();
        }
        RuneBlock.Glyph g = RuneBlock.Glyph.of(glyph);
        return (g.painted(row, col) ? g.paint() : Blocks.BLACK_TERRACOTTA).getDefaultState();
    }
}
