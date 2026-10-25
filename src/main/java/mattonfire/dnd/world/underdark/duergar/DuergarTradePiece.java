package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.world.gen.PieceBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Duergar Hold's Trade Hall, its door towards local +b: 11 x 9 and 6 high, Basalt Forge Brick walls under a
 * Greysteel roof. A Greysteel counter runs across the room with the factor behind it (the hall is the factor's home);
 * barrels of the factor's stock ({@code chests/duergar_trade}: Darklake Pearls, gold nuggets) stand along the back
 * wall, and the <b>hearth</b>, a Duergar Brazier in a brick fireplace, warms one side.
 */
public class DuergarTradePiece extends UnderdarkPiece {
    public static final int HALF_A = 5;
    public static final int HALF_B = 4;
    public static final int ROOF = 5;
    /** The counter's b. */
    static final int COUNTER = -1;

    public DuergarTradePiece(BlockPos center, Direction front, long seed) {
        super(DuergarHolds.TRADE, center, front, seed, -HALF_A, -1, -HALF_B, HALF_A, ROOF + 1, HALF_B);
    }

    public DuergarTradePiece(NbtCompound nbt) {
        super(DuergarHolds.TRADE, nbt);
    }

    /** Where the factor lives, behind the counter. */
    public BlockPos factorHome() {
        return this.at(0, 0, COUNTER - 1);
    }

    /** Where you stand in front of the counter. */
    public BlockPos atCounter() {
        return this.at(0, 0, COUNTER + 1);
    }

    /** The hearth brazier. */
    public BlockPos hearth() {
        return this.at(-HALF_A + 1, 0, 1);
    }

    @Override
    protected void build(Builder b) {
        BlockState bricks = DuergarHolds.bricks();
        for (int a = -HALF_A; a <= HALF_A; a++) {
            for (int c = -HALF_B; c <= HALF_B; c++) {
                boolean wall = Math.abs(a) == HALF_A || Math.abs(c) == HALF_B;
                b.set(a, -1, c, wall ? bricks : Math.floorMod(a, 2) == 0 ? Blocks.POLISHED_DEEPSLATE.getDefaultState()
                        : Blocks.DEEPSLATE_TILES.getDefaultState());
                for (int y = 0; y < ROOF; y++) {
                    if (wall) {
                        boolean corner = Math.abs(a) == HALF_A && Math.abs(c) == HALF_B;
                        b.set(a, y, c, corner ? PieceBlocks.pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y) : bricks);
                    } else {
                        b.clear(a, y, c);
                    }
                }
                b.set(a, ROOF, c, DuergarHolds.greysteel());
                b.clear(a, ROOF + 1, c);
            }
        }
        // The door, 3 wide and 3 high, with braziers outside.
        for (int a = -1; a <= 1; a++) {
            for (int y = 0; y <= 2; y++) {
                b.clear(a, y, HALF_B);
            }
        }
        // Windows.
        b.set(-3, 2, HALF_B, Blocks.IRON_BARS);
        b.set(3, 2, HALF_B, Blocks.IRON_BARS);
        // The counter: Greysteel slabs on brick, a gap at the end to get behind it.
        for (int a = -3; a <= 3; a++) {
            b.set(a, 0, COUNTER, ModBlockFamilies.GREYSTEEL_PLATE.slab().getDefaultState()
                    .with(net.minecraft.block.SlabBlock.TYPE, net.minecraft.block.enums.SlabType.TOP));
        }
        b.set(-4, 0, COUNTER, bricks);
        // Barrels of stock along the back wall.
        for (int a : new int[] {-3, -2, 2, 3}) {
            b.barrel(a, 0, -HALF_B + 1, Direction.UP, DuergarHolds.TRADE_LOOT);
        }
        b.set(0, 1, -HALF_B, Blocks.SOUL_LANTERN);
        // The hearth: a brazier in a brick fireplace on the side wall, a basalt chimney breast over it.
        b.set(-HALF_A + 1, -1, 1, bricks);
        b.set(-HALF_A + 1, 0, 1, DuergarHolds.brazier());
        b.set(-HALF_A + 1, 0, 0, ModBlockFamilies.BASALT_FORGE_BRICKS.wall().getDefaultState());
        b.set(-HALF_A + 1, 0, 2, ModBlockFamilies.BASALT_FORGE_BRICKS.wall().getDefaultState());
        for (int y = 2; y < ROOF; y++) {
            b.set(-HALF_A + 1, y, 1, PieceBlocks.pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
        }
        b.hangingLantern(1, ROOF - 1, 1, 1, Blocks.SOUL_LANTERN);
        DuergarHolds.duergar(b, 0, 0, COUNTER - 1, DuergarEntities.DUERGAR_FACTOR, 3);
    }
}
