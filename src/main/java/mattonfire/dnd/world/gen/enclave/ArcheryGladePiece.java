package mattonfire.dnd.world.gen.enclave;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.Direction;

/**
 * The archery glade: three targets on hay bales along the south side of a clearing, a fletching table and
 * a barrel of arrows on the north side. The Fletcher keeps to it.
 */
public class ArcheryGladePiece extends EnclavePiece {
    static final int REACH = 4;

    public ArcheryGladePiece(int x, int y, int z, long seed) {
        super(ElvenEnclaveStructures.GLADE, box(x, y, z, -REACH, -1, -REACH, REACH, 6, REACH), x, y, z, seed);
    }

    public ArcheryGladePiece(NbtCompound nbt) {
        super(ElvenEnclaveStructures.GLADE, nbt);
    }

    @Override
    protected void build(Builder b) {
        b.clearForest(-REACH, 1, -REACH, REACH, 12, REACH);
        b.lawn(-REACH, -REACH, REACH, REACH, 5);
        for (int x = -REACH; x <= REACH; x++) {
            for (int z = -REACH; z <= REACH; z++) {
                if ((Math.abs(x) == REACH || Math.abs(z) == REACH) && this.noise(x, 1, z) < 0.35F) {
                    b.flower(x, 1, z);
                }
            }
        }
        // Targets on hay bales.
        for (int x = -3; x <= 3; x += 3) {
            b.set(x, 1, 3, Blocks.HAY_BLOCK);
            b.set(x, 2, 3, Blocks.TARGET);
        }
        // The shooting line.
        for (int x = -3; x <= 3; x++) {
            b.set(x, 0, -2, Blocks.COARSE_DIRT);
        }
        b.set(-3, 1, -3, Blocks.FLETCHING_TABLE);
        b.container(3, 1, -3, facing(Blocks.BARREL, Direction.UP), TALAN_LOOT);
        b.set(0, 1, -4, Blocks.BIRCH_FENCE);
        b.set(0, 2, -4, lantern(false));
        b.elf(0, 1, -1, ModEntityTypes.ELF_FLETCHER);
        b.elf(2, 1, 0, ModEntityTypes.ELF_WARDEN);
    }
}
