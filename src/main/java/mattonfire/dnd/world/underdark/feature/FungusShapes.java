package mattonfire.dnd.world.underdark.feature;

import java.util.Collection;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.classes.Registry.UnderdarkFlora;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.Material;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;

/** What the giant Underdark fungi grow on and through, and how their cap blocks show their skin. */
final class FungusShapes {
    private FungusShapes() {
    }

    /** Ground a giant fungus can stand on. */
    static boolean isSoil(BlockState state) {
        return state.isOf(UnderdarkBlocks.FUNGAL_LOAM) || state.isOf(Blocks.MYCELIUM) || state.isOf(Blocks.GRAVEL)
                || state.isOf(Blocks.CLAY) || state.isOf(Blocks.MOSS_BLOCK) || state.isOf(Blocks.DEEPSLATE)
                || state.isOf(Blocks.TUFF) || state.isOf(Blocks.STONE) || state.isOf(UnderdarkBlocks.FAERZRESS_STONE)
                || state.isIn(BlockTags.DIRT);
    }

    /** Whether a fungus may grow into {@code pos}: air or a small plant, never water or solid rock. */
    static boolean canReplace(StructureWorldAccess world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        Material material = state.getMaterial();
        return material == Material.PLANT || material == Material.REPLACEABLE_PLANT || state.isOf(Blocks.GLOW_LICHEN)
                || state.isOf(UnderdarkFlora.FAERIE_LICHEN) || state.isOf(UnderdarkFlora.ZURKHWOOD_SPORES)
                || state.isOf(UnderdarkFlora.NIGHTLIGHT_SPORES) || state.isOf(UnderdarkBlocks.BLUECAP);
    }

    /** How many replaceable blocks there are straight up from {@code origin} (origin included), up to {@code max}. */
    static int room(StructureWorldAccess world, BlockPos origin, int max) {
        BlockPos.Mutable pos = origin.mutableCopy();
        for (int i = 0; i < max; i++) {
            pos.set(origin.getX(), origin.getY() + i, origin.getZ());
            if (world.isOutOfHeightLimit(pos) || !canReplace(world, pos)) {
                return i;
            }
        }
        return max;
    }

    static boolean place(StructureWorldAccess world, BlockPos pos, BlockState state) {
        if (!canReplace(world, pos)) {
            return false;
        }
        world.setBlockState(pos, state, Block.NOTIFY_ALL);
        return true;
    }

    /**
     * Shows each placed cap block's skin only where it faces something that isn't part of the fungus (vanilla's huge
     * mushrooms do the same, so the inside faces show the gill texture).
     */
    static void dressCaps(StructureWorldAccess world, Collection<BlockPos> caps) {
        for (BlockPos pos : caps) {
            BlockState state = world.getBlockState(pos);
            if (!state.contains(ConnectingBlock.UP)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockState neighbour = world.getBlockState(pos.offset(direction));
                boolean hidden = neighbour.isOf(state.getBlock()) || neighbour.isOf(UnderdarkBlocks.ZURKHWOOD_STEM)
                        || neighbour.isOf(UnderdarkFlora.ZURKHWOOD_GILLS);
                state = state.with(ConnectingBlock.FACING_PROPERTIES.get(direction), !hidden);
            }
            world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
        }
    }
}
