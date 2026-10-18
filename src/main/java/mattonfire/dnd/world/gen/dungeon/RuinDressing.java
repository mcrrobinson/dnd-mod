package mattonfire.dnd.world.gen.dungeon;

import mattonfire.dnd.classes.Blocks.RubbleBlock;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.RuinBlocks;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.Direction;

/**
 * The Dwarven Ruin's props: rubble heaps in drifts of Collapsed Rubble, broken pillars, cracked anvils,
 * headless statues, barrels and seams of gilded stone. Like {@link CryptDressing} and {@link WarrenDressing}, rooms call these
 * after building (only for {@link DungeonTheme#DWARVEN_RUIN}) with spots they know are clear of their
 * doors, chests, spawn points and traps. Props go only into air ({@link DungeonPiece.Builder#setIfAir}),
 * a broken pillar only takes out the pillar's own blocks, and every draw comes from the builder's
 * separate {@code dressing} random, so dressing never moves a room's chests, traps or puzzle.
 */
final class RuinDressing {
    private RuinDressing() {
    }

    static boolean applies(DungeonPiece piece) {
        return piece.theme() == DungeonTheme.DWARVEN_RUIN;
    }

    private static BlockState rubbleBlock(DungeonPiece.Builder b) {
        float r = b.dressing.nextFloat();
        return (r < 0.5F ? Blocks.COBBLED_DEEPSLATE : r < 0.75F ? Blocks.CRACKED_DEEPSLATE_BRICKS : r < 0.9F ? Blocks.TUFF
                : Blocks.GRAVEL).getDefaultState();
    }

