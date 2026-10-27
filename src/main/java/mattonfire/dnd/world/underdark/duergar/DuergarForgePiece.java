package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Duergar Hold's Forge Hall, its door towards local +b (+a is the road's side). The hold's biggest building:
 * <ul>
 * <li>the <b>hall</b>, 13 x 9 with its corners chamfered, walls 8 high on a battered blackstone plinth, buttressed,
 * under a stepped roof;</li>
 * <li>the <b>great stack</b>: a 3 x 3 chimney rising from the middle of the back wall to y 14-17, smoking from
 * campfires on magma, with the great furnace in its foot; two thin flues smoke from the side walls;</li>
 * <li>a <b>porch</b> 5 wide with a lean-to roof, its door recessed in a basalt-and-blackstone portal;</li>
 * <li>half the time a <b>smelting wing</b> on the road side under a lean-to roof, with two more blast furnaces.</li>
 * </ul>
 * Inside: two channels of lava in the floor under iron bars (lava a block below the floor, the bars flush with it);
 * along the back wall a grindstone, two lit blast furnaces, the great furnace, a smithing table and the forge chest
 * ({@code chests/duergar_forge}); three anvils; a lava cauldron; Duergar Braziers. Two soldiers work here.
 * <p>
 * The Grey Clans' law ({@code DuergarLaw}) guards the chest, and the anvils, furnaces, smithing table, braziers and
 * forge bricks of this hall.
 */
public class DuergarForgePiece extends DuergarPiece {
    /** The hall's half-sizes: a from -6 to 6, b from -4 to 4 (the porch out to 6, its portal at 7). */
    public static final int HALF_A = 6;
    public static final int HALF_B = 4;
    /** The walls' top course (the roof starts above it). */
    public static final int ROOF = 7;
    /** Blocks from the centre to just outside the door. */
    public static final int DOOR = 8;
    /** The forge chest (local). */
    public static final int CHEST_A = 4;
    public static final int CHEST_B = -3;
    /** The lava channels' b (along a from -4 to 4; the front one is broken by the walkway). */
    static final int BACK_CHANNEL = -2;
    static final int FRONT_CHANNEL = 1;

    public DuergarForgePiece(BlockPos center, Direction front, long seed, boolean mirror, int style) {
        super(DuergarHolds.FORGE, center, front, seed, mirror, style, -8, -3, -7, 11, 19, DOOR);
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
        return Math.abs(a) <= 4 && (c == BACK_CHANNEL || c == FRONT_CHANNEL && Math.abs(a) >= 2);
    }

