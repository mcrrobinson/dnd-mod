package mattonfire.dnd.classes.Blocks.underdark;

import mattonfire.dnd.world.underdark.UnderdarkRifts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Rift Veil: the pool of black, shimmering nothing at the bottom of an Underdark Rift (design 9, 2.6). It has no
 * collision and can't be broken, moved by pistons, replaced or washed away. A player who stands in it for a second
 * (a tick in creative) falls through into the Underdark; so does a mob pushed in. Items and XP orbs that fall in are
 * gone. Mobs don't path into it ({@code PathNodeType.DAMAGE_OTHER}, registered in {@code RiftBlocks}). What happens on
 * contact is {@link UnderdarkRifts#veilContact}.
 */
public class RiftVeilBlock extends Block {
    public RiftVeilBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // A thin sheet at the top, so it reads as a pool's surface and the crosshair can still find it.
        return Block.createCuboidShape(0.0, 12.0, 0.0, 16.0, 14.0, 16.0);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world instanceof ServerWorld server) {
            UnderdarkRifts.veilContact(server, pos, entity);
        }
    }

    @Override
    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.BLOCK;
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return false;
    }

    @Override
    public boolean canBucketPlace(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Motes drift down into it, and now and then a low hum.
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            world.addParticle(ParticleTypes.REVERSE_PORTAL, x, pos.getY() + 1.6 + random.nextDouble(), z, 0.0, -0.08, 0.0);
        }
        if (random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 0.9,
                    pos.getZ() + random.nextDouble(), 0.0, -0.4, 0.0);
        }
        if (random.nextInt(160) == 0) {
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_PORTAL_AMBIENT,
                    SoundCategory.BLOCKS, 0.35F, 0.45F + random.nextFloat() * 0.1F, false);
        }
    }
}
