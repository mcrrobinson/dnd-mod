package mattonfire.dnd.world.gen.stronghold;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The Great Longhouse at the back of the hold, the Half-Orc hearth: 21 by 11 under an A-frame roof of spruce
 * and dark oak, with its door facing the pit. Inside: a fire pit in the middle, log benches, war drums (note
 * blocks on hay), a trophy wall of mob heads on the back wall, and at the far end the War Chief's seat of
 * blackstone and gold on a dais, with the hoard chest beside it. The War Chief waits by the seat.
 */
public class LonghousePiece extends StrongholdPiece {
    static final int A = 10;
    static final int FRONT = -8;
    static final int BACK = -18;
    static final int RIDGE = -13;
    private static final Block[] WALL_HEADS = {Blocks.SKELETON_WALL_SKULL, Blocks.SKELETON_WALL_SKULL, Blocks.ZOMBIE_WALL_HEAD,
            Blocks.CREEPER_WALL_HEAD, Blocks.SKELETON_WALL_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL};
    /** The hoard chest, beside the seat. */
    static final int HOARD_A = 3;
    static final int HOARD_B = BACK + 1;
    /** The seat. */
    static final int SEAT_B = BACK + 2;

    public LonghousePiece(BlockPos center, Direction gate, long seed, long hold) {
        super(OrcStrongholdStructures.LONGHOUSE, center, gate, seed, hold, -A, 0, BACK, A, 10, FRONT);
    }

    public LonghousePiece(NbtCompound nbt) {
        super(OrcStrongholdStructures.LONGHOUSE, nbt);
    }

    private static int roofY(int b) {
        return 4 + (5 - Math.abs(b - RIDGE));
    }

    /** Where the hoard chest is, in the world. */
    public BlockPos hoard() {
        return this.at(HOARD_A, 1, HOARD_B);
    }

    /** The floor in front of the seat, in the world. */
    public BlockPos seat() {
        return this.at(0, 1, SEAT_B + 2);
    }

    @Override
    protected void build(Builder b) {
        // Floor, walls and the space inside.
        for (int x = -A; x <= A; x++) {
            for (int z = BACK; z <= FRONT; z++) {
                b.set(x, 0, z, z % 3 == 0 ? Blocks.DARK_OAK_PLANKS : Blocks.SPRUCE_PLANKS);
                int top = roofY(z);
                boolean longWall = z == BACK || z == FRONT;
                boolean endWall = x == -A || x == A;
                for (int y = 1; y < top; y++) {
                    if (endWall || longWall && y <= 3) {
                        boolean post = longWall && (x + A) % 4 == 0 || endWall && longWall;
                        b.set(x, y, z, post ? log(Blocks.DARK_OAK_LOG, Direction.Axis.Y) : Blocks.SPRUCE_PLANKS.getDefaultState());
                    } else {
                        b.set(x, y, z, Blocks.AIR);
                    }
                }
                // The A-frame roof: spruce and dark oak courses, a dark oak ridge.
                if (z == RIDGE) {
                    b.set(x, top, z, Blocks.DARK_OAK_PLANKS);
                } else {
                    Block stair = Math.abs(z - RIDGE) % 2 == 0 ? Blocks.DARK_OAK_STAIRS : Blocks.SPRUCE_STAIRS;
                    b.set(x, top, z, stairs(stair, z > RIDGE ? Direction.NORTH : Direction.SOUTH, false));
                }
            }
        }
        // The door, facing the pit.
        b.air(-1, 1, FRONT, 1, 3, FRONT);
        b.set(-2, 3, FRONT + 1, Blocks.WALL_TORCH.getDefaultState().with(net.minecraft.block.WallTorchBlock.FACING, Direction.SOUTH));
        b.set(2, 3, FRONT + 1, Blocks.WALL_TORCH.getDefaultState().with(net.minecraft.block.WallTorchBlock.FACING, Direction.SOUTH));

        // The fire pit and the benches round it.
        b.fill(-1, 0, RIDGE, 1, 0, RIDGE + 2, Blocks.BLACKSTONE);
        b.set(0, 1, RIDGE + 1, Blocks.CAMPFIRE);
        for (int z = RIDGE - 1; z <= RIDGE + 3; z++) {
            b.set(-4, 1, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
            b.set(4, 1, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
        }
        // War drums.
        for (int side = -1; side <= 1; side += 2) {
            b.set(side * 8, 1, FRONT - 2, log(Blocks.HAY_BLOCK, Direction.Axis.Y));
            b.set(side * 8, 2, FRONT - 2, Blocks.NOTE_BLOCK);
            b.set(side * 8, 1, BACK + 3, log(Blocks.HAY_BLOCK, Direction.Axis.Y));
            b.set(side * 8, 2, BACK + 3, Blocks.NOTE_BLOCK);
        }
        // Lanterns hung from the ridge.
        for (int x = -6; x <= 6; x += 6) {
            b.set(x, roofY(RIDGE) - 1, RIDGE, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        }
        // The trophy wall: mob heads on the back wall either side of the seat.
        for (int x : new int[]{-8, -6, -4, 4, 6, 8}) {
            for (int y = 2; y <= 3; y++) {
                Block head = b.pick(WALL_HEADS);
                b.set(x, y, BACK + 1, head.getDefaultState().with(WallSkullBlock.FACING, Direction.SOUTH));
            }
        }
        // The War Chief's seat: blackstone and gold on a dais.
        b.fill(-2, 0, BACK + 1, 2, 0, SEAT_B + 1, Blocks.POLISHED_BLACKSTONE);
        b.set(0, 1, SEAT_B, stairs(Blocks.BLACKSTONE_STAIRS, Direction.NORTH, false));
        b.set(-1, 1, SEAT_B, Blocks.GOLD_BLOCK);
        b.set(1, 1, SEAT_B, Blocks.GOLD_BLOCK);
        b.set(0, 1, BACK + 1, Blocks.CHISELED_POLISHED_BLACKSTONE);
        b.set(0, 2, BACK + 1, Blocks.GOLD_BLOCK);
        b.set(0, 3, BACK + 1, Blocks.WITHER_SKELETON_SKULL);
        b.warBanner(-2, 3, BACK + 1, Direction.SOUTH);
        b.warBanner(2, 3, BACK + 1, Direction.SOUTH);
        // The hoard (lockpick DC 15) and the chief's barrels.
        b.container(HOARD_A, 1, HOARD_B, facing(Blocks.CHEST, Direction.SOUTH), HOARD_LOOT);
        b.set(-HOARD_A, 1, HOARD_B, facing(Blocks.BARREL, Direction.UP));
        b.set(-HOARD_A - 1, 1, HOARD_B, facing(Blocks.BARREL, Direction.UP));

        b.orc(0, 1, SEAT_B + 2, ModEntityTypes.ORC_WAR_CHIEF, b.pos(0, 1, SEAT_B + 2), 6);
    }
}
