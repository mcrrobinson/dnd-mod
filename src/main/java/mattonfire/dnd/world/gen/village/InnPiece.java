package mattonfire.dnd.world.gen.village;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Blocks.hobbit.HobbitBlocks;
import mattonfire.dnd.classes.Registry.AdventuringBlocks;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.entity.InnkeeperEntity;
import mattonfire.dnd.tavern.Tavern;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.random.Random;

/**
 * The Green Dragon: a thatched inn with half-timbered walls, a round green door in a stone arch, a
 * roaring fire on a soot-blackened hearth, a bar backed by ale casks you can draw from and tables laid
 * for supper. The innkeeper tends the bar (food and drink for emeralds) and
 * a bounty board on the east wall posts jobs for adventurers. Up a ladder in the north-east corner,
 * a sleeping loft under the thatch has four beds for guests who rent a room.
 */
public class InnPiece extends HobbitPiece {
    /** The inn's name, written on the Room Keys its innkeeper sells. */
    public static final String NAME = "Green Dragon";
    private static final int WIDTH = 15;
    private static final int DEPTH = 11;
    private static final int HEIGHT = 13;

    public InnPiece(BlockBox box, Direction facing, long seed) {
        super(HobbitVillageStructures.INN, box, facing, seed);
    }

    public InnPiece(NbtCompound nbt) {
        super(HobbitVillageStructures.INN, nbt);
    }

