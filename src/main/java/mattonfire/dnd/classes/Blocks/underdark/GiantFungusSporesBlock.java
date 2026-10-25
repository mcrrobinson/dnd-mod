package mattonfire.dnd.classes.Blocks.underdark;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.feature.ConfiguredFeature;

/**
 * Zurkhwood or Nightlight Spores: a sapling for a giant Underdark fungus. Planted on Fungal Loam or mycelium where
 * the block light is 12 or less, a random tick grows it 1 time in 7 (a sapling's odds) and each bone meal 45% of the
 * time, into the configured feature {@code feature} (the same one worldgen places). Growing needs the room the fungus
 * needs; if it doesn't fit, the spores stay.
 */
public class GiantFungusSporesBlock extends PlantBlock implements Fertilizable {
    private static final VoxelShape SHAPE = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 6.0, 13.0);
    /** The brightest block light spores take root in. */
    public static final int MAX_LIGHT = 12;
    public static final float BONE_MEAL_CHANCE = 0.45F;
    public static final int RANDOM_TICK_ODDS = 7;

    private final RegistryKey<ConfiguredFeature<?, ?>> feature;

    public GiantFungusSporesBlock(String featurePath, Settings settings) {
        super(settings);
        this.feature = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new net.minecraft.util.Identifier(DnDClasses.MOD_ID,
                featurePath));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(UnderdarkBlocks.FUNGAL_LOAM) || floor.isOf(Blocks.MYCELIUM);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return super.canPlaceAt(state, world, pos) && world.getLightLevel(net.minecraft.world.LightType.BLOCK, pos) <= MAX_LIGHT;
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (random.nextInt(RANDOM_TICK_ODDS) == 0) {
            grow(world, random, pos, state);
        }
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return random.nextFloat() < BONE_MEAL_CHANCE;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        growFeature(world, random, pos, state, feature);
    }

    /**
     * Swaps the block at {@code pos} for the configured feature {@code key} grown from there; puts {@code state} back if
     * the feature doesn't fit. Shared with the Bluecap, whose bone meal grows a small nightlight tower.
     */
    public static boolean growFeature(ServerWorld world, Random random, BlockPos pos, BlockState state,
            RegistryKey<ConfiguredFeature<?, ?>> key) {
        ConfiguredFeature<?, ?> configured = world.getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE).get(key);
        if (configured == null) {
            DnDClasses.LOGGER.warn("[Underdark] no configured feature {}", key.getValue());
            return false;
        }
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NO_REDRAW);
        if (configured.generate(world, world.getChunkManager().getChunkGenerator(), random, pos)) {
            DnDClasses.LOGGER.info("[Underdark] grew {} at {} {} {}", key.getValue(), pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        world.setBlockState(pos, state, Block.NO_REDRAW);
        return false;
    }
}
