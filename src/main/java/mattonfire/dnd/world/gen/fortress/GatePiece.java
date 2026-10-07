package mattonfire.dnd.world.gen.fortress;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The great gate: a paved terrace with braziers and guards in front of a towering deepslate
 * facade set into the mountainside, flanked by gold-capped pillars and banners, with a gilded
 * doorway under a raised portcullis and a crest above it.
 */
public class GatePiece extends FortressPiece {
    private static final int WIDTH = 21;
    private static final int DEPTH = 10;
    private static final int HEIGHT = 18;
    /** The terrace runs from the front edge up to the facade. */
    static final int FACADE = 4;
    private static final int MID = WIDTH / 2;

    public GatePiece(BlockBox box, Direction facing, long seed) {
        super(DwarvenFortressStructures.GATE, box, facing, seed);
    }

    public GatePiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.GATE, nbt);
    }

    public static GatePiece create(Random random, BlockPos front, Direction facing) {
        return new GatePiece(boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        DyeColor banner = b.pick(new DyeColor[]{DyeColor.RED, DyeColor.BLUE, DyeColor.BLACK, DyeColor.PURPLE});

        // Terrace: paved, open to the sky, with a low parapet and a gap for the approach.
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < FACADE; z++) {
                b.foundation(x, z);
                b.set(x, 0, z, z == 0 || x == 0 || x == WIDTH - 1 ? Blocks.DEEPSLATE_TILES : Blocks.POLISHED_DEEPSLATE);
                b.air(x, 1, z, x, HEIGHT - 1, z);
            }
        }
        b.fill(MID - 1, 0, 0, MID + 1, 0, FACADE - 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
        for (int x = 0; x < WIDTH; x++) {
            if (Math.abs(x - MID) > 3) {
                b.set(x, 1, 0, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
            }
        }
        for (int z = 1; z < FACADE; z++) {
            b.set(0, 1, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
            b.set(WIDTH - 1, 1, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        }
        for (int x : new int[]{0, MID - 4, MID + 4, WIDTH - 1}) {
            b.set(x, 1, 0, Blocks.POLISHED_BLACKSTONE_BRICKS);
            b.set(x, 2, 0, lantern(false));
        }
        b.brazier(2, 1, 2);
        b.brazier(WIDTH - 3, 1, 2);

        // The facade: solid masonry from the terrace to the back of the piece.
        for (int x = 0; x < WIDTH; x++) {
            for (int z = FACADE; z < DEPTH; z++) {
                b.foundation(x, z);
                b.set(x, 0, z, Blocks.POLISHED_DEEPSLATE);
                for (int y = 1; y < HEIGHT; y++) {
                    b.set(x, y, z, masonry(b));
                }
            }
        }
        // Base course, string course and a gold band.
        b.fill(0, 1, FACADE, WIDTH - 1, 1, FACADE, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.fill(0, 11, FACADE, WIDTH - 1, 11, FACADE, Blocks.CHISELED_DEEPSLATE);
        b.fill(0, 15, FACADE, WIDTH - 1, 15, FACADE, Blocks.GILDED_BLACKSTONE);
        // Cornice overhanging the terrace, and battlements along the top.
        for (int x = 0; x < WIDTH; x++) {
            b.set(x, 16, FACADE - 1, stairs(Blocks.DEEPSLATE_TILE_STAIRS, OUT, true));
            if (x % 2 == 1) {
                b.set(x, HEIGHT - 1, FACADE, Blocks.AIR);
            }
        }

        // The passage, five wide and seven tall, with a portcullis drawn up into the arch.
        b.air(MID - 2, 1, FACADE, MID + 2, 7, DEPTH - 1);
        b.fill(MID - 2, 0, FACADE, MID + 2, 0, DEPTH - 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.fill(MID - 1, 1, FACADE, MID + 1, 1, DEPTH - 1, Blocks.RED_CARPET);
        b.fill(MID - 2, 7, FACADE + 1, MID + 2, 7, FACADE + 1, Blocks.IRON_BARS);
        b.set(MID - 2, 7, FACADE, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true));
        b.set(MID + 2, 7, FACADE, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true));
        b.set(MID, 7, DEPTH - 2, lantern(true));
        // Gilded door frame.
        b.fill(MID - 3, 1, FACADE, MID - 3, 8, FACADE, Blocks.GILDED_BLACKSTONE);
        b.fill(MID + 3, 1, FACADE, MID + 3, 8, FACADE, Blocks.GILDED_BLACKSTONE);
        b.fill(MID - 3, 8, FACADE, MID + 3, 8, FACADE, Blocks.GILDED_BLACKSTONE);
        b.set(MID, 8, FACADE, Blocks.GOLD_BLOCK);

        // The crest above the door: a gold rune in a chiselled lozenge.
        b.set(MID, 13, FACADE, Blocks.GOLD_BLOCK);
        for (int[] p : new int[][]{{-1, 12}, {1, 12}, {-1, 14}, {1, 14}, {-2, 13}, {2, 13}}) {
            b.set(MID + p[0], p[1], FACADE, Blocks.GILDED_BLACKSTONE);
        }
        b.set(MID, 12, FACADE, Blocks.CHISELED_DEEPSLATE);
        b.set(MID, 14, FACADE, Blocks.CHISELED_DEEPSLATE);

        // Pillars standing out in front of the facade, with banners between them and the door.
        for (int x : new int[]{4, WIDTH - 5}) {
            b.column(x, FACADE - 1, 14);
            b.set(x, 15, FACADE - 1, Blocks.GOLD_BLOCK);
        }
        b.banner(MID - 5, 10, FACADE - 1, OUT, banner);
        b.banner(MID + 5, 10, FACADE - 1, OUT, banner);
        b.set(MID - 5, 6, FACADE - 1, lantern(false));
        b.set(MID - 5, 5, FACADE - 1, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        b.set(MID + 5, 6, FACADE - 1, lantern(false));
        b.set(MID + 5, 5, FACADE - 1, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        b.fill(MID - 5, 1, FACADE - 1, MID - 5, 4, FACADE - 1, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        b.fill(MID + 5, 1, FACADE - 1, MID + 5, 4, FACADE - 1, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);

        // Arrow slits.
        for (int x : new int[]{2, WIDTH - 3}) {
            b.fill(x, 8, FACADE, x, 9, FACADE, Blocks.IRON_BARS);
        }

        b.dwarf(MID - 3, 1, 2);
        b.dwarf(MID + 3, 1, 2);
    }
}
