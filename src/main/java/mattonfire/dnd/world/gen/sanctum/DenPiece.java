package mattonfire.dnd.world.gen.sanctum;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.SanctumBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The Alchemist's den, west of the square: a squat brimstone brick workshop under a nether brick roof, its door
 * facing the square. Inside, brewing stands and cauldrons along the back wall, shelves of barrels, the den's
 * chest and two sulphur vents fuming in the floor; outside its south wall a planter of nether wart on soul sand.
 */
public class DenPiece extends BrimstonePiece {
    static final int WEST = -13;
    static final int EAST = -7;
    static final int BACK = -9;
    static final int FRONT = -2;
    static final int HEIGHT = 5;

    public DenPiece(BlockPos center, Direction gate, long seed, long sanctum) {
        super(BrimstoneSanctumStructures.DEN, center, gate, seed, sanctum, WEST, 0, BACK, EAST + 1, HEIGHT + 1, FRONT + 1);
    }

    public DenPiece(NbtCompound nbt) {
        super(BrimstoneSanctumStructures.DEN, nbt);
    }

    @Override
    protected void build(Builder b) {
        for (int x = WEST; x <= EAST; x++) {
            for (int z = BACK; z <= FRONT; z++) {
                boolean edge = x == WEST || x == EAST || z == BACK || z == FRONT;
                boolean corner = (x == WEST || x == EAST) && (z == BACK || z == FRONT);
                b.set(x, 0, z, edge ? ModBlockFamilies.BRIMSTONE_BRICKS.base() : Blocks.NETHER_BRICKS);
                for (int y = 1; y < HEIGHT; y++) {
                    if (corner) {
                        b.set(x, y, z, Blocks.CHISELED_POLISHED_BLACKSTONE);
                    } else if (edge) {
                        boolean window = y == 2 && (x == WEST || z == BACK) && Math.floorMod(x + z, 3) == 0;
                        b.set(x, y, z, window ? Blocks.IRON_BARS.getDefaultState() : masonry(b).getDefaultState());
                    } else {
                        b.set(x, y, z, Blocks.AIR);
                    }
                }
                b.set(x, HEIGHT, z, edge ? Blocks.NETHER_BRICK_SLAB.getDefaultState() : Blocks.NETHER_BRICKS.getDefaultState());
            }
        }
        // The door, facing the square.
        b.air(EAST, 1, -5, EAST, 2, -5);
        b.fill(EAST + 1, 1, -4, EAST + 1, 2, -4, wall(ModBlockFamilies.BRIMSTONE_BRICKS));
        b.set(EAST + 1, 3, -4, soulLantern(false));

        // Brewing stands and cauldrons along the back wall; barrels on the west wall.
        b.set(WEST + 1, 1, BACK + 1, Blocks.BREWING_STAND);
        b.set(WEST + 3, 1, BACK + 1, Blocks.BREWING_STAND);
        b.set(WEST + 2, 1, BACK + 1, Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 1 + b.random.nextInt(3)));
        b.set(WEST + 4, 1, BACK + 1, Blocks.CAULDRON);
        b.set(WEST + 1, 1, BACK + 3, facing(Blocks.BARREL, Direction.UP));
        b.set(WEST + 1, 2, BACK + 3, facing(Blocks.BARREL, Direction.UP));
        b.set(WEST + 1, 1, BACK + 4, Blocks.MAGMA_BLOCK);
        b.set(WEST + 1, 2, BACK + 4, Blocks.BREWING_STAND);
        b.container(EAST - 1, 1, BACK + 1, facing(Blocks.CHEST, Direction.WEST), DEN_LOOT);
        b.set(EAST - 1, 1, FRONT - 1, Blocks.CRAFTING_TABLE);
        b.set(WEST + 3, 0, BACK + 3, SanctumBlocks.SULPHUR_VENT);
        b.set(EAST - 2, 0, FRONT - 2, SanctumBlocks.SULPHUR_VENT);
        b.set((WEST + EAST) / 2, HEIGHT - 1, (BACK + FRONT) / 2, soulLantern(true));

        // The nether wart planter outside the south wall.
        for (int x = WEST + 1; x <= EAST - 1; x++) {
            b.set(x, 0, FRONT + 1, Blocks.SOUL_SAND);
            b.set(x, 1, FRONT + 1, Blocks.NETHER_WART.getDefaultState().with(NetherWartBlock.AGE, b.random.nextInt(4)));
        }
    }
}
