package mattonfire.dnd.world.gen.enclave;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

/**
 * A rope bridge between two platforms, one block wide: birch slabs with fence handrails, sagging one block
 * in the middle (in half-slab steps, so it can be walked without jumping). The walkway runs along a
 * 4-connected line of cells at the bridge deck's height; at each end the platform's rail is cut away
 * where the bridge lands. Cells are in world x/z; the origin is (0, deck, 0).
 */
public class BridgePiece extends EnclavePiece {
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
        for (int i = 0; i < count; i++) {
            int x = this.cells[2 * i];
            int z = this.cells[2 * i + 1];
            int sag = sag(i, count);
            // Level 0: a top slab at deck height, flush with the platforms; 1: a bottom slab; 2: a top slab one lower.
            int slabY = sag == 2 ? -1 : 0;
            int railY = sag == 2 ? 0 : 1;
            b.air(x, slabY + 1, z, x, slabY + 3, z);
            b.set(x, slabY, z, slab(Blocks.BIRCH_SLAB, sag != 1));
            for (Direction d : Direction.Type.HORIZONTAL) {
                int nx = x + d.getOffsetX();
                int nz = z + d.getOffsetZ();
                long key = ChunkPos.toLong(nx, nz);
                if (walkway.contains(key) || landing.contains(key)) {
                    continue;
                }
                b.set(nx, railY, nz, Blocks.BIRCH_FENCE);
                b.air(nx, railY + 1, nz, nx, railY + 2, nz);
                // Lamps every few cells, and chains hanging under the rails like ropes.
                if (i % 6 == 3) {
                    b.set(nx, railY + 1, nz, lantern(false));
                } else if (i % 3 == 1) {
                    b.set(nx, railY - 1, nz, Blocks.CHAIN);
                }
            }
        }
    }
}
