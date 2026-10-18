package mattonfire.dnd.world.gen;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.BlockFamily;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

/**
 * Block states and ids the worldgen pieces share. Piece code imports these statically
 * ({@code import static mattonfire.dnd.world.gen.PieceBlocks.*}).
 */
public final class PieceBlocks {
    /**
     * The block-update flags worldgen writes with: tell clients ({@link Block#NOTIFY_LISTENERS}), but don't
     * update neighbours, which may sit in chunks that aren't there yet.
     */
    public static final int UPDATE = Block.NOTIFY_LISTENERS;

    private PieceBlocks() {
    }

    /** The mod's chest loot table {@code dndclasses:chests/<name>}. */
    public static Identifier loot(String name) {
        return new Identifier(DnDClasses.MOD_ID, "chests/" + name);
    }

    public static BlockState stairs(Block block, Direction facing, boolean upsideDown) {
        return block.getDefaultState().with(StairsBlock.FACING, facing)
                .with(StairsBlock.HALF, upsideDown ? BlockHalf.TOP : BlockHalf.BOTTOM);
    }

    public static BlockState slab(Block block, boolean top) {
        return block.getDefaultState().with(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    public static BlockState topSlab(Block block) {
        return slab(block, true);
    }

    public static BlockState bottomSlab(Block block) {
        return slab(block, false);
    }

    /** A log or other pillar block lying along {@code axis}. */
    public static BlockState pillar(Block block, Direction.Axis axis) {
        return block.getDefaultState().with(PillarBlock.AXIS, axis);
    }

    /** The stairs of {@code family} facing {@code facing} (see {@link #stairs(Block, Direction, boolean)}). */
    public static BlockState stairs(BlockFamily family, Direction facing, boolean upsideDown) {
        return stairs(family.stairs(), facing, upsideDown);
    }

    /** The slab of {@code family}, in the top or bottom half. */
    public static BlockState slab(BlockFamily family, boolean top) {
        return slab(family.slab(), top);
    }

    /** The wall of {@code family} (it joins up with its neighbours once the chunk is done, see {@link #joinsNeighbours}). */
    public static BlockState wall(BlockFamily family) {
        return family.wall().getDefaultState();
    }

    /** The pillar of {@code family} along {@code axis}: its pillar shape, or the base if that lies on an axis. */
    public static BlockState pillar(BlockFamily family, Direction.Axis axis) {
        return pillar(family.has(BlockFamily.Shape.PILLAR) ? family.pillar() : family.base(), axis);
    }

    /**
     * One half of a closed door facing {@code facing} (the side you walk out of it towards): set the lower half
     * at the doorway's floor and the upper half above it.
     */
    public static BlockState door(Block door, Direction facing, boolean upper) {
        return door.getDefaultState().with(DoorBlock.FACING, facing)
                .with(DoorBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }

    /** A closed trapdoor hinged on the {@code facing} side's opposite, in the top or bottom half of its block. */
    public static BlockState trapdoor(Block trapdoor, Direction facing, boolean top) {
        return trapdoor.getDefaultState().with(TrapdoorBlock.FACING, facing)
                .with(TrapdoorBlock.HALF, top ? BlockHalf.TOP : BlockHalf.BOTTOM);
    }

    /** Leaves that never decay. */
    public static BlockState leaves(Block block) {
        return block.getDefaultState().with(LeavesBlock.PERSISTENT, true);
    }

    public static BlockState lantern(boolean hanging) {
        return Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, hanging);
    }

    public static BlockState soulLantern(boolean hanging) {
        return Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, hanging);
    }

    /** The block turned to {@code facing}, through whichever facing property it has (none: unchanged). */
    public static BlockState facing(Block block, Direction facing) {
        BlockState state = block.getDefaultState();
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return state.with(Properties.HORIZONTAL_FACING, facing);
        }
        if (state.contains(Properties.FACING)) {
            return state.with(Properties.FACING, facing);
        }
        return state;
    }

    /** The state lit, if it can be (campfires, furnaces, candles). */
    public static BlockState lit(BlockState state) {
        return state.contains(Properties.LIT) ? state.with(Properties.LIT, true) : state;
    }

    /**
     * Shapes that join up with their neighbours (fences, panes, stairs, walls, chains): a piece marks them for
     * post-processing, which fixes their shape once the chunk is finished.
     */
    public static boolean joinsNeighbours(Block block) {
        return block instanceof FenceBlock || block instanceof PaneBlock || block instanceof StairsBlock
                || block instanceof WallBlock || block instanceof ChainBlock;
    }
}
