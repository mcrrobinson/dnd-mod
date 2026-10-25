package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.world.gen.PieceBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Duergar Hold's Forge Hall, its door towards local +b: 15 x 11 and 7 high, Basalt Forge Brick walls under a
 * Greysteel roof. Two channels of lava run in the floor under iron bars (lava a block below the floor, the bars
 * flush with it). Along the back wall two lit blast furnaces, a smithing table and the forge chest
 * ({@code chests/duergar_forge}); three anvils in the middle; a lava cauldron; Duergar Braziers in the corners. Two
 * soldiers work here.
 * <p>
 * The Grey Clans' law ({@code DuergarLaw}) guards the chest, and the anvils, furnaces, smithing table, braziers and
 * forge bricks of this hall.
 */
public class DuergarForgePiece extends UnderdarkPiece {
    /** Outer half-sizes: a from -7 to 7, b from -5 to 5. */
    public static final int HALF_A = 7;
    public static final int HALF_B = 5;
    /** Inside height (the roof is at this y). */
    public static final int ROOF = 6;
    /** The forge chest (local). */
    public static final int CHEST_A = 5;
    public static final int CHEST_B = -4;
    /** The lava channels' b (along a from -5 to 5; the front one is broken by the walkway). */
    static final int BACK_CHANNEL = -3;
    static final int FRONT_CHANNEL = 1;

    public DuergarForgePiece(BlockPos center, Direction front, long seed) {
        super(DuergarHolds.FORGE, center, front, seed, -HALF_A, -3, -HALF_B, HALF_A, ROOF + 1, HALF_B);
    }

    public DuergarForgePiece(NbtCompound nbt) {
        super(DuergarHolds.FORGE, nbt);
    }

    /** The forge chest, in the world. */
    public BlockPos chest() {
        return this.at(CHEST_A, 0, CHEST_B);
    }

    /** Where you stand in front of the forge chest. */
    public BlockPos atChest() {
        return this.at(CHEST_A, 0, CHEST_B + 2);
    }

    /** Just inside the door. */
    public BlockPos inside() {
        return this.at(0, 0, HALF_B - 1);
    }

    /** Whether local a/b is in a lava channel. */
    static boolean channel(int a, int c) {
        return Math.abs(a) <= 5 && (c == BACK_CHANNEL || c == FRONT_CHANNEL && Math.abs(a) >= 2);
    }

    @Override
    protected void build(Builder b) {
        BlockState bricks = DuergarHolds.bricks();
        BlockState greysteel = DuergarHolds.greysteel();
        for (int a = -HALF_A; a <= HALF_A; a++) {
            for (int c = -HALF_B; c <= HALF_B; c++) {
                boolean wall = Math.abs(a) == HALF_A || Math.abs(c) == HALF_B;
                b.set(a, -3, c, bricks);
                b.set(a, -2, c, channel(a, c) ? Blocks.LAVA.getDefaultState() : bricks);
                b.set(a, -1, c, channel(a, c) ? Blocks.IRON_BARS.getDefaultState()
                        : wall ? bricks : Math.floorMod(a + c, 2) == 0 ? Blocks.DEEPSLATE_TILES.getDefaultState()
                        : Blocks.POLISHED_DEEPSLATE.getDefaultState());
                for (int y = 0; y < ROOF; y++) {
                    if (wall) {
                        boolean pillar = Math.abs(a) == HALF_A && Math.abs(c) == HALF_B
                                || Math.abs(a) == HALF_A && c == 0 || Math.abs(c) == HALF_B && a % 4 == 0;
                        b.set(a, y, c, pillar ? PieceBlocks.pillar(Blocks.POLISHED_BASALT, net.minecraft.util.math.Direction.Axis.Y)
                                : bricks);
                    } else {
                        b.clear(a, y, c);
                    }
                }
                b.set(a, ROOF, c, greysteel);
                b.clear(a, ROOF + 1, c);
            }
        }
        // The door: 3 wide, 3 high.
        for (int a = -1; a <= 1; a++) {
            for (int y = 0; y <= 2; y++) {
                b.clear(a, y, HALF_B);
            }
        }
        // Windows of iron bars in the side walls.
        for (int c = -2; c <= 2; c += 4) {
            b.set(-HALF_A, 3, c, Blocks.IRON_BARS);
            b.set(HALF_A, 3, c, Blocks.IRON_BARS);
        }
        // Along the back: blast furnaces, the smithing table, the chest.
        BlockState furnace = PieceBlocks.facing(Blocks.BLAST_FURNACE, Direction.SOUTH).with(AbstractFurnaceBlock.LIT, true);
        b.set(-3, 0, -4, furnace);
        b.set(3, 0, -4, furnace);
        b.set(-3, 1, -4, Blocks.POLISHED_BASALT);
        b.set(3, 1, -4, Blocks.POLISHED_BASALT);
        b.set(0, 0, -4, Blocks.SMITHING_TABLE);
        b.chest(CHEST_A, 0, CHEST_B, Direction.SOUTH, DuergarHolds.FORGE_LOOT);
        b.set(-5, 0, -4, Blocks.GRINDSTONE);
        // Three anvils.
        BlockState anvil = Blocks.ANVIL.getDefaultState().with(AnvilBlock.FACING, Direction.EAST);
        b.set(-4, 0, -1, anvil);
        b.set(0, 0, -1, Blocks.CHIPPED_ANVIL.getDefaultState().with(AnvilBlock.FACING, Direction.EAST));
        b.set(4, 0, -1, anvil);
        b.set(-6, 0, 0, Blocks.LAVA_CAULDRON);
        // Braziers in the corners, and chains hanging from the roof.
        for (int a : new int[] {-6, 6}) {
            for (int c : new int[] {-4, 4}) {
                b.set(a, 0, c, DuergarHolds.brazier());
            }
        }
        for (int a = -4; a <= 4; a += 4) {
            b.hangingLantern(a, ROOF - 1, 2, 2, Blocks.SOUL_LANTERN);
        }
        DuergarHolds.duergar(b, -2, 0, 2, DuergarEntities.DUERGAR_SOLDIER, 6);
        DuergarHolds.duergar(b, 2, 0, -2, DuergarEntities.DUERGAR_SOLDIER, 6);
    }
}
