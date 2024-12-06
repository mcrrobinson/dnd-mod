package mattonfire.dnd.classes;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;

public class CustomFireballEntity extends FireballEntity {
    private int explosionPower = 4; // Adjust explosion power (default is often 1 or 2)
    private static final BlockState fire = Blocks.FIRE.getDefaultState();
    private static final int sphere_radius = 5;

    public CustomFireballEntity(EntityType<? extends FireballEntity> entityType, World world) {
        super(entityType, world);
    }

    public void setFireBlocks(World world, int rx, int blockPosY, int z, BlockState blockState, int radius) {
        if (world.getBlockState(new BlockPos(rx, blockPosY, z)).isAir()) {
            world.setBlockState(new BlockPos(rx, blockPosY, z), blockState);
        }
    }

    public void startStaffAction(BlockPos pos, World world, boolean entityPlacement) {
        if (!world.isClient) {
            int blockPosX = pos.getX();
            int blockPosZ = pos.getZ();
            int blockPosY = pos.getY();
            if (entityPlacement)
                blockPosY -= 1;

            int radius_sqr = sphere_radius * sphere_radius;
            for (int x = -sphere_radius; x <= sphere_radius; x++) {
                int hh = (int) Math.sqrt(radius_sqr - x * x);
                int rx = blockPosX + x;
                int ph = blockPosZ + hh;

                for (int z = blockPosZ - hh; z < ph; z++) {
                    setFireBlocks(world, rx, blockPosY + 1, z, fire, sphere_radius);
                }
            }
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        // Cause an explosion on impact
        if (!this.world.isClient) {
            world.createExplosion(this, this.getX(), this.getY(), this.getZ(), explosionPower,
                    ExplosionSourceType.NONE);
            this.discard(); // Remove fireball after explosion
        }
    }
}
