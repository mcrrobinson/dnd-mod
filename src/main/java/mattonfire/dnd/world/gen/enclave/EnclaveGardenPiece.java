package mattonfire.dnd.world.gen.enclave;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Blocks;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.Direction;

/** A flower garden: beds of woodland flowers, sweet berry bushes and sometimes (40%) a beehive on a post. */
public class EnclaveGardenPiece extends EnclavePiece {
    static final int REACH = 3;

    public EnclaveGardenPiece(int x, int y, int z, long seed) {
        super(ElvenEnclaveStructures.GARDEN, box(x, y, z, -REACH, -1, -REACH, REACH, 5, REACH), x, y, z, seed);
    }

    public EnclaveGardenPiece(NbtCompound nbt) {
        super(ElvenEnclaveStructures.GARDEN, nbt);
    }

    @Override
    protected void build(Builder b) {
        b.clearForest(-REACH, 1, -REACH, REACH, 12, REACH);
        b.lawn(-REACH, -REACH, REACH, REACH, 4);
        for (int x = -REACH; x <= REACH; x++) {
            for (int z = -REACH; z <= REACH; z++) {
                // A cross of moss paths through the beds.
                if (x == 0 || z == 0) {
                    b.set(x, 0, z, Blocks.MOSS_BLOCK);
                    continue;
                }
                float n = this.noise(x, 1, z);
                if (n < 0.25F) {
                    b.set(x, 1, z, Blocks.SWEET_BERRY_BUSH.getDefaultState().with(SweetBerryBushBlock.AGE, 3));
                } else if (n < 0.85F) {
                    b.flower(x, 1, z);
                }
            }
        }
        if (b.chance(0.4F)) {
            b.set(0, 1, 0, Blocks.BIRCH_FENCE);
            b.beehive(0, 2, 0, Direction.SOUTH, 2);
        } else {
            b.set(0, 1, 0, Blocks.POTTED_FLOWERING_AZALEA_BUSH);
        }
        b.elf(1, 1, 0, ModEntityTypes.WOOD_ELF);
    }
}
