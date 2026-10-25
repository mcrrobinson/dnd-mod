package mattonfire.dnd.world.underdark.drow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

/**
 * A web bridge from one tower's platform to the next's: one block wide, Web Bridge Planks (flush with the platforms,
 * which are all at the same height), straight from middle to middle as a four-connected line, through a gap cut in
 * each parapet. Cobweb handrails on alternate blocks, cobwebs hanging under its middle third where it sags, and Faerie
 * Fire Lanterns hanging under it a quarter of the way from each end. It cuts through any rock in its way, with three
 * blocks of air over the deck. The piece's centre is the first tower's (at its foot); y is the platform's height.
 */
public class DrowBridgePiece extends UnderdarkPiece {
    private final int dx;
    private final int dz;
    private final int platform;

    public DrowBridgePiece(BlockPos from, BlockPos to, int platform, long seed) {
        super(DrowOutposts.BRIDGE, from, Direction.SOUTH, seed,
                Math.min(0, to.getX() - from.getX()) - DrowTowerPiece.HALF, platform - 3,
                Math.min(0, to.getZ() - from.getZ()) - DrowTowerPiece.HALF,
                Math.max(0, to.getX() - from.getX()) + DrowTowerPiece.HALF, platform + 4,
                Math.max(0, to.getZ() - from.getZ()) + DrowTowerPiece.HALF);
        this.dx = to.getX() - from.getX();
        this.dz = to.getZ() - from.getZ();
        this.platform = platform;
    }

    public DrowBridgePiece(NbtCompound nbt) {
        super(DrowOutposts.BRIDGE, nbt);
        this.dx = nbt.getInt("DX");
        this.dz = nbt.getInt("DZ");
        this.platform = nbt.getInt("Platform");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("DX", this.dx);
        nbt.putInt("DZ", this.dz);
        nbt.putInt("Platform", this.platform);
    }

    /** The middle of the deck, where you stand on it, in the world. */
    public BlockPos middle() {
        List<int[]> cells = line(0, 0, this.dx, this.dz);
        int[] mid = cells.get(cells.size() / 2);
        return this.at(mid[0], this.platform + 1, mid[1]);
    }

    /** The deck's blocks out in the open (past both parapets), in the world: the survey checks they're all there. */
    public List<BlockPos> deck() {
        List<BlockPos> deck = new ArrayList<>();
        for (int[] cell : line(0, 0, this.dx, this.dz)) {
            if (this.cheb(cell, 0, 0) > 3 && this.cheb(cell, this.dx, this.dz) > 3) {
                deck.add(this.at(cell[0], this.platform, cell[1]));
            }
        }
        return deck;
    }

    /** The four-connected line of cells from (x0, z0) to (x1, z1), both included. */
    static List<int[]> line(int x0, int z0, int x1, int z1) {
        List<int[]> cells = new ArrayList<>();
        int ax = Math.abs(x1 - x0);
        int az = Math.abs(z1 - z0);
        int sx = Integer.signum(x1 - x0);
        int sz = Integer.signum(z1 - z0);
        int x = x0;
        int z = z0;
        int e = 0;
        cells.add(new int[] {x, z});
        for (int i = 0; i < ax + az; i++) {
            int ex = e + az;
            int ez = e - ax;
            if (Math.abs(ex) < Math.abs(ez)) {
                x += sx;
                e = ex;
            } else {
                z += sz;
                e = ez;
            }
            cells.add(new int[] {x, z});
        }
        return cells;
    }

    @Override
    protected void build(Builder b) {
        int p = this.platform;
        List<int[]> cells = line(0, 0, this.dx, this.dz);
        Set<Long> deck = new HashSet<>();
        for (int[] cell : cells) {
            deck.add(ChunkPos.toLong(cell[0], cell[1]));
        }
        int[] side = Math.abs(this.dx) > Math.abs(this.dz) ? new int[] {0, 1} : new int[] {1, 0};
        // The stretch that's out in the open (past both towers' parapets).
        int first = -1;
        int last = -1;
        for (int i = 0; i < cells.size(); i++) {
            int[] cell = cells.get(i);
            if (this.cheb(cell, 0, 0) > 3 && this.cheb(cell, this.dx, this.dz) > 3) {
                if (first < 0) {
                    first = i;
                }
                last = i;
            }
        }
        for (int i = 0; i < cells.size(); i++) {
            int[] cell = cells.get(i);
            int a = cell[0];
            int c = cell[1];
            int fromA = this.cheb(cell, 0, 0);
            int fromB = this.cheb(cell, this.dx, this.dz);
            if (fromA <= 2 || fromB <= 2) {
                continue; // on a platform
            }
            b.clear(a, c, p + 1, p + 3);
            if (fromA == 3 || fromB == 3) {
                continue; // the gap in the parapet; the platform's rim is the deck here
            }
            b.solidify(a, p, c, UnderdarkBlocks.WEB_BRIDGE.getDefaultState());
            if (first < 0) {
                continue;
            }
            int along = i - first;
            int span = last - first + 1;
            if (along % 2 == 0 && fromA > 4 && fromB > 4) {
                for (int s : new int[] {-1, 1}) {
                    int ra = a + side[0] * s;
                    int rc = c + side[1] * s;
                    if (!deck.contains(ChunkPos.toLong(ra, rc))) {
                        b.solidify(ra, p + 1, rc, Blocks.COBWEB.getDefaultState());
                    }
                }
            }
            if (along > span / 3 && along < span * 2 / 3 && along % 2 == 1) {
                b.solidify(a, p - 1, c, Blocks.COBWEB.getDefaultState());
            }
            if (along == span / 4 || along == span * 3 / 4) {
                b.hangingLantern(a, p - 1, c, 1, UnderdarkBlocks.FAERIE_LANTERN);
            }
        }
    }

    private int cheb(int[] cell, int ox, int oz) {
        return Math.max(Math.abs(cell[0] - ox), Math.abs(cell[1] - oz));
    }
}
