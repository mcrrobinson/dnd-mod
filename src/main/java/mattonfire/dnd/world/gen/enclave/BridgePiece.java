package mattonfire.dnd.world.gen.enclave;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.elven.HangingRopePlankBlock;
import mattonfire.dnd.classes.Blocks.elven.WovenRopePlankBlock;
import mattonfire.dnd.classes.Registry.ElvenBlocks;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.world.gen.Desolation;
import mattonfire.dnd.world.gen.DesolationLevel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LanternBlock;
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
 *
 * <p>In a desolate enclave a bridge may have fallen ({@link DesolationLevel#bridgesFallen}): see {@link #fall}.
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
        DesolationLevel level = Desolation.level();
        if (level != null && Desolation.roll(this.seed, 0, 0, 0, 40) < level.bridgesFallen) {
            this.fall(b, walkway, landing, rails);
        }
    }

    /**
     * A desolate enclave's bridge that has fallen: it snapped somewhere along its span and each half swung down and
     * hangs from its platform on edge ({@link ElvenBlocks#HANGING_ROPE_PLANK}), with its two rails hanging beside it as
     * chains from the end posts. A plank or two by the snap broke off; the rest of each half hangs as long as it was.
     * Under the whole old span lies a trail of what fell, on the first thing below it (the ground, a canopy, a roof):
     * planks, and beside them lengths of rope, broken posts and the odd lamp. One bridge in 4 snapped at one end only
     * and hangs whole from the other. Only the bridge's own blocks are taken out, never the tree or platform.
     *
     * <p>Every column is decided from the piece's seed, its cells and the blocks in that column, so each chunk places
     * its part of the same fall.
     */
    private void fall(Builder b, Set<Long> walkway, Set<Long> landing, Map<Long, Rail> rails) {
        int count = this.cells.length / 2;
        for (int i = 0; i < count; i++) {
            int x = this.cells[2 * i];
            int z = this.cells[2 * i + 1];
            this.clearBridge(b, x, z);
        }
        // Every rail column, the corner posts where the walkway steps sideways included (those only touch the
        // walkway diagonally).
        for (long key : rails.keySet()) {
            this.clearBridge(b, ChunkPos.getPackedX(key), ChunkPos.getPackedZ(key));
        }
        // Where it snapped: cells below `snap` hang from the first end, the rest from the last.
        float end = Desolation.roll(this.seed, 3, 0, 0, 41);
        int snap;
        if (end < 0.25F || count < 4) {
            snap = end < 0.125F ? 0 : count;
        } else {
            float at = 0.2F + 0.6F * Desolation.roll(this.seed, 1, 0, 0, 41);
            snap = Math.max(1, Math.min(count - 1, Math.round(at * count)));
        }
        if (b.inChunk(this.cells[0], 0, this.cells[1])) {
            DnDClasses.LOGGER.debug("[Desolation] bridge of {} fell, snapped at {}: hangs from {} and {}", count, snap,
                    b.pos(this.cells[0], 0, this.cells[1]).toShortString(),
                    b.pos(this.cells[2 * count - 2], 0, this.cells[2 * count - 1]).toShortString());
        }
        // A plank or two by the snap broke off rather than hang.
        int first = snap;
        int last = count - snap;
        if (snap > 0 && snap < count) {
            first = Math.max(1, snap - 1 - (int) (Desolation.roll(this.seed, 4, 0, 0, 41) * 2));
            last = Math.max(1, count - snap - 1 - (int) (Desolation.roll(this.seed, 5, 0, 0, 41) * 2));
        }
        this.hang(b, 0, 1, first, walkway, landing);
        this.hang(b, count - 1, -1, last, walkway, landing);
        // The rest lies under the old span on the first thing below it (the ground, a canopy, a roof): a trail of
        // planks, most lying where they were, more where a hanging half reached the ground and lies on along it.
        // How far a half reaches is taken from each column's own drop (close to the anchor's), so no chunk needs
        // another's blocks.
        for (int i = 0; i < count; i++) {
            int m = i < snap ? i : count - 1 - i;
            int hung = i < snap ? first : last;
            int x = this.cells[2 * i];
            int z = this.cells[2 * i + 1];
            if (m == 0 && hung > 0 || !b.inChunk(x, 0, z)) {
                continue;
            }
            int y = surfaceY(b, x, z);
            if (y == Integer.MIN_VALUE) {
                continue;
            }
            boolean draped = 1 - y + m <= hung;
            if (Desolation.roll(this.seed, x, y, z, 42) < (draped ? 0.9F : 0.75F)) {
                b.set(x, y, z, this.plank(i, count, false));
            }
        }
        // And bits of the rails beside it: lengths of rope, broken posts, now and then a lamp.
        for (Map.Entry<Long, Rail> entry : rails.entrySet()) {
            int x = ChunkPos.getPackedX(entry.getKey());
            int z = ChunkPos.getPackedZ(entry.getKey());
            if (!b.inChunk(x, 0, z)) {
                continue;
            }
            int y = surfaceY(b, x, z);
            if (y == Integer.MIN_VALUE) {
                continue;
            }
            Rail rail = entry.getValue();
            float roll = Desolation.roll(this.seed, x, y, z, 43);
            Direction.Axis axis = rail.axis() == Direction.Axis.Y ? this.axis(rail.index(), count) : rail.axis();
            if (rail.index() % 6 == 3 && roll < 0.35F) {
                b.set(x, y, z, lantern(false));
            } else if (roll < 0.3F) {
                b.set(x, y, z, Blocks.CHAIN.getDefaultState().with(ChainBlock.AXIS, axis));
            } else if (roll < 0.45F) {
                b.set(x, y, z, Blocks.BIRCH_FENCE);
            }
        }
    }

    /**
     * Hangs {@code length} cells of the bridge from the cell at {@code anchor}, the others being {@code anchor + step},
     * {@code anchor + 2 * step} and so on.
     */
    private void hang(Builder b, int anchor, int step, int length, Set<Long> walkway, Set<Long> landing) {
        if (length <= 0) {
            return;
        }
        int count = this.cells.length / 2;
        int ax = this.cells[2 * anchor];
        int az = this.cells[2 * anchor + 1];
        Direction out = this.outwards(anchor, step, landing);
        // The deck, hanging on edge against the platform's side, ropes up and down; where it reaches the ground or
        // something else under it (leaves, a roof), the next plank lies on top.
        BlockState hanging = ElvenBlocks.HANGING_ROPE_PLANK.getDefaultState().with(HangingRopePlankBlock.FACING, out);
        for (int j = 0; j < length; j++) {
            int y = -j;
            if (!open(b.get(ax, y, az))) {
                break;
            }
            if (j < length - 1 && surface(b.get(ax, y - 1, az))) {
                b.set(ax, y, az, this.plank(anchor, count, false));
                break;
            }
            b.set(ax, y, az, hanging);
        }
        // The rails: the end posts are still standing, with the rails' ropes hanging from them.
        Direction side = out.rotateYClockwise();
        for (Direction d : new Direction[]{side, side.getOpposite()}) {
            int x = ax + d.getOffsetX();
            int z = az + d.getOffsetZ();
            long key = ChunkPos.toLong(x, z);
            if (walkway.contains(key) || landing.contains(key) || !b.inChunk(x, 0, z)) {
                continue;
            }
            if (open(b.get(x, 1, z))) {
                b.set(x, 1, z, Blocks.BIRCH_FENCE);
            }
            for (int j = 0; j < length && open(b.get(x, -j, z)); j++) {
                b.set(x, -j, z, Blocks.CHAIN);
            }
        }
    }

    /** The way from the platform out along the bridge at its end cell {@code anchor}. */
    private Direction outwards(int anchor, int step, Set<Long> landing) {
        int ax = this.cells[2 * anchor];
        int az = this.cells[2 * anchor + 1];
        for (Direction d : Direction.Type.HORIZONTAL) {
            if (landing.contains(ChunkPos.toLong(ax - d.getOffsetX(), az - d.getOffsetZ()))) {
                return d;
            }
        }
        int next = anchor + step;
        if (next >= 0 && next < this.cells.length / 2) {
            Direction d = Direction.fromVector(this.cells[2 * next] - ax, 0, this.cells[2 * next + 1] - az);
            if (d != null) {
                return d;
            }
        }
        return Direction.SOUTH;
    }

    /** Whether something can hang through {@code state}: air or a plant, not liquid. Null (another chunk) can't. */
    private static boolean open(BlockState state) {
        return state != null && (state.isAir() || state.isReplaceable()) && state.getFluidState().isEmpty();
    }

    /** Whether a fallen plank can lie on {@code state}: the ground, leaves, a roof, anything but air, plants and liquid. */
    private static boolean surface(BlockState state) {
        return state != null && !open(state) && state.getFluidState().isEmpty();
    }

    /** The height (below the deck) a plank lies at on the first thing under column x/z, or MIN_VALUE if none. */
    private static int surfaceY(Builder b, int x, int z) {
        if (!open(b.get(x, -1, z))) {
            return Integer.MIN_VALUE;
        }
        for (int y = -1; y > -64; y--) {
            BlockState below = b.get(x, y - 1, z);
            if (below == null || !below.getFluidState().isEmpty()) {
                return Integer.MIN_VALUE;
            }
            if (surface(below)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** Takes the bridge's blocks out of column x/z. */
    private void clearBridge(Builder b, int x, int z) {
        for (int y = -2; y <= 3; y++) {
            if (!b.inChunk(x, y, z)) {
                continue;
            }
            BlockState state = b.get(x, y, z);
            if (state.isOf(ElvenBlocks.WOVEN_ROPE_PLANK) || state.isOf(Blocks.BIRCH_FENCE) || state.isOf(STRINGER)
                    || state.isOf(Blocks.CHAIN) || state.getBlock() instanceof LanternBlock) {
                b.set(x, y, z, Blocks.AIR);
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
