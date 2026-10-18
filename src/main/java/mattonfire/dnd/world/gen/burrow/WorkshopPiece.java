package mattonfire.dnd.world.gen.burrow;

import static mattonfire.dnd.classes.Registry.ModBlockFamilies.*;
import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Registry.CogsdeepBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.block.RailBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * One of the two workshop halls either side of the Commons ({@link #side} +1 east, -1 west): 17 by 11, 6 high,
 * with a stone floor, a band of cut riveted brass under the ceiling and two runs of pipework along it. Benches of
 * cosmetic contraptions line both long walls, every one unpowered, a cogwork panel in the wall above each: piston
 * and cogwork towers crowned with glowcrystal lamps, droppers feeding hoppers into barrels, repeater and comparator
 * lines on smooth stone, sticky pistons against slime, note blocks, lightning-rod coils on brass and tool benches.
 * Down the middle stand two machine islands on brass housings either side of the aisle, and a blueprint table. The east hall opens onto the lush grotto; the west
 * hall ends in a forge of furnaces and a blast furnace. A Tinker and a Clockwork Sentinel work here.
 */
public class WorkshopPiece extends CogsdeepPiece {
    /** The hall's inner end (by the Commons) and outer end, in |a|. */
    public static final int NEAR = CommonsPiece.HALF;
    public static final int FAR = 26;
    public static final int HALF_WIDTH = 6;
    public static final int TOP = 6;

    private final int side;

    public WorkshopPiece(BlockPos center, Direction door, long seed, long burrow, int side) {
        super(CogsdeepStructures.WORKSHOP, center, door, seed, burrow, side * NEAR, -1, -HALF_WIDTH, side * FAR, TOP, HALF_WIDTH);
        this.side = side;
    }

    public WorkshopPiece(NbtCompound nbt) {
        super(CogsdeepStructures.WORKSHOP, nbt);
        this.side = nbt.getInt("Side");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Side", this.side);
    }

    /** +1 for the east workshop, -1 for the west. */
    public int side() {
        return this.side;
    }

    /** The middle of the hall, in the world. */
    public BlockPos middle() {
        return this.at(this.side * (NEAR + FAR) / 2, 0, 0);
    }

    @Override
    protected void build(Builder b) {
        int s = this.side;
        b.room(Math.min(s * NEAR, s * FAR), -HALF_WIDTH, Math.max(s * NEAR, s * FAR), HALF_WIDTH, TOP, Blocks.STONE);
        // A smooth stone aisle down the middle.
        for (int a = NEAR + 1; a < FAR; a++) {
            for (int c = -1; c <= 1; c++) {
                b.set(s * a, -1, c, Blocks.SMOOTH_STONE);
            }
        }
        // A band of cut riveted brass round the walls under the ceiling.
        for (int a = NEAR; a <= FAR; a++) {
            b.set(s * a, TOP - 1, -HALF_WIDTH, CUT_RIVETED_BRASS.base());
            b.set(s * a, TOP - 1, HALF_WIDTH, CUT_RIVETED_BRASS.base());
        }
        for (int c = -HALF_WIDTH; c <= HALF_WIDTH; c++) {
            b.set(s * NEAR, TOP - 1, c, CUT_RIVETED_BRASS.base());
            b.set(s * FAR, TOP - 1, c, CUT_RIVETED_BRASS.base());
        }
        b.doorway(s * NEAR, 0, false, 3, 4);

        // Benches against both long walls, one contraption every three blocks.
        for (int a = NEAR + 2; a <= FAR - 3; a += 3) {
            for (int wall : new int[]{-1, 1}) {
                this.contraption(b, s * a, wall * (HALF_WIDTH - 1), wall, b.random.nextInt(8));
                // A cogwork panel set into the wall above each bench.
                b.set(s * a, 3, wall * HALF_WIDTH, CogsdeepBlocks.COGWORK_PANEL);
            }
        }
        // Two machine islands either side of the aisle.
        this.island(b, s * (NEAR + 6), -3);
        this.island(b, s * (FAR - 6), 3);

        if (s > 0) {
            // The far end opens onto the lush grotto.
            b.doorway(s * FAR, 0, false, 3, 4);
        } else {
            // The forge: furnaces and a blast furnace along the far wall, a cauldron and an anvil.
            int a = s * (FAR - 1);
            Direction in = Direction.EAST;
            b.set(a, 0, -2, facing(Blocks.FURNACE, in));
            b.set(a, 0, -1, facing(Blocks.BLAST_FURNACE, in));
            b.set(a, 0, 0, facing(Blocks.FURNACE, in));
            b.set(a, 0, 1, facing(Blocks.BLAST_FURNACE, in));
            b.set(a, 0, 2, Blocks.CAULDRON);
            b.fill(a, 1, -2, a, 1, 1, Blocks.BRICKS);
            b.set(a, 2, -1, Blocks.LANTERN);
            b.set(a + 2, 0, -3, facing(Blocks.ANVIL, Direction.NORTH));
        }
        b.container(s * (FAR - 2), 0, -HALF_WIDTH + 1, facing(Blocks.CHEST, Direction.SOUTH), WORKSHOP_LOOT);

        for (int a = NEAR + 3; a < FAR; a += 5) {
            b.hangingLantern(s * a, TOP - 1, 0, 1);
        }
        // Pipework along the ceiling, wall to wall, either side of the lanterns.
        b.pipes(s * (NEAR + 1), TOP - 1, -3, s * (FAR - 1), -3);
        b.pipes(s * (NEAR + 1), TOP - 1, 3, s * (FAR - 1), 3);
        // The hall's blueprint table, by the first machine island.
        b.set(s * (NEAR + 9), 0, -3, facing(CogsdeepBlocks.BLUEPRINT_TABLE, Direction.SOUTH));

        b.tinker(s * (NEAR + 8), 0, 0, 8);
        b.sentinel(s * (FAR - 4), 0, 0, 12);
    }

    /**
     * One cosmetic contraption against a long wall at a, b ({@code wall} is -1 for the north wall, +1 the south):
     * none of them is powered.
     */
    private void contraption(Builder b, int a, int c, int wall, int kind) {
        Direction out = wall < 0 ? Direction.SOUTH : Direction.NORTH;
        switch (kind) {
            case 0 -> {
                // A piston tower: piston, cogwork, a glowcrystal lamp.
                b.set(a, 0, c, facing(Blocks.PISTON, Direction.UP));
                b.set(a, 1, c, CogsdeepBlocks.COGWORK_PANEL);
                b.set(a, 2, c, glowcrystal(b));
            }
            case 1 -> {
                // A dropper feeding a hopper into a barrel of parts.
                b.set(a, 1, c, facing(Blocks.DROPPER, Direction.DOWN));
                b.set(a, 0, c, Blocks.HOPPER.getDefaultState().with(HopperBlock.FACING, Direction.EAST));
                b.container(a + 1, 0, c, facing(Blocks.BARREL, Direction.UP), WORKSHOP_LOOT);
            }
            case 2 -> {
                // A repeater and comparator line on smooth stone.
                b.set(a, 0, c, Blocks.SMOOTH_STONE);
                b.set(a + 1, 0, c, Blocks.SMOOTH_STONE);
                b.set(a - 1, 0, c, Blocks.SMOOTH_STONE);
                b.set(a - 1, 1, c, Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.EAST)
                        .with(RepeaterBlock.DELAY, 1 + b.random.nextInt(4)));
                b.set(a, 1, c, Blocks.REDSTONE_WIRE);
                b.set(a + 1, 1, c, Blocks.COMPARATOR.getDefaultState().with(ComparatorBlock.FACING, Direction.WEST));
            }
            case 3 -> {
                // A sticky piston shoving at a slime block, a cogwork housing behind it.
                b.set(a - 1, 0, c, CogsdeepBlocks.COGWORK_PANEL);
                b.set(a, 0, c, facing(Blocks.STICKY_PISTON, Direction.EAST));
                b.set(a + 1, 0, c, Blocks.SLIME_BLOCK);
            }
            case 4 -> {
                // Note blocks under a lamp.
                b.set(a, 0, c, Blocks.NOTE_BLOCK);
                b.set(a + 1, 0, c, Blocks.NOTE_BLOCK);
                b.set(a, 1, c, glowcrystal(b));
            }
            case 5 -> {
                // A lightning-rod coil on a brass housing, chains from the ceiling.
                b.set(a, 0, c, RIVETED_BRASS.base());
                b.set(a, 1, c, Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.UP));
                b.set(a, 2, c, Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.UP));
                b.fill(a, 3, c, a, TOP - 1, c, Blocks.CHAIN);
            }
            case 6 -> {
                // A tool bench: crafting table, grindstone and a stonecutter.
                b.set(a - 1, 0, c, Blocks.CRAFTING_TABLE);
                b.set(a, 0, c, facing(Blocks.STONECUTTER, out));
                b.set(a + 1, 0, c, Blocks.FLETCHING_TABLE);
            }
            default -> {
                // A dispenser over a rail stub.
                b.set(a, 1, c, facing(Blocks.DISPENSER, out));
                b.set(a, 0, c, CUT_RIVETED_BRASS.base());
                b.set(a, 0, c - wall, Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.EAST_WEST));
            }
        }
    }

    /** A machine island two blocks deep on a brass housing: pistons and cogwork round two lamps, a rail loop on top. */
    private void island(Builder b, int a, int c) {
        int dc = c < 0 ? -1 : 1;
        for (int i = -1; i <= 1; i++) {
            b.set(a + i, 0, c, CUT_RIVETED_BRASS.base());
            b.set(a + i, 0, c + dc, CUT_RIVETED_BRASS.base());
        }
        b.set(a - 1, 1, c, facing(Blocks.PISTON, Direction.UP));
        b.set(a + 1, 1, c, facing(Blocks.STICKY_PISTON, Direction.UP));
        b.set(a, 1, c, CogsdeepBlocks.COGWORK_PANEL);
        b.set(a, 2, c, glowcrystal(b));
        b.set(a - 1, 1, c + dc, Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.EAST_WEST));
        b.set(a, 1, c + dc, Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.EAST_WEST));
        b.set(a + 1, 1, c + dc, Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.EAST_WEST));
        b.set(a + 1, 2, c, glowcrystal(b));
    }
}
