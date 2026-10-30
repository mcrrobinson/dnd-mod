package mattonfire.dnd.classes.Blocks;

import mattonfire.dnd.world.gen.warren.KoboldWarrenLaw;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * A Kobold Egg Clutch (design 10, 4.3): a cluster of speckled red eggs on straw in a warren's nursery. Easy to break,
 * and it drops nothing. Breaking one costs 30 standing with the Kobold Tribes whether a kobold sees it or not, and
 * if one does, the whole warren comes for you ({@link KoboldWarrenLaw#eggClutchBroken}).
 */
public class KoboldEggClutchBlock extends Block {
    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 7, 15);

    public KoboldEggClutchBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            KoboldWarrenLaw.eggClutchBroken(serverPlayer, pos);
        }
    }
}
