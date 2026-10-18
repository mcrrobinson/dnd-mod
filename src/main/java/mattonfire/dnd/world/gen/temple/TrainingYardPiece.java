package mattonfire.dnd.world.gen.temple;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.Registry.ModTempleBlocks;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The training yard along the other side of the courtyard: a beaten floor of coarse dirt and gravel, three
 * targets on hay bales along the parapet, three sparring posts, armour stands in the Knight set, a grindstone and an anvil, and a
 * Templar or two at drill.
 */
public class TrainingYardPiece extends TemplePiece {
    static final int WIDTH = DragonTemplePlanner.SIDE_WIDTH;
    static final int DEPTH = DragonTemplePlanner.DEPTH;
    private static final Block[] FLOOR = {Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.PACKED_MUD, Blocks.DIRT_PATH};

    public TrainingYardPiece(BlockPos front, Direction facing, long seed) {
        super(DragonTempleStructures.YARD, boxFrom(front, facing, WIDTH, 4, DEPTH), facing, seed);
    }

    public TrainingYardPiece(NbtCompound nbt) {
        super(DragonTempleStructures.YARD, nbt);
    }

    @Override
    protected void build(Builder b) {
        for (int x = 1; x < WIDTH; x++) {
            for (int z = 1; z < DEPTH - 1; z++) {
                b.set(x, 0, z, b.pick(FLOOR));
            }
        }
        // Targets on hay bales along the outer parapet, facing in.
        for (int z = 5; z < DEPTH - 3; z += 6) {
            b.set(1, 1, z, Blocks.HAY_BLOCK);
            b.set(1, 2, z, Blocks.TARGET);
        }
        // Sparring posts, their padded fronts towards the middle of the yard.
        for (int z = 8; z <= 20; z += 6) {
            b.set(3, 1, z, facing(ModTempleBlocks.SPARRING_POST, Direction.EAST));
        }
        // Armour stands in the Knight set: the first in the full set, the others in what's left of it.
        ItemStack[] knight = {new ItemStack(ModItems.KNIGHT_HELMET), new ItemStack(ModItems.KNIGHT_CHESTPLATE),
                new ItemStack(ModItems.KNIGHT_LEGGINGS), new ItemStack(ModItems.KNIGHT_BOOTS)};
        b.armorStand(5, 1, 8, Direction.WEST, knight);
        b.armorStand(5, 1, 14, Direction.WEST, ItemStack.EMPTY, new ItemStack(ModItems.KNIGHT_CHESTPLATE),
                new ItemStack(Items.CHAINMAIL_LEGGINGS));
        b.armorStand(5, 1, 20, Direction.WEST, new ItemStack(ModItems.KNIGHT_HELMET), new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        b.set(6, 1, DEPTH - 3, Blocks.GRINDSTONE);
        b.set(4, 1, DEPTH - 3, facing(Blocks.CHIPPED_ANVIL, Direction.EAST));
        b.set(2, 1, DEPTH - 3, Blocks.BARREL);
        for (int z : new int[]{2, DEPTH - 3}) {
            b.set(WIDTH - 1, 1, z, Blocks.POLISHED_BLACKSTONE_WALL);
            b.set(WIDTH - 1, 2, z, lantern(false));
        }

        b.dragonborn(4, 1, 4, ModEntityTypes.DRAGONBORN_TEMPLAR);
        if (b.chance(0.5F)) {
            b.dragonborn(3, 1, 17, ModEntityTypes.DRAGONBORN_TEMPLAR);
        }
    }
}
