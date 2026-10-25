package mattonfire.dnd.classes.Blocks.underdark;

import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Duergar Brazier: a squat black-iron bowl of coals on a heavy foot, the Holds' light (14). It smokes like a
 * campfire and burns whatever stands in it for 1 damage a second, except the duergar (and anything else in
 * {@link UnderdarkBlocks#BRAZIER_IMMUNE}) and fire-immune mobs.
 */
public class DuergarBrazierBlock extends Block {
    public static final float BURN_DAMAGE = 1.0F;
    public static final int BURN_INTERVAL_TICKS = 20;
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(4, 0, 4, 12, 3, 12),
            Block.createCuboidShape(6, 3, 6, 10, 6, 10),
            Block.createCuboidShape(1, 6, 1, 15, 12, 15));

    public DuergarBrazierBlock(Settings settings) {
        super(settings);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    /** Whether the brazier burns this entity: living, not fire-immune, not in {@link UnderdarkBlocks#BRAZIER_IMMUNE}. */
    public static boolean burns(Entity entity) {
        return entity instanceof LivingEntity && !entity.isFireImmune()
                && !entity.getType().isIn(UnderdarkBlocks.BRAZIER_IMMUNE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && burns(entity) && world.getTime() % BURN_INTERVAL_TICKS == 0
                && entity.getY() >= pos.getY() + 0.5) {
            entity.damage(world.getDamageSources().inFire(), BURN_DAMAGE);
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 12 / 16.0;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) {
            world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + (random.nextDouble() - 0.5) * 0.4,
                    y + random.nextDouble() * 0.2, z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.05, 0.0);
        }
        for (int i = 0; i < 2; i++) {
            world.addParticle(random.nextInt(12) == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME,
                    x + (random.nextDouble() - 0.5) * 0.6, y, z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.01, 0.0);
        }
        if (random.nextInt(10) == 0) {
            world.playSound(x, y, z, SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS,
                    0.5F + random.nextFloat() * 0.4F, 0.7F + random.nextFloat() * 0.3F, false);
        }
    }
}