    public static InnPiece create(Random random, int x, int y, int z, Direction facing) {
        return new InnPiece(centeredBox(x, y, z, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    private static Block fieldstone(Builder b) {
        return b.chance(0.4F) ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE;
    }

    @Override
    protected void build(Builder b) {
        b.lawn(0, 0, WIDTH - 1, DEPTH - 1, HEIGHT - 1);
        b.fill(1, 0, 1, 13, 0, 9, Blocks.SPRUCE_PLANKS);

        // Walls: a fieldstone footing, wattle and daub under a timbered top course, and timber corner posts.
        for (int x = 1; x <= 13; x++) {
            for (int z = 1; z <= 9; z++) {
                if (x == 1 || x == 13 || z == 1 || z == 9) {
                    b.set(x, 1, z, fieldstone(b));
                    b.fill(x, 2, z, x, 3, z, ModBlockFamilies.WATTLE_AND_DAUB.base());
                    b.set(x, 4, z, ModBlockFamilies.TIMBERED_WATTLE_AND_DAUB.base());
                }
            }
        }
        for (int[] p : new int[][]{{1, 1}, {13, 1}, {1, 9}, {13, 9}}) {
            b.fill(p[0], 1, p[1], p[0], 4, p[1], pillar(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        }
        b.air(2, 1, 2, 12, 4, 8);

        this.roof(b);

        // Fireplace in the west wall on a sooty hearth, chimney up the outside.
        b.fill(1, 1, 4, 1, 4, 6, ModBlockFamilies.HEARTH_STONE.base());
        b.set(1, 1, 5, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.FACING, Direction.EAST));
        b.set(1, 2, 5, Blocks.AIR);
        b.fill(0, 1, 4, 0, 2, 6, ModBlockFamilies.HEARTH_STONE.base());
        b.fill(0, 3, 5, 0, 11, 5, Blocks.BRICKS);
        b.set(0, 12, 5, Blocks.CAMPFIRE);
        b.fill(2, 3, 4, 2, 3, 6, topSlab(Blocks.SPRUCE_SLAB));
        b.candles(2, 4, 4);
        b.pot(2, 4, 5);
        b.candles(2, 4, 6);

        // Round door and windows.
        b.roundDoor(7, 1, 1, Blocks.WARPED_DOOR, Blocks.WARPED_PLANKS);
        b.roundWindow(3, 3, 1, true);
        b.roundWindow(11, 3, 1, true);
        b.roundWindow(4, 3, 9, true);
        b.roundWindow(10, 3, 9, true);
        b.roundWindow(13, 3, 5, false);

        this.furnish(b);
        this.loft(b);
        this.frontage(b);
    }

    /**
     * A thatched gable roof: thatch stairs stepping up from front and back to a slab ridge along x, thatch
     * along the gable eaves and timbered wattle and daub filling the gable ends.
     */
    private void roof(Builder b) {
        for (int k = 0; k <= 5; k++) {
            int y = 5 + k;
            int front = k;
            int rear = 10 - k;
            for (int x = 0; x < WIDTH; x++) {
                if (k == 5) {
                    b.set(x, y, front, slab(this.thatch(x, y, front), false));
                } else {
                    b.set(x, y, front, stairs(this.thatch(x, y, front), IN, false));
                    b.set(x, y, rear, stairs(this.thatch(x, y, rear), OUT, false));
                }
            }
            for (int z = front + 1; z < rear; z++) {
                if (k == 0) {
                    b.fill(1, y, z, 13, y, z, Blocks.OAK_PLANKS);
                    b.set(0, y, z, this.thatch(0, y, z).base());
                    b.set(WIDTH - 1, y, z, this.thatch(WIDTH - 1, y, z).base());
                } else {
                    b.set(1, y, z, ModBlockFamilies.TIMBERED_WATTLE_AND_DAUB.base());
                    b.set(13, y, z, ModBlockFamilies.TIMBERED_WATTLE_AND_DAUB.base());
                }
            }
        }
    }

    private void furnish(Builder b) {
        b.fill(5, 1, 2, 9, 1, 4, Blocks.GREEN_CARPET);

        // Bar counter with drinks and nibbles; behind it a row of ale casks to draw from, larder shelves and
        // more casks on top, and herbs and onions hanging from the ceiling.
        for (int x = 3; x <= 9; x++) {
            b.set(x, 1, 6, topSlab(Blocks.SPRUCE_SLAB));
            if (b.chance(0.45F)) {
                b.frame(x, 2, 6, Direction.UP, new ItemStack(Items.HONEY_BOTTLE));
            } else {
                b.tableFood(x, 2, 6);
            }
            b.set(x, 1, 8, facing(HobbitBlocks.ALE_CASK, OUT));
            if (x % 2 == 0) {
                b.set(x, 2, 8, facing(HobbitBlocks.ALE_CASK, OUT));
            } else {
                b.larder(x, 2, 8, OUT, PANTRY);
            }
            b.herbs(x, 4, 8, x % 2 == 0);
        }
        b.larder(10, 1, 8, OUT, ALE);
        b.set(11, 1, 8, Blocks.CAKE);

        // Tavern tables by the front windows and along the east wall, with benches either side.
        for (int x : new int[]{3, 4, 10, 11}) {
            b.set(x, 1, 3, AdventuringBlocks.SPRUCE_TAVERN_TABLE);
            b.tableFood(x, 2, 3);
            b.set(x, 1, 4, AdventuringBlocks.OAK_TAVERN_BENCH.along(Direction.Axis.X));
            b.set(x, 1, 2, AdventuringBlocks.OAK_TAVERN_BENCH.along(Direction.Axis.X));
        }
        for (int z = 5; z <= 6; z++) {
            b.set(11, 1, z, AdventuringBlocks.SPRUCE_TAVERN_TABLE);
            b.tableFood(11, 2, z);
            b.set(12, 1, z, AdventuringBlocks.OAK_TAVERN_BENCH.along(Direction.Axis.Z));
        }
        b.set(10, 1, 5, AdventuringBlocks.OAK_TAVERN_BENCH.along(Direction.Axis.Z));

        b.set(4, 4, 4, lantern(true));
        b.set(10, 4, 4, lantern(true));
        b.set(7, 4, 6, lantern(true));
        b.set(3, 4, 7, lantern(true));
        b.set(11, 4, 7, lantern(true));

        // Room Keys from this innkeeper open the beds anywhere in the inn (the loft).
        b.hobbit(6, 1, 7, Tavern.INNKEEPER, hobbit -> ((InnkeeperEntity) hobbit).setInn(this.boundingBox, NAME));
        b.hobbit(3, 1, 5);
        b.hobbit(9, 1, 4);

        // The bounty board: two boards side by side on the east wall, past the end of the bar.
        b.set(12, 2, 7, facing(Tavern.BOUNTY_BOARD, Direction.WEST));
        b.set(12, 2, 8, facing(Tavern.BOUNTY_BOARD, Direction.WEST));
        // Wanted Posters: one over the bounty board, one on the front wall by the door.
        b.set(12, 3, 7, AdventuringBlocks.WANTED_POSTER.poster(Direction.WEST, 0));
        b.set(5, 3, 2, AdventuringBlocks.WANTED_POSTER.poster(Direction.NORTH, 2));
    }

    /**
     * The guest loft on the ceiling boards (y 5), under the thatch: a ladder up the east wall by the
     * door, four beds along the eaves (z 3 and 7, where there's a block of headroom) and a walkway
     * down the middle (z 4-6) with full headroom. Uses no random draws, so the rest of the inn is
     * unchanged.
     */
    private void loft(Builder b) {
        for (int y = 1; y <= 6; y++) {
            b.set(12, y, 3, facing(Blocks.LADDER, Direction.WEST));
        }
        b.bed(3, 6, 3, Direction.WEST, DyeColor.GREEN);
        b.bed(10, 6, 3, Direction.EAST, DyeColor.GREEN);
        b.bed(3, 6, 7, Direction.WEST, DyeColor.GREEN);
        b.bed(10, 6, 7, Direction.EAST, DyeColor.GREEN);
        b.fill(3, 6, 5, 11, 6, 5, Blocks.GREEN_CARPET);
        b.set(7, 9, 5, lantern(true));
        // Nightstands with a candle, between the beds.
        for (int z : new int[]{3, 7}) {
            b.set(6, 6, z, facing(Blocks.BARREL, Direction.UP));
            b.set(6, 7, z, Blocks.CANDLE.getDefaultState().with(net.minecraft.block.CandleBlock.CANDLES, 2)
                    .with(net.minecraft.block.CandleBlock.LIT, true));
        }
    }

    private void frontage(Builder b) {
        for (int x = 1; x <= 13; x++) {
            if (Math.abs(x - 7) > 2 && b.chance(0.75F)) {
                b.flower(x, 1, 0);
            }
        }
        b.ground(7, 0, Blocks.DIRT_PATH);

        // The inn's banner beside the door: the Green Dragon, green on yellow.
        b.banner(5, 4, 0, OUT, DyeColor.YELLOW, ModBannerPatterns.GREEN_DRAGON.in(DyeColor.GREEN));
        b.set(4, 1, 0, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, 0));
        b.set(4, 2, 0, Blocks.AIR);
        BlockPos sign = b.pos(4, 1, 0);
        if (b.chunkBox.contains(sign) && b.world.getBlockEntity(sign) instanceof SignBlockEntity entity) {
            entity.setTextOnRow(0, Text.literal("~ The ~"));
            entity.setTextOnRow(1, Text.literal("Green Dragon"));
            entity.setTextOnRow(2, Text.literal("Inn"));
            entity.setTextOnRow(3, Text.literal("Ales & Suppers"));
        }
    }
}
