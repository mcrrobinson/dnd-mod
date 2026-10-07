package mattonfire.dnd.world.gen.fortress;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * A forge: a lava channel behind an iron grate along the back wall, a row of blast furnaces, a
 * great hearth in the middle, anvils and smithing tables, tools hung on the walls.
 */
public class ForgePiece extends FortressPiece {
    static final int WIDTH = 13;
    static final int DEPTH = 13;
    static final int HEIGHT = 9;
    private static final int MID = WIDTH / 2;

    public ForgePiece(BlockBox box, Direction facing, long seed) {
        super(DwarvenFortressStructures.FORGE, box, facing, seed);
    }

    public ForgePiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.FORGE, nbt);
    }

    public static ForgePiece create(Random random, BlockPos front, Direction facing) {
        return new ForgePiece(boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        int back = DEPTH - 1;
        b.shell(WIDTH, HEIGHT, DEPTH, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        b.doorway(MID, 0, 3, 4);

        // The lava channel, one deep, behind a grate.
        for (int x = 2; x <= WIDTH - 3; x++) {
            b.set(x, -1, back - 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
            b.set(x, 0, back - 1, Blocks.LAVA);
            b.set(x, 1, back - 1, Blocks.IRON_BARS);
            b.set(x, 2, back - 1, Blocks.AIR);
        }
        b.set(1, 0, back - 1, Blocks.MAGMA_BLOCK);
        b.set(WIDTH - 2, 0, back - 1, Blocks.MAGMA_BLOCK);
        b.fill(1, HEIGHT - 2, back - 1, WIDTH - 2, HEIGHT - 2, back - 1, stairs(Blocks.DEEPSLATE_TILE_STAIRS, OUT, true));

        // Blast furnaces down the left wall, a chimney hood over them.
        for (int z = 2; z <= back - 3; z += 2) {
            b.set(1, 1, z, lit(facing(Blocks.BLAST_FURNACE, Direction.EAST)));
            b.set(1, 2, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            b.set(1, 1, z + 1, facing(Blocks.SMOKER, Direction.EAST));
        }
        for (int z = 1; z < back - 1; z++) {
            b.set(2, 4, z, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true));
        }

        // Anvils, smithing tables and a grindstone down the right wall.
        Block[] anvils = {Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL};
        b.set(WIDTH - 2, 1, 2, facing(b.pick(anvils), IN));
        b.set(WIDTH - 2, 1, 3, Blocks.SMITHING_TABLE);
        b.set(WIDTH - 2, 1, 5, facing(b.pick(anvils), IN));
        b.set(WIDTH - 2, 1, 6, Blocks.STONECUTTER);
        b.set(WIDTH - 2, 1, 8, facing(Blocks.GRINDSTONE, Direction.WEST));
        b.set(WIDTH - 2, 1, 9, Blocks.SMITHING_TABLE);

        // The great hearth: a lava cauldron in a gilded ring, a chain-hung hood above.
        b.fill(MID - 1, 1, 5, MID + 1, 1, 7, Blocks.GILDED_BLACKSTONE);
        b.set(MID, 1, 6, Blocks.LAVA_CAULDRON);
        b.set(MID - 1, 2, 5, Blocks.CAMPFIRE);
        b.set(MID + 1, 2, 7, Blocks.CAMPFIRE);
        b.fill(MID - 1, 5, 5, MID + 1, 5, 7, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.set(MID, 5, 6, Blocks.IRON_BARS);
        b.fill(MID, 6, 6, MID, HEIGHT - 2, 6, Blocks.CHAIN);
        b.set(MID - 1, 4, 5, lantern(true));
        b.set(MID + 1, 4, 7, lantern(true));

        // Chests of tools and ingots, and work hung on the walls.
        b.chest(2, 1, back - 2, Direction.EAST, FORGE);
        b.chest(WIDTH - 3, 1, back - 2, Direction.WEST, FORGE);
        ItemStack[] work = {new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.GOLDEN_AXE),
                new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.SHIELD), new ItemStack(Items.IRON_SWORD)};
        for (int z = 2; z <= back - 3; z += 3) {
            b.frame(WIDTH - 2, 3, z, Direction.WEST, b.pick(work).copy());
        }
        b.set(3, HEIGHT - 2, 3, lantern(true));
        b.set(WIDTH - 4, HEIGHT - 2, 3, lantern(true));

        b.dwarf(MID - 2, 1, 3);
        b.dwarf(MID + 2, 1, 8);
    }
}
