package mattonfire.dnd.world.underdark.drow;

import java.util.List;
import mattonfire.dnd.classes.Blocks.TrapBlocks;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.classes.Blocks.TrapTriggerBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstaclePlacer;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.drow.Drow;
import mattonfire.dnd.world.gen.PieceBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlazedTerracottaBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The spider shrine in the middle of a Drow Outpost, facing the barracks (local +b):
 * <ul>
 * <li>an 11 by 11 floor of Drowstone Bricks edged with Polished Faerzress, a Spider Sigil inlay in front, a Faerie Fire
 * Lantern on a post at each corner;</li>
 * <li>the spider idol, 5 high: a pedestal of Polished Faerzress, a Drowstone abdomen and head with crying-obsidian eyes,
 * and eight legs of Drowstone Brick walls arching out to the floor (you can walk under the knees);</li>
 * <li>an altar with a lantern in front of it;</li>
 * <li>behind the idol, the reliquary niche: the shrine's chest ({@code chests/drow_shrine}) in a small vault whose
 * archway is framed with Dread Ward blocks (a Medium {@link ObstacleTypes#DREAD_WARD}: Frightened, slowed and weakened
 * inside it unless a Paladin is near);</li>
 * <li>a dart trap on the approach ({@value #TRAP_B} blocks out in front): a hidden plate and a launcher in a post,
 * firing Drow Poison darts (CON DC 13);</li>
 * <li>the outpost's priestess.</li>
 * </ul>
 */
public class DrowShrinePiece extends UnderdarkPiece {
    /** Half the floor (it runs -5..5). */
    public static final int HALF = 5;
    /** The niche's back wall. */
    public static final int NICHE_BACK = -9;
    /** How far out in front the trap's plate is. */
    public static final int TRAP_B = 7;
    /** The trap's challenge tier (it fires 3 darts of 1.67 damage). */
    public static final int TRAP_TIER = 2;

    public DrowShrinePiece(BlockPos center, Direction front, long seed) {
        super(DrowOutposts.SHRINE, center, front, seed, -HALF - 1, -2, NICHE_BACK - 1, HALF + 1, 6, TRAP_B + 1);
    }

    public DrowShrinePiece(NbtCompound nbt) {
        super(DrowOutposts.SHRINE, nbt);
    }

    /** The reliquary chest, in the world. */
    public BlockPos reliquary() {
        return this.at(0, 0, NICHE_BACK + 1);
    }

    /** The dart trap's plate, in the world. */
    public BlockPos trapPlate() {
        return this.at(0, -1, TRAP_B);
    }

    /** Where you stand in front of the altar, in the world. */
    public BlockPos altarSpot() {
        return this.at(0, 0, 4);
    }

    @Override
    protected void build(Builder b) {
        BlockState bricks = ModBlockFamilies.DROWSTONE_BRICKS.base().getDefaultState();
        BlockState polished = ModBlockFamilies.POLISHED_FAERZRESS.base().getDefaultState();
        // The floor, and air over it.
        for (int a = -HALF; a <= HALF; a++) {
            for (int c = -HALF; c <= HALF; c++) {
                boolean edge = Math.abs(a) == HALF || Math.abs(c) == HALF;
                b.set(a, -1, c, edge ? polished : bricks);
                b.clear(a, c, 0, 5);
            }
        }
        // The sigil inlay in front of the altar.
        Direction[] turns = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int t = 0;
        for (int c = 3; c <= 4; c++) {
            for (int a = -1; a <= 1; a++) {
                b.set(a, -1, c, UnderdarkBlocks.SPIDER_SIGIL_STONE.getDefaultState()
                        .with(GlazedTerracottaBlock.FACING, turns[t++ % turns.length]));
            }
        }
        // Lantern posts at the corners.
        for (int a : new int[] {-HALF, HALF}) {
            for (int c : new int[] {-HALF, HALF}) {
                b.set(a, 0, c, PieceBlocks.pillar(ModBlockFamilies.DROWSTONE_BRICKS, Direction.Axis.Y));
                b.set(a, 1, c, PieceBlocks.pillar(ModBlockFamilies.DROWSTONE_BRICKS, Direction.Axis.Y));
                b.lantern(a, 2, c, UnderdarkBlocks.FAERIE_LANTERN);
            }
        }
        this.idol(b, bricks, polished);
        // The altar.
        b.set(0, 0, 2, polished);
        b.set(-1, 0, 2, PieceBlocks.stairs(ModBlockFamilies.DROWSTONE_BRICKS, Direction.EAST, true));
        b.set(1, 0, 2, PieceBlocks.stairs(ModBlockFamilies.DROWSTONE_BRICKS, Direction.WEST, true));
        b.lantern(0, 1, 2, UnderdarkBlocks.FAERIE_LANTERN);
        this.niche(b, bricks);
        this.trap(b);
        b.resident(2, 0, 3, Drow.DROW_PRIESTESS, 6);
    }

    /** The spider idol (local c -4..1). */
    private void idol(Builder b, BlockState bricks, BlockState polished) {
        for (int a = -2; a <= 2; a++) {
            for (int c = -3; c <= 0; c++) {
                b.set(a, 0, c, polished);
            }
        }
        // The abdomen: an egg of Drowstone, its top smoothed with Polished Faerzress.
        for (int a = -2; a <= 2; a++) {
            for (int y = 1; y <= 4; y++) {
                for (int c = -5; c <= 0; c++) {
                    if (inAbdomen(a, y, c)) {
                        b.set(a, y, c, inAbdomen(a, y + 1, c) ? bricks : polished);
                    }
                }
            }
        }
        // The head, and its eyes.
        for (int a = -1; a <= 1; a++) {
            for (int y = 1; y <= 2; y++) {
                b.set(a, y, 1, bricks);
                b.set(a, y, 0, bricks);
            }
        }
        b.set(-1, 2, 1, Blocks.CRYING_OBSIDIAN);
        b.set(1, 2, 1, Blocks.CRYING_OBSIDIAN);
        // Eight legs: from the body's side up to a knee and out and down to a foot on the floor.
        int[][] legs = {{0, 2}, {-1, 0}, {-2, -2}, {-3, -4}};
        BlockState wall = ModBlockFamilies.DROWSTONE_BRICKS.wall().getDefaultState();
        for (int side : new int[] {-1, 1}) {
            for (int[] leg : legs) {
                double kc = (leg[0] + leg[1]) / 2.0D;
                this.limb(b, wall, side * 2, 2, leg[0], side * 3, 4, kc);
                this.limb(b, wall, side * 3, 4, kc, side * 5, 0, leg[1]);
            }
        }
    }

    static boolean inAbdomen(int a, int y, int c) {
        double x = a / 1.6D;
        double h = (y - 2.0D) / 1.4D;
        double z = (c + 2.5D) / 1.8D;
        return x * x + h * h + z * z <= 1.0D;
    }

    /** A straight leg segment of walls from (a0, y0, c0) to (a1, y1, c1). */
    private void limb(Builder b, BlockState wall, double a0, double y0, double c0, double a1, double y1, double c1) {
        int steps = 12;
        for (int i = 0; i <= steps; i++) {
            double f = i / (double) steps;
            int a = (int) Math.round(a0 + (a1 - a0) * f);
            int y = (int) Math.round(y0 + (y1 - y0) * f);
            int c = (int) Math.round(c0 + (c1 - c0) * f);
            if (Math.abs(a) > 2 || y > 0) {
                b.set(a, y, c, wall);
            }
        }
    }

    /** The reliquary niche behind the idol, its archway framed with Dread Ward blocks. */
    private void niche(Builder b, BlockState bricks) {
        for (int a = -2; a <= 2; a++) {
            for (int c = NICHE_BACK; c <= -HALF; c++) {
                for (int y = -1; y <= 3; y++) {
                    boolean shell = y == -1 || y == 3 || Math.abs(a) == 2 || c == NICHE_BACK;
                    if (c == -HALF) {
                        continue; // the archway, below
                    }
                    b.set(a, y, c, shell ? bricks : Blocks.AIR.getDefaultState());
                }
            }
        }
        b.set(0, 1, NICHE_BACK, UnderdarkBlocks.SPIDER_SIGIL_STONE.getDefaultState()
                .with(GlazedTerracottaBlock.FACING, Direction.SOUTH));
        b.hangingLantern(0, 2, NICHE_BACK + 2, 1, UnderdarkBlocks.FAERIE_LANTERN);
        b.chest(0, 0, NICHE_BACK + 1, Direction.SOUTH, DrowOutposts.SHRINE_LOOT);
        // The archway: open 3 wide and 3 high, the ward stones round it.
        long group = b.random.nextLong();
        for (int a = -2; a <= 2; a++) {
            for (int y = 0; y <= 3; y++) {
                boolean frame = Math.abs(a) == 2 || y == 3;
                if (frame) {
                    ObstaclePlacer.place(b.world, b.chunkBox, b.pos(a, y, -HALF), ObstacleTypes.DREAD_WARD, Tier.MEDIUM,
                            false, group);
                } else {
                    b.clear(a, y, -HALF);
                }
            }
        }
    }

    /** The dart trap on the approach: a hidden plate on the path and a launcher in a post beside it. */
    private void trap(Builder b) {
        int la = 3;
        b.set(la, 0, TRAP_B, ModBlockFamilies.DROWSTONE_BRICKS.base());
        b.set(la, 1, TRAP_B, TrapBlocks.DART_LAUNCHER.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
        b.set(la, 2, TRAP_B, ModBlockFamilies.DROWSTONE_BRICKS.wall());
        BlockPos plate = b.pos(0, -1, TRAP_B);
        if (!b.chunkBox.contains(plate)) {
            return;
        }
        BlockPos launcher = b.pos(la, 1, TRAP_B);
        b.world.setBlockState(plate, TrapBlocks.TRAP_TRIGGER.getDefaultState(), PieceBlocks.UPDATE);
        if (b.world.getBlockEntity(plate) instanceof TrapTriggerBlockEntity trap) {
            trap.setup(TrapKind.DART, 0L, TRAP_TIER, List.of(launcher), List.of(), null, Direction.NORTH);
            trap.setDrowPoison(true);
        }
    }
}
