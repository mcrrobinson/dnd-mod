package mattonfire.dnd.world.gen.enclave;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import mattonfire.dnd.classes.Blocks.elven.WovenRopePlankBlock;
import mattonfire.dnd.classes.Registry.ElvenBlocks;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

/**
 * A rope bridge between two platforms, one block wide: a deck of Woven Rope Planks with fence handrails, sagging
 * one block in the middle (in half-block steps, like slabs, so it can be walked without jumping). The walkway
 * runs along a 4-connected line of cells at the bridge deck's height; at each end the platform's rail is cut
 * away where the bridge lands. Cells are in world x/z; the origin is (0, deck, 0).
 *
 * <p>The handrail stands on a stripped Heartwood stringer flush with the deck on each side, like the Heart Tree
 * stair's rail. Its posts are placed already joined up, turn the corner where the walkway steps sideways, and stack
 * two high where the rail drops a level at the sag, so each side reads as one continuous rail.
 */
public class BridgePiece extends EnclavePiece {
    /** The beam under the handrail. */
    private static final Block STRINGER = ModBlockFamilies.STRIPPED_HEARTWOOD_LOG.base();

    /** World x, z of each walkway cell, in order. */
    private final int[] cells;
    /** World x, z of the platform cells the bridge lands on, whose rail is cut away. */
    private final int[] landings;

    public BridgePiece(int[] cells, int[] landings, int deck, long seed) {
        super(ElvenEnclaveStructures.BRIDGE, bounds(cells, landings, deck), 0, deck, 0, seed);
        this.cells = cells;
        this.landings = landings;
    }

    public BridgePiece(NbtCompound nbt) {
        super(ElvenEnclaveStructures.BRIDGE, nbt);
        this.cells = nbt.getIntArray("Cells");
        this.landings = nbt.getIntArray("Landings");
    }

