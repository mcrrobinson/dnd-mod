package mattonfire.dnd.world.gen.stronghold;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The smithy on the side of the yard: an open-sided shed under a dark oak roof, a blackstone forge against
 * the back wall with a blast furnace, a furnace and a channel of lava, an anvil, a grindstone, a smithing
 * table, an armour stand, and the armoury chest; a heap of Smithy Slag outside the forge end. The Orc
 * Quartermaster trades here.
 */
public class SmithyPiece extends StrongholdPiece {
    static final int A1 = 12;
    static final int A2 = 19;
    static final int B1 = -5;
    static final int B2 = 5;
    /** How far past the side the slag heap reaches. */
    static final int SLAG_B = 2;

    public SmithyPiece(BlockPos center, Direction gate, long seed, long hold) {
        super(OrcStrongholdStructures.SMITHY, center, gate, seed, hold, A1, -1, B1, A2, 6, B2 + SLAG_B);
    }

    public SmithyPiece(NbtCompound nbt) {
        super(OrcStrongholdStructures.SMITHY, nbt);
    }

    @Override
    protected void build(Builder b) {
        // Floor and roof.
        for (int x = A1; x <= A2; x++) {
            for (int z = B1; z <= B2; z++) {
                b.set(x, 0, z, b.chance(0.3F) ? Blocks.COBBLED_DEEPSLATE : Blocks.PACKED_MUD);
                b.set(x, 5, z, slab(Blocks.DARK_OAK_SLAB, false));
            }
        }
        // Corner posts.
        for (int x : new int[]{A1, A2}) {
            for (int z : new int[]{B1, B2}) {
                b.fill(x, 1, z, x, 4, z, pillar(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
            }
        }
        // The forge: a blackstone back wall, the furnaces and a lava channel in front of it.
        b.fill(A2, 1, B1 + 1, A2, 4, B2 - 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.set(A2, 2, 0, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        b.set(A2 - 1, 1, -2, facing(Blocks.BLAST_FURNACE, Direction.WEST));
        b.set(A2 - 1, 1, 2, facing(Blocks.FURNACE, Direction.WEST));
        b.fill(A2 - 1, -1, -1, A2 - 1, -1, 1, Blocks.BLACKSTONE);
        b.fill(A2 - 1, 0, -1, A2 - 1, 0, 1, Blocks.LAVA);
        b.fill(A2 - 2, 0, -1, A2 - 2, 0, 1, Blocks.POLISHED_BLACKSTONE);
        b.fill(A2 - 2, 1, -1, A2 - 2, 1, 1, slab(Blocks.POLISHED_BLACKSTONE_SLAB, false));
        b.set(A2 - 1, 1, -1, Blocks.IRON_BARS);
        b.set(A2 - 1, 1, 0, Blocks.IRON_BARS);
        b.set(A2 - 1, 1, 1, Blocks.IRON_BARS);
        b.set(A2 - 1, 0, -2, Blocks.BLACKSTONE);
        b.set(A2 - 1, 0, 2, Blocks.BLACKSTONE);
        // Tools of the trade.
        b.set(A1 + 3, 1, -3, facing(Blocks.ANVIL, Direction.NORTH));
        b.set(A1 + 3, 1, 3, Blocks.GRINDSTONE.getDefaultState().with(net.minecraft.block.GrindstoneBlock.FACE,
                net.minecraft.block.enums.WallMountLocation.FLOOR));
        b.set(A1 + 1, 1, 3, Blocks.SMITHING_TABLE);
        b.container(A1 + 1, 1, -4, facing(Blocks.CHEST, Direction.EAST), ARMORY_LOOT);
        b.set(A1 + 2, 1, -4, facing(Blocks.BARREL, Direction.UP));
        b.set(A2 - 1, 4, 0, Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true));

        // The slag heap outside the forge end: two layers of lumps tipped out of the furnaces.
        for (int x = A2 - 3; x <= A2; x++) {
            for (int z = B2 + 1; z <= B2 + SLAG_B; z++) {
                if (x == A2 - 3 && z == B2 + SLAG_B || b.chance(0.15F)) {
                    continue;
                }
                b.set(x, 1, z, ModBlockFamilies.SMITHY_SLAG.base());
                if (x >= A2 - 1 && z == B2 + 1) {
                    b.set(x, 2, z, ModBlockFamilies.SMITHY_SLAG.base());
                }
            }
        }

        b.orc(A1 + 3, 1, 0, ModEntityTypes.ORC_QUARTERMASTER, b.pos(A1 + 3, 1, 0), 4);
    }
}