    @Override
    protected void build(Builder b) {
        DuergarMasonry.Style style = this.style();
        boolean wing = b.random.nextBoolean();
        int stack = 14 + b.random.nextInt(4);
        int flue = 11 + b.random.nextInt(2);

        DuergarMasonry.Mass mass = new DuergarMasonry.Mass(-6, 9, -4, 6)
                .box(-HALF_A, HALF_A, -HALF_B, HALF_B, 2, ROOF)
                .box(-2, 2, HALF_B + 1, HALF_B + 2, 0, 5);
        if (wing) {
            mass.box(7, 9, -3, 2, 0, 4).cut(9, -3).cut(9, 2);
        }
        DuergarMasonry.build(b, mass, style, 3, DuergarMasonry.Open.NONE);

        // The floor: lava channels under iron bars.
        BlockState bricks = DuergarHolds.bricks();
        for (int a = -HALF_A + 1; a <= HALF_A - 1; a++) {
            for (int c = -HALF_B + 1; c <= HALF_B - 1; c++) {
                if (channel(a, c)) {
                    b.set(a, -3, c, bricks);
                    b.set(a, -2, c, Blocks.LAVA.getDefaultState());
                    b.set(a, -1, c, Blocks.IRON_BARS.getDefaultState());
                }
            }
        }

        // The great stack in the back wall, the great furnace in its foot; thin flues from the side walls.
        DuergarMasonry.chimney(b, -1, 1, -HALF_B - 2, -HALF_B, 0, stack);
        BlockState lit = PieceBlocks.facing(Blocks.BLAST_FURNACE, Direction.SOUTH).with(AbstractFurnaceBlock.LIT, true);
        b.set(0, 0, -HALF_B, lit);
        b.set(0, 1, -HALF_B, Blocks.MAGMA_BLOCK.getDefaultState());
        b.set(0, 2, -HALF_B, DuergarMasonry.baseStairs(Direction.NORTH, true));
        DuergarMasonry.chimney(b, -HALF_A, -HALF_A, 0, 0, ROOF + 1, flue);
        DuergarMasonry.chimney(b, HALF_A, HALF_A, 0, 0, ROOF + 1, flue - 1);

        // The porch's portal; buttresses and windows.
        DuergarMasonry.portal(b, 0, 1, 3, HALF_B + 2);
        DuergarMasonry.buttress(b, -HALF_A - 1, -2, Direction.WEST, 5);
        DuergarMasonry.buttress(b, -HALF_A - 1, 2, Direction.WEST, 5);
        DuergarMasonry.window(b, -HALF_A, 3, 0, Direction.WEST, 2, true);
        DuergarMasonry.buttress(b, -3, -HALF_B - 1, Direction.NORTH, 5);
        DuergarMasonry.buttress(b, 3, -HALF_B - 1, Direction.NORTH, 5);
        DuergarMasonry.buttress(b, -4, HALF_B + 1, Direction.SOUTH, 4);
        DuergarMasonry.buttress(b, 4, HALF_B + 1, Direction.SOUTH, 4);
        DuergarMasonry.window(b, -3, 3, HALF_B, Direction.SOUTH, 2, false);
        DuergarMasonry.window(b, 3, 3, HALF_B, Direction.SOUTH, 2, false);
        if (wing) {
            DuergarMasonry.window(b, 9, 1, -1, Direction.EAST, 2, false);
            DuergarMasonry.window(b, 9, 1, 0, Direction.EAST, 2, false);
            BlockState west = PieceBlocks.facing(Blocks.BLAST_FURNACE, Direction.WEST).with(AbstractFurnaceBlock.LIT, true);
            b.set(8, 0, -2, west);
            b.set(8, 0, 1, west);
            b.set(8, 0, -1, Blocks.RAW_IRON_BLOCK.getDefaultState());
            b.hangingLantern(7, 4, 0, 1, Blocks.SOUL_LANTERN);
        } else {
            DuergarMasonry.buttress(b, HALF_A + 1, -2, Direction.EAST, 5);
            DuergarMasonry.buttress(b, HALF_A + 1, 2, Direction.EAST, 5);
            DuergarMasonry.window(b, HALF_A, 3, 0, Direction.EAST, 2, true);
        }
        // High slits in the side walls, under the eaves.
        for (int c : new int[] {-2, 2}) {
            b.set(-HALF_A, 6, c, Blocks.IRON_BARS.getDefaultState());
            if (!wing) {
                b.set(HALF_A, 6, c, Blocks.IRON_BARS.getDefaultState());
            }
        }

        // Along the back: grindstone, blast furnaces, smithing table, the chest.
        b.set(-4, 0, -3, Blocks.GRINDSTONE.getDefaultState());
        b.set(-3, 0, -3, lit);
        b.set(-3, 1, -3, DuergarMasonry.pillar());
        b.set(3, 0, -3, lit);
        b.set(3, 1, -3, DuergarMasonry.pillar());
        b.set(2, 0, -3, Blocks.SMITHING_TABLE.getDefaultState());
        b.chest(CHEST_A, 0, CHEST_B, Direction.SOUTH, DuergarHolds.FORGE_LOOT);
        // Three anvils, a lava cauldron.
        BlockState anvil = Blocks.ANVIL.getDefaultState().with(AnvilBlock.FACING, Direction.EAST);
        b.set(-4, 0, 0, anvil);
        b.set(0, 0, -1, Blocks.CHIPPED_ANVIL.getDefaultState().with(AnvilBlock.FACING, Direction.EAST));
        b.set(4, 0, 0, anvil);
        b.set(-5, 0, 0, Blocks.LAVA_CAULDRON.getDefaultState());
        // Braziers, and lanterns hanging from the roof.
        for (int a : new int[] {-5, 5}) {
            for (int c : new int[] {-2, 2}) {
                b.set(a, 0, c, DuergarHolds.brazier());
            }
        }
        for (int a = -3; a <= 3; a += 3) {
            b.hangingLantern(a, ROOF, 2, 2, Blocks.SOUL_LANTERN);
        }
        // Braziers flanking the portal outside.
        b.set(-3, 0, HALF_B + 3, DuergarHolds.brazier());
        b.set(3, 0, HALF_B + 3, DuergarHolds.brazier());
        DuergarHolds.duergar(b, -2, 0, 2, DuergarEntities.DUERGAR_SOLDIER, 6);
        DuergarHolds.duergar(b, 2, 0, -1, DuergarEntities.DUERGAR_SOLDIER, 6);
    }
}