    private static BlockBox bounds(int[] cells, int[] landings, int deck) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int[] list : new int[][]{cells, landings}) {
            for (int i = 0; i < list.length; i += 2) {
                minX = Math.min(minX, list[i]);
                maxX = Math.max(maxX, list[i]);
                minZ = Math.min(minZ, list[i + 1]);
                maxZ = Math.max(maxZ, list[i + 1]);
            }
        }
        return new BlockBox(minX - 1, deck - 2, minZ - 1, maxX + 1, deck + 4, maxZ + 1);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putIntArray("Cells", this.cells);
        nbt.putIntArray("Landings", this.landings);
    }

    /** How many half-blocks cell {@code i} of {@code count} sags below the deck (0-2). */
    static int sag(int i, int count) {
        int most = count >= 6 ? 2 : count >= 3 ? 1 : 0;
        return (int) Math.round(most * Math.sin(Math.PI * (i + 0.5D) / count));
    }

    @Override
    protected void build(Builder b) {
        int count = this.cells.length / 2;
        Set<Long> walkway = new HashSet<>();
        Set<Long> landing = new HashSet<>();
        for (int i = 0; i < this.cells.length; i += 2) {
            walkway.add(ChunkPos.toLong(this.cells[i], this.cells[i + 1]));
        }
        for (int i = 0; i < this.landings.length; i += 2) {
            landing.add(ChunkPos.toLong(this.landings[i], this.landings[i + 1]));
            // Cut the platform's rail.
            b.air(this.landings[i], 1, this.landings[i + 1], this.landings[i], 3, this.landings[i + 1]);
        }
        // The deck, and the rail cells either side of it: each at the height of the highest deck cell beside it.
        Map<Long, Rail> rails = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            int x = this.cells[2 * i];
            int z = this.cells[2 * i + 1];
            int sag = sag(i, count);
            // Level 0: a top-half plank at deck height, flush with the platforms; 1: a bottom-half one; 2: a top-half
            // one a block lower.
            int slabY = sag == 2 ? -1 : 0;
            int railY = sag == 2 ? 0 : 1;
            b.air(x, slabY + 1, z, x, slabY + 3, z);
            b.set(x, slabY, z, this.plank(i, count, sag != 1));
            for (Direction d : Direction.Type.HORIZONTAL) {
                long key = ChunkPos.toLong(x + d.getOffsetX(), z + d.getOffsetZ());
                if (walkway.contains(key) || landing.contains(key)) {
                    continue;
                }
                Rail rail = rails.get(key);
                if (rail == null || rail.y < railY) {
                    rails.put(key, new Rail(railY, i, this.axis(i, count)));
                }
            }
        }
        // Where the walkway steps sideways, the rail on the outside of the step has a gap at the corner: two posts
        // that only touch diagonally. Fill the corner so the rail turns with the walkway.
        Map<Long, Rail> corners = new HashMap<>();
        for (Map.Entry<Long, Rail> entry : rails.entrySet()) {
            int x = ChunkPos.getPackedX(entry.getKey());
            int z = ChunkPos.getPackedZ(entry.getKey());
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    Rail other = rails.get(ChunkPos.toLong(x + dx, z + dz));
                    if (other == null) {
                        continue;
                    }
                    long a = ChunkPos.toLong(x + dx, z);
                    long c = ChunkPos.toLong(x, z + dz);
                    long free = walkway.contains(a) ? c : walkway.contains(c) ? a : Long.MAX_VALUE;
                    if (free != Long.MAX_VALUE && !walkway.contains(free) && !landing.contains(free) && !rails.containsKey(free)) {
                        int y = Math.max(entry.getValue().y, other.y);
                        corners.merge(free, new Rail(y, entry.getValue().index, Direction.Axis.Y), (p, q) -> p.y >= q.y ? p : q);
                    }
                }
            }
        }
        rails.putAll(corners);
        // Every fence post: one on each rail cell, and where the rail drops a level (the sag), a second post on top of
        // the lower one so the two parts join.
        Set<BlockPos> fences = new HashSet<>();
        for (Map.Entry<Long, Rail> entry : rails.entrySet()) {
            int x = ChunkPos.getPackedX(entry.getKey());
            int z = ChunkPos.getPackedZ(entry.getKey());
            int y = entry.getValue().y;
            fences.add(new BlockPos(x, y, z));
            for (Direction d : Direction.Type.HORIZONTAL) {
                Rail next = rails.get(ChunkPos.toLong(x + d.getOffsetX(), z + d.getOffsetZ()));
                if (next != null && next.y > y) {
                    fences.add(new BlockPos(x, next.y, z));
                }
            }
        }
        for (Map.Entry<Long, Rail> entry : rails.entrySet()) {
            int x = ChunkPos.getPackedX(entry.getKey());
            int z = ChunkPos.getPackedZ(entry.getKey());
            Rail rail = entry.getValue();
            int top = fences.contains(new BlockPos(x, rail.y + 1, z)) ? rail.y + 1 : rail.y;
            // A stripped Heartwood stringer flush with the deck under each post (like the Heart Tree stair's rail).
            b.set(x, rail.y - 1, z, pillar(STRINGER, rail.axis));
            b.air(x, rail.y, z, x, top + 2, z);
            for (int y = rail.y; y <= top; y++) {
                b.set(x, y, z, fence(fences, x, y, z));
            }
            // Lamps every few cells, and chains hanging under the stringer like ropes.
            if (rail.index % 6 == 3) {
                b.set(x, top + 1, z, lantern(false));
            } else if (rail.index % 3 == 1) {
                b.set(x, rail.y - 2, z, Blocks.CHAIN);
            }
        }
    }

    /** A birch fence post at x/y/z, joined to the posts beside it ({@code fences}) without waiting on neighbour updates. */
    private static BlockState fence(Set<BlockPos> fences, int x, int y, int z) {
        return Blocks.BIRCH_FENCE.getDefaultState()
                .with(FenceBlock.NORTH, fences.contains(new BlockPos(x, y, z - 1)))
                .with(FenceBlock.SOUTH, fences.contains(new BlockPos(x, y, z + 1)))
                .with(FenceBlock.WEST, fences.contains(new BlockPos(x - 1, y, z)))
                .with(FenceBlock.EAST, fences.contains(new BlockPos(x + 1, y, z)));
    }

    /** A rail cell: its post's height, the walkway cell it's beside, and the way its stringer runs. */
    private record Rail(int y, int index, Direction.Axis axis) {
    }

    /** The deck plank for cell {@code i}, in the top or bottom half, lying along the way the walkway runs there. */
    private BlockState plank(int i, int count, boolean top) {
        return ElvenBlocks.WOVEN_ROPE_PLANK.getDefaultState()
                .with(WovenRopePlankBlock.HALF, top ? BlockHalf.TOP : BlockHalf.BOTTOM)
                .with(WovenRopePlankBlock.AXIS, this.axis(i, count));
    }

    /** The way the walkway runs at cell {@code i}: towards the next cell (the last cell, from the one before). */
    private Direction.Axis axis(int i, int count) {
        int j = i + 1 < count ? i + 1 : i - 1;
        boolean alongX = j >= 0 && this.cells[2 * j] != this.cells[2 * i];
        return alongX ? Direction.Axis.X : Direction.Axis.Z;
    }
}
