package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.world.gen.PieceBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.SlabType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Duergar Hold's barracks, its door towards local +b: 11 x 7 and 5 high, Basalt Forge Brick walls under a Greysteel
 * roof. Four bunks along the back wall: a black bed under a Greysteel shelf with a dark wool mattress on it. A chest of
 * kit ({@code chests/duergar_barracks}) between them, barrels by the door, and two soldiers off duty.
 */
public class DuergarBarracksPiece extends UnderdarkPiece {
    public static final int HALF_A = 5;
    public static final int HALF_B = 3;
    public static final int ROOF = 4;

    public DuergarBarracksPiece(BlockPos center, Direction front, long seed) {
        super(DuergarHolds.BARRACKS, center, front, seed, -HALF_A, -1, -HALF_B, HALF_A, ROOF + 1, HALF_B);
    }

    public DuergarBarracksPiece(NbtCompound nbt) {
        super(DuergarHolds.BARRACKS, nbt);
    }

    /** Just inside the door. */
    public BlockPos inside() {
        return this.at(0, 0, HALF_B - 1);
    }

    @Override
    protected void build(Builder b) {
        BlockState bricks = DuergarHolds.bricks();
        for (int a = -HALF_A; a <= HALF_A; a++) {
            for (int c = -HALF_B; c <= HALF_B; c++) {
                boolean wall = Math.abs(a) == HALF_A || Math.abs(c) == HALF_B;
                b.set(a, -1, c, wall ? bricks : Blocks.DEEPSLATE_TILES.getDefaultState());
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
        b.clear(0, 0, HALF_B);
        b.clear(0, 1, HALF_B);
        b.set(-3, 2, HALF_B, Blocks.IRON_BARS);
        b.set(3, 2, HALF_B, Blocks.IRON_BARS);
        // Bunks.
        BlockState shelf = ModBlockFamilies.GREYSTEEL_PLATE.slab().getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
        for (int a : new int[] {-4, -2, 2, 4}) {
            BlockState bed = Blocks.BLACK_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
            b.set(a, 0, -1, bed.with(BedBlock.PART, BedPart.FOOT));
            b.set(a, 0, -2, bed.with(BedBlock.PART, BedPart.HEAD));
            for (int c = -2; c <= -1; c++) {
                b.set(a, 1, c, shelf);
                b.set(a, 2, c, Blocks.GRAY_CARPET);
            }
        }
        b.chest(0, 0, -2, Direction.SOUTH, DuergarHolds.BARRACKS_LOOT);
        b.set(-4, 0, 2, PieceBlocks.facing(Blocks.BARREL, Direction.UP));
        b.set(4, 0, 2, PieceBlocks.facing(Blocks.BARREL, Direction.UP));
        b.hangingLantern(0, ROOF - 1, 0, 1, Blocks.SOUL_LANTERN);
        DuergarHolds.duergar(b, -1, 0, 0, DuergarEntities.DUERGAR_SOLDIER, 8);
        DuergarHolds.duergar(b, 1, 0, 1, DuergarEntities.DUERGAR_SOLDIER, 8);
    }
}