    private static BlockState rubbleSlab(DungeonPiece.Builder b) {
        Block slab = b.dressing.nextBoolean() ? Blocks.COBBLED_DEEPSLATE_SLAB : ModBlockFamilies.CRACKED_RUNIC_DEEPSLATE_BRICKS.slab();
        return slab.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    /** {@code layers} of Collapsed Rubble. */
    static BlockState rubbleLayers(int layers) {
        return RuinBlocks.COLLAPSED_RUBBLE.getDefaultState().with(RubbleBlock.LAYERS, layers);
    }

    /**
     * A drift of Collapsed Rubble round (x, z): up to {@code max} layers on each free floor cell beside it,
     * thinning out, so a heap looks like it spilled.
     */
    static void rubbleDrift(DungeonPiece.Builder b, int x, int z, int max) {
        for (Direction side : Direction.Type.HORIZONTAL) {
            if (b.dressing.nextFloat() < 0.6F) {
                b.setIfAir(x + side.getOffsetX(), 1, z + side.getOffsetZ(), rubbleLayers(1 + b.dressing.nextInt(max)));
            }
        }
    }

    /** A heap of fallen stone at (x, z): a block or two, with a slab on top or beside it, in a drift of rubble. */
    static void rubble(DungeonPiece.Builder b, int x, int z) {
        if (!b.setIfAir(x, 1, z, rubbleBlock(b))) {
            return;
        }
        float r = b.dressing.nextFloat();
        if (r < 0.35F) {
            b.setIfAir(x, 2, z, rubbleSlab(b));
        } else if (r < 0.7F) {
            Direction side = Direction.fromHorizontal(b.dressing.nextInt(4));
            b.setIfAir(x + side.getOffsetX(), 1, z + side.getOffsetZ(), rubbleSlab(b));
        }
        rubbleDrift(b, x, z, 4);
    }

    /** A cracked anvil (chipped or damaged) facing {@code facing}. */
    static void anvil(DungeonPiece.Builder b, int x, int z, Direction facing) {
        Block anvil = b.dressing.nextBoolean() ? Blocks.CHIPPED_ANVIL : Blocks.DAMAGED_ANVIL;
        b.setIfAir(x, 1, z, anvil.getDefaultState().with(AnvilBlock.FACING, facing));
    }

    /** One wall-side prop: a cracked anvil, a barrel or two, or a heap of rubble. */
    static void wallProp(DungeonPiece.Builder b, int x, int z, Direction facing) {
        switch (b.dressing.nextInt(3)) {
            case 0 -> anvil(b, x, z, facing);
            case 1 -> {
                if (b.setIfAir(x, 1, z, Blocks.BARREL.getDefaultState()) && b.dressing.nextFloat() < 0.3F) {
                    b.setIfAir(x, 2, z, Blocks.BARREL.getDefaultState());
                }
            }
            default -> rubble(b, x, z);
        }
    }

    /**
     * Breaks the pillar at (x, z) built by {@code Builder.column} up to {@code top}: its shaft is gone
     * from a random height up to just under the ceiling, leaving a stump on the floor and the capital
     * hanging from the ceiling, with rubble from it beside the stump. Only the pillar's own blocks go.
     */
    static void brokenColumn(DungeonPiece.Builder b, int x, int z, int top) {
        Block pillar = b.palette.pillar();
        int from = 2 + b.dressing.nextInt(2);
        for (int y = from; y < top - 1; y++) {
            BlockState now = b.get(x, y, z);
            if (now != null && now.isOf(pillar)) {
                b.set(x, y, z, Blocks.AIR.getDefaultState());
            }
        }
        Direction side = Direction.fromHorizontal(b.dressing.nextInt(4));
        b.setIfAir(x + side.getOffsetX(), 1, z + side.getOffsetZ(), pillar.getDefaultState()
                .with(net.minecraft.block.PillarBlock.AXIS, side.getAxis()));
        if (b.dressing.nextBoolean()) {
            Direction other = side.rotateYClockwise();
            b.setIfAir(x + other.getOffsetX(), 1, z + other.getOffsetZ(), rubbleSlab(b));
        }
        rubbleDrift(b, x, z, 3);
    }

    /**
     * A headless statue of a dwarf lord: a polished blackstone plinth, a polished deepslate body and
     * a chiselled deepslate chest, its head long gone; now and then fallen, leaving only the plinth and rubble.
     */
    static void statue(DungeonPiece.Builder b, int x, int z) {
        if (!b.setIfAir(x, 1, z, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState())) {
            return;
        }
        if (b.dressing.nextFloat() < 0.25F) {
            Direction side = Direction.fromHorizontal(b.dressing.nextInt(4));
            b.setIfAir(x + side.getOffsetX(), 1, z + side.getOffsetZ(), rubbleBlock(b));
            return;
        }
        b.setIfAir(x, 2, z, Blocks.POLISHED_DEEPSLATE.getDefaultState());
        b.setIfAir(x, 3, z, Blocks.CHISELED_DEEPSLATE.getDefaultState());
        if (b.dressing.nextFloat() < 0.3F) {
            b.setIfAir(x, 4, z, Blocks.POLISHED_DEEPSLATE_WALL.getDefaultState());
        }
    }

    /** A bit of the hold's wealth left behind: gilded blackstone, sometimes with a barrel beside it. */
    static void hoardScraps(DungeonPiece.Builder b, int x, int z) {
        b.setIfAir(x, 1, z, Blocks.GILDED_BLACKSTONE.getDefaultState());
        if (b.dressing.nextFloat() < 0.4F) {
            b.setIfAir(x, 2, z, Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM));
        }
    }

    /**
     * Along each wall, either side of its middle (where doors are), at {@code from} blocks from each
     * end: a wall prop ({@link #wallProp}), each with {@code chance}.
     */
    static void wallProps(DungeonPiece.Builder b, int width, int depth, int from, float chance) {
        for (Direction wall : Direction.Type.HORIZONTAL) {
            boolean alongX = wall.getAxis() == Direction.Axis.Z;
            int length = alongX ? width : depth;
            int fixed = switch (wall) {
                case NORTH, WEST -> 2;
                case SOUTH -> depth - 3;
                default -> width - 3;
            };
            for (int start : new int[]{from, length - 1 - from}) {
                if (b.dressing.nextFloat() >= chance) {
                    continue;
                }
                int x = alongX ? start : fixed;
                int z = alongX ? fixed : start;
                wallProp(b, x, z, wall.getOpposite());
            }
        }
    }
}
