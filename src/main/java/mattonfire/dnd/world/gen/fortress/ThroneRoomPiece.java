package mattonfire.dnd.world.gen.fortress;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The throne room at the end of the great hall: a red carpet up to a stepped dais where the
 * Dwarf King sits on a gold throne between braziers and banners, guarded. A narrow door behind
 * the throne leads to the treasury.
 */
public class ThroneRoomPiece extends FortressPiece {
    private static final int WIDTH = 19;
    private static final int DEPTH = 17;
    private static final int HEIGHT = 15;
    private static final int MID = WIDTH / 2;
    /** The dais top is this far above the floor; the treasury door opens from it. */
    public static final int DAIS_HEIGHT = 2;

    public ThroneRoomPiece(BlockBox box, Direction facing, long seed) {
        super(DwarvenFortressStructures.THRONE_ROOM, box, facing, seed);
    }

    public ThroneRoomPiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.THRONE_ROOM, nbt);
    }

    public static ThroneRoomPiece create(Random random, BlockPos front, Direction facing) {
        return new ThroneRoomPiece(boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        int top = HEIGHT - 1;
        int back = DEPTH - 1;
        DyeColor colour = b.pick(new DyeColor[]{DyeColor.RED, DyeColor.BLUE, DyeColor.PURPLE});

        b.shell(WIDTH, HEIGHT, DEPTH, Blocks.POLISHED_DEEPSLATE.getDefaultState());
        b.doorway(MID, 0, 5, 7);
        for (int z = 0; z < DEPTH; z++) {
            b.set(0, 6, z, Blocks.GILDED_BLACKSTONE);
            b.set(WIDTH - 1, 6, z, Blocks.GILDED_BLACKSTONE);
            b.set(0, 10, z, Blocks.GOLD_BLOCK);
            b.set(WIDTH - 1, 10, z, Blocks.GOLD_BLOCK);
        }
        for (int x = 0; x < WIDTH; x++) {
            b.set(x, 10, back, Blocks.GOLD_BLOCK);
        }

        // The carpet up to the dais.
        b.fill(MID - 1, 0, 1, MID + 1, 0, 10, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.fill(MID - 1, 1, 1, MID + 1, 1, 10, Blocks.RED_CARPET);

        // Pillars down both sides.
        for (int z = 3; z <= 11; z += 4) {
            b.column(3, z, top - 1);
            b.column(WIDTH - 4, z, top - 1);
            b.banner(4, 10, z, Direction.EAST, colour);
            b.banner(WIDTH - 5, 10, z, Direction.WEST, colour);
        }

        // The dais: two broad steps up to the throne.
        b.fill(4, 1, 12, WIDTH - 5, 1, back - 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.fill(4, 1, 11, WIDTH - 5, 1, 11, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, IN, false));
        b.fill(6, 2, 13, WIDTH - 7, 2, back - 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.fill(6, 2, 12, WIDTH - 7, 2, 12, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, IN, false));
        b.fill(MID - 1, 3, 12, MID + 1, 3, 13, Blocks.RED_CARPET);

        // The throne: a gold seat with arms and a tall back, under a hanging crown of lanterns.
        b.set(MID, 3, 14, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, IN, false));
        b.set(MID - 1, 3, 14, Blocks.GOLD_BLOCK);
        b.set(MID + 1, 3, 14, Blocks.GOLD_BLOCK);
        b.set(MID - 1, 4, 14, bottomSlab(Blocks.POLISHED_BLACKSTONE_SLAB));
        b.set(MID + 1, 4, 14, bottomSlab(Blocks.POLISHED_BLACKSTONE_SLAB));
        b.fill(MID, 3, back - 1, MID, 6, back - 1, Blocks.GOLD_BLOCK);
        b.set(MID, 5, back - 1, Blocks.GILDED_BLACKSTONE);
        b.set(MID, 7, back - 1, Blocks.CHISELED_DEEPSLATE);
        b.set(MID, 8, back - 1, Blocks.GOLD_BLOCK);
        b.banner(MID - 3, 9, back - 1, OUT, colour);
        b.banner(MID + 3, 9, back - 1, OUT, colour);
        b.brazier(4, 2, 12);
        b.brazier(WIDTH - 5, 2, 12);
        b.brazier(6, 3, 13);
        b.brazier(WIDTH - 7, 3, 13);

        // The treasury door, hidden behind the throne at dais height.
        b.air(MID - 1, DAIS_HEIGHT + 1, back, MID + 1, DAIS_HEIGHT + 3, back);
        b.set(MID - 1, DAIS_HEIGHT + 1, back - 1, Blocks.AIR);
        b.set(MID + 1, DAIS_HEIGHT + 1, back - 1, Blocks.AIR);

        b.chandelier(MID, top, 5, 5);
        b.chandelier(MID, top, 10, 5);
        for (int z = 2; z < DEPTH - 1; z += 5) {
            b.set(5, top, z, pillar(Blocks.OCHRE_FROGLIGHT, Direction.Axis.Y));
            b.set(WIDTH - 6, top, z, pillar(Blocks.OCHRE_FROGLIGHT, Direction.Axis.Y));
        }

        b.dwarf(MID, 3, 13, dwarf -> dwarf.crown());
        b.dwarf(MID - 3, 1, 9);
        b.dwarf(MID + 3, 1, 9);
    }
}
