package mattonfire.dnd.world.gen.fortress;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Blocks.dwarven.WaystoneBlock;
import mattonfire.dnd.classes.Registry.AdventuringBlocks;
import mattonfire.dnd.classes.Registry.DwarvenBlocks;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The great gate: a paved terrace with braziers, guards and a waystone in front of a towering runic
 * brick facade set into the mountainside, flanked by gold-veined pillars and banners, with a chiseled
 * runic doorway under a raised portcullis and the clan crest above it.
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
        b.fill(MID - 1, 0, 0, MID + 1, 0, FACADE - 1, RUNIC.base());
        for (int x = 0; x < WIDTH; x++) {
            if (Math.abs(x - MID) > 3) {
                b.set(x, 1, 0, wall(RUNIC));
            }
        }
        for (int z = 1; z < FACADE; z++) {
            b.set(0, 1, z, wall(RUNIC));
            b.set(WIDTH - 1, 1, z, wall(RUNIC));
        }
        // Runic Column posts along the parapet, each with a lantern on top.
        for (int x : new int[]{0, MID - 4, MID + 4, WIDTH - 1}) {
            b.runicColumn(x, 1, 2, 0);
            b.set(x, 3, 0, lantern(false));
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
        b.fill(0, 1, FACADE, WIDTH - 1, 1, FACADE, RUNIC.base());
        b.fill(0, 11, FACADE, WIDTH - 1, 11, FACADE, chiseledRunic());
        b.fill(0, 15, FACADE, WIDTH - 1, 15, FACADE, Blocks.GILDED_BLACKSTONE);
        // A battered plinth: the foot of the facade slopes out onto the terrace, except at the door.
        for (int x = 0; x < WIDTH; x++) {
            if (Math.abs(x - MID) > 3) {
                b.set(x, 1, FACADE - 1, stairs(RUNIC, IN, false));
            }
        }
        // Buttresses up the facade's corners: vertical slabs on the plinth.
        for (int x : new int[]{0, WIDTH - 1}) {
            b.fill(x, 2, FACADE - 1, x, 14, FACADE - 1, facing(DwarvenBlocks.RUNIC_DEEPSLATE_BRICK_VERTICAL_SLAB, OUT));
        }
        // Corbels under the cornice.
        for (int x = 1; x < WIDTH; x += 2) {
            b.corbel(x, 15, FACADE - 1, OUT);
        }
        // Cornice overhanging the terrace, and battlements along the top.
        for (int x = 0; x < WIDTH; x++) {
            b.set(x, 16, FACADE - 1, stairs(Blocks.DEEPSLATE_TILE_STAIRS, OUT, true));
            if (x % 2 == 1) {
                b.set(x, HEIGHT - 1, FACADE, Blocks.AIR);
            } else {
                b.set(x, HEIGHT - 1, FACADE, wall(RUNIC));
            }
        }

        // The passage, five wide and seven tall, with a portcullis drawn up into the arch.
        b.air(MID - 2, 1, FACADE, MID + 2, 7, DEPTH - 1);
        b.fill(MID - 2, 0, FACADE, MID + 2, 0, DEPTH - 1, RUNIC.base());
        b.fill(MID - 1, 1, FACADE, MID + 1, 1, DEPTH - 1, Blocks.RED_CARPET);
        b.fill(MID - 2, 7, FACADE + 1, MID + 2, 7, FACADE + 1, Blocks.IRON_BARS);
        b.archTop(MID - 2, MID + 2, 7, FACADE);
        b.set(MID, 7, DEPTH - 2, lantern(true));
        // Chiseled runic door frame with a gold keystone.
        b.fill(MID - 3, 1, FACADE, MID - 3, 8, FACADE, chiseledRunic());
        b.fill(MID + 3, 1, FACADE, MID + 3, 8, FACADE, chiseledRunic());
        b.fill(MID - 3, 8, FACADE, MID + 3, 8, FACADE, chiseledRunic());
        b.set(MID, 8, FACADE, Blocks.GOLD_BLOCK);

        // The clan crest above the door, in a gilded lozenge.
        b.crest(MID, 13, FACADE, OUT);
        for (int[] p : new int[][]{{-1, 12}, {1, 12}, {-1, 14}, {1, 14}, {-2, 13}, {2, 13}}) {
            b.set(MID + p[0], p[1], FACADE, Blocks.GILDED_BLACKSTONE);
        }
        b.set(MID, 12, FACADE, chiseledRunic());
        b.set(MID, 14, FACADE, chiseledRunic());

        // Pillars standing out in front of the facade, with banners between them and the door.
        for (int x : new int[]{4, WIDTH - 5}) {
            b.column(x, FACADE - 1, 14, IN);
            b.set(x, 15, FACADE - 1, Blocks.GOLD_BLOCK);
        }
        b.banner(MID - 5, 10, FACADE - 1, OUT, banner, ModBannerPatterns.CLAN_ANVIL.in(DyeColor.YELLOW));
        b.banner(MID + 5, 10, FACADE - 1, OUT, banner, ModBannerPatterns.CLAN_ANVIL.in(DyeColor.YELLOW));
        b.set(MID - 5, 6, FACADE - 1, lantern(false));
        b.set(MID - 5, 5, FACADE - 1, wall(RUNIC));
        b.set(MID + 5, 6, FACADE - 1, lantern(false));
        b.set(MID + 5, 5, FACADE - 1, wall(RUNIC));
        b.fill(MID - 5, 1, FACADE - 1, MID - 5, 4, FACADE - 1, wall(RUNIC));
        b.fill(MID + 5, 1, FACADE - 1, MID + 5, 4, FACADE - 1, wall(RUNIC));

        // Arrow slits.
        for (int x : new int[]{2, WIDTH - 3}) {
            b.fill(x, 8, FACADE, x, 9, FACADE, Blocks.IRON_BARS);
            b.corbel(x, 7, FACADE - 1, OUT);
        }

        // Wanted Posters on the facade either side of the door, between the frame and the banner walls.
        b.set(MID - 4, 3, FACADE - 1, AdventuringBlocks.WANTED_POSTER.poster(OUT, 1));
        b.set(MID + 4, 2, FACADE - 1, AdventuringBlocks.WANTED_POSTER.poster(OUT, 3));

        b.dwarf(MID - 3, 1, 2);
        b.dwarf(MID + 3, 1, 2);

        // The waystone beside the approach, naming the hold to whoever comes up to the gate.
        b.set(MID + 2, 1, 1, facing(DwarvenBlocks.WAYSTONE, OUT));
        b.set(MID + 2, 2, 1, facing(DwarvenBlocks.WAYSTONE, OUT).with(WaystoneBlock.HALF, DoubleBlockHalf.UPPER));
    }
}
