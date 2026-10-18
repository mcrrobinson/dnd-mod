package mattonfire.dnd.world.gen.fortress;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Obstacles.types.CraftElementObstacles;
import mattonfire.dnd.classes.Registry.DwarvenBlocks;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The treasury behind the throne: a low runic vault heaped with coins behind a mithral grille, with
 * treasure chests on gilded plinths and the odd block of gold, emerald or diamond. Always watched, and shut behind a
 * Clockwork Door (Hard): an Artificer or a Rogue repairs it open, anyone else smashes it and angers
 * the dwarves who see it.
 */
public class TreasuryPiece extends FortressPiece {
    private static final Block COINS = DwarvenBlocks.TREASURY_COIN_PILE;
    private static final int WIDTH = 13;
    private static final int DEPTH = 11;
    private static final int HEIGHT = 7;
    private static final int MID = WIDTH / 2;

    public TreasuryPiece(BlockBox box, Direction facing, long seed) {
        super(DwarvenFortressStructures.TREASURY, box, facing, seed);
    }

    public TreasuryPiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.TREASURY, nbt);
    }

    public static TreasuryPiece create(Random random, BlockPos front, Direction facing) {
        return new TreasuryPiece(boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, random.nextLong());
    }

    @Override
    protected void build(Builder b) {
        b.shell(WIDTH, HEIGHT, DEPTH, RUNIC.base().getDefaultState());
        b.air(MID - 1, 1, 0, MID + 1, 3, 0);
        b.fill(MID - 1, 1, 1, MID + 1, 1, 1, DwarvenBlocks.MITHRAL_LATTICE);
        b.set(MID, 1, 1, Blocks.AIR);
        for (int x = 1; x < WIDTH - 1; x++) {
            b.set(x, HEIGHT - 1, 1, Blocks.GILDED_BLACKSTONE);
            b.set(x, HEIGHT - 1, DEPTH - 2, Blocks.GILDED_BLACKSTONE);
        }

        // Heaps of coins along the walls, highest in the corners, with the odd gold or emerald block showing.
        // The heap tops are part-piles, so the heaps slope. (Same random draws as the old gold-block heaps.)
        Block[] hoard = {COINS, COINS, COINS, Blocks.GOLD_BLOCK};
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int z = 2; z < DEPTH - 1; z++) {
                int edge = Math.min(Math.min(x - 1, WIDTH - 2 - x), DEPTH - 2 - z);
                if (edge > 1 || Math.abs(x - MID) <= 1) {
                    continue;
                }
                int heap = 2 - edge + (b.chance(0.4F) ? 1 : 0);
                for (int y = 1; y <= heap; y++) {
                    boolean emerald = b.chance(0.04F);
                    Block block = emerald ? Blocks.EMERALD_BLOCK : b.pick(hoard);
                    if (block == COINS || y == heap && !emerald) {
                        int layers = y < heap ? 8 : 3 + Math.floorMod(x * 31 + z * 17, 5);
                        b.set(x, y, z, COINS.getDefaultState().with(SnowBlock.LAYERS, layers));
                    } else {
                        b.set(x, y, z, block);
                    }
                }
            }
        }
        if (b.chance(0.35F)) {
            b.set(1, 4, DEPTH - 2, Blocks.DIAMOND_BLOCK);
        }

        // Treasure chests on plinths down the middle.
        for (int z = 4; z <= DEPTH - 3; z += 3) {
            b.set(MID - 2, 1, z, Blocks.GILDED_BLACKSTONE);
            b.chest(MID - 2, 2, z, Direction.EAST, TREASURY);
            b.set(MID + 2, 1, z, Blocks.GILDED_BLACKSTONE);
            b.chest(MID + 2, 2, z, Direction.WEST, TREASURY);
        }
        b.set(MID, 1, DEPTH - 2, Blocks.AMETHYST_BLOCK);
        b.set(MID, 2, DEPTH - 2, Blocks.AMETHYST_CLUSTER.getDefaultState().with(AmethystClusterBlock.FACING, Direction.UP));

        for (int z = 3; z < DEPTH - 1; z += 3) {
            b.set(MID, HEIGHT - 2, z, lantern(true));
        }
        b.dwarf(MID, 1, 3);

        // Last, so the randoms above match treasuries generated before the door was added.
        b.obstacle(MID - 1, 1, 0, MID + 1, 3, 0, CraftElementObstacles.CLOCKWORK_DOOR, Tier.HARD, false);
    }
}
