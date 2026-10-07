package mattonfire.dnd.world.gen.fortress;

import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * A working mine: a rough cavern hacked out of the deepslate, its walls studded with ore and
 * amethyst, shored up with timber, with a rail line and an ore cart down the middle.
 */
public class MinePiece extends FortressPiece {
    private static final int WIDTH = 13;
    private static final int DEPTH = 13;
    private static final int HEIGHT = 10;
    private static final int MID = WIDTH / 2;

    private static final Block[] ORES = {
            Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE_DIAMOND_ORE
    };

    public MinePiece(BlockBox box, Direction facing, long seed) {
        super(DwarvenFortressStructures.MINE, box, facing, seed);
    }

    public MinePiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.MINE, nbt);
    }

    public static MinePiece create(Random random, BlockPos front, Direction facing) {
        return new MinePiece(boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    /** The cavern: a lumpy dome over the whole floor, opening onto the corridor at the front. */
    private static boolean open(int x, int y, int z, long seed) {
        if (y < 1 || x < 1 || x > WIDTH - 2 || z > DEPTH - 2 || y > HEIGHT - 2) {
            return false;
        }
        if (z == 0) {
            return Math.abs(x - MID) <= 1 && y <= 4;
        }
        double dx = (x - MID) / 5.5D;
        double dy = (y - 1) / 7.5D;
        double dz = (z - DEPTH / 2.0D) / 6.0D;
        long h = MathHelper.hashCode(x, y, z) ^ seed;
        double wobble = ((h >>> 16) & 0xFF) / 255.0D * 0.35D;
        return dx * dx + dy * dy + dz * dz < 1.0D - wobble || (Math.abs(x - MID) <= 1 && y <= 4 && z <= 3);
    }

    @Override
    protected void build(Builder b) {
        long shape = b.random.nextLong();
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                b.foundation(x, z);
                b.set(x, 0, z, b.chance(0.3F) ? Blocks.COBBLED_DEEPSLATE : Blocks.DEEPSLATE);
                for (int y = 1; y < HEIGHT; y++) {
                    float roll = b.random.nextFloat();
                    if (open(x, y, z, shape)) {
                        b.set(x, y, z, Blocks.AIR);
                        continue;
                    }
                    boolean exposed = open(x + 1, y, z, shape) || open(x - 1, y, z, shape) || open(x, y + 1, z, shape)
                            || open(x, y - 1, z, shape) || open(x, y, z + 1, shape) || open(x, y, z - 1, shape);
                    if (exposed && roll < 0.14F) {
                        b.set(x, y, z, b.pick(ORES));
                    } else if (exposed && roll < 0.17F) {
                        b.set(x, y, z, Blocks.AMETHYST_BLOCK);
                    } else {
                        b.set(x, y, z, roll < 0.3F ? Blocks.TUFF : Blocks.DEEPSLATE);
                    }
                }
            }
        }

        // The rail line in from the corridor, with an ore cart on it.
        for (int z = 0; z <= DEPTH - 3; z++) {
            b.set(MID, 0, z, Blocks.POLISHED_DEEPSLATE);
            b.set(MID, 1, z, Blocks.RAIL);
        }
        b.chestCart(MID, 1, DEPTH - 4, MINE);

        // Timber shoring and lamps.
        for (int z : new int[]{3, 8}) {
            for (int x : new int[]{MID - 3, MID + 3}) {
                b.fill(x, 1, z, x, 3, z, pillar(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
            }
            b.fill(MID - 3, 4, z, MID + 3, 4, z, pillar(Blocks.DARK_OAK_LOG, Direction.Axis.X));
            b.set(MID - 2, 3, z, lantern(true));
            b.set(MID + 2, 3, z, lantern(true));
        }

        // Amethyst growing up from the floor, and the miners' gear.
        for (int[] p : new int[][]{{2, 6}, {WIDTH - 3, 5}, {3, 10}}) {
            b.set(p[0], 0, p[1], Blocks.AMETHYST_BLOCK);
            b.set(p[0], 1, p[1], Blocks.AMETHYST_CLUSTER.getDefaultState().with(AmethystClusterBlock.FACING, Direction.UP));
        }
        b.chest(MID + 2, 1, 4, Direction.WEST, MINE);
        b.set(MID - 2, 1, 4, Blocks.CRAFTING_TABLE);
        b.frame(MID - 2, 2, 4, Direction.UP, new ItemStack(Items.IRON_PICKAXE));

        for (int i = 0; i < 2; i++) {
            b.dwarf(MID + (i == 0 ? -2 : 2), 1, 6, dwarf -> dwarf.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND,
                    new ItemStack(Items.IRON_PICKAXE)));
        }
    }
}
