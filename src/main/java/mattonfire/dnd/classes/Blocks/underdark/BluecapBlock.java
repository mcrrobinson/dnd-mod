package mattonfire.dnd.classes.Blocks.underdark;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.feature.ConfiguredFeature;

/**
 * The Bluecap: an {@link UnderdarkFungusBlock} that bone meal grows into a small nightlight tower (1×1, 6-9 tall), 40%
 * of the time, like a vanilla mushroom.
 */
public class BluecapBlock extends UnderdarkFungusBlock implements Fertilizable {
    public static final RegistryKey<ConfiguredFeature<?, ?>> SMALL_TOWER = RegistryKey.of(
            RegistryKeys.CONFIGURED_FEATURE, new Identifier(DnDClasses.MOD_ID, "underdark/small_nightlight_tower"));

    public BluecapBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return random.nextFloat() < 0.4F;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        GiantFungusSporesBlock.growFeature(world, random, pos, state, SMALL_TOWER);
    }
}
