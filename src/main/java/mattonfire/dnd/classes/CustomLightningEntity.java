package mattonfire.dnd.classes;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;

public class CustomLightningEntity extends FireballEntity {
    private static final int explosionPower = 4; // Adjust explosion power (default is often 1 or 2)

    public CustomLightningEntity(EntityType<? extends FireballEntity> entityType, World world) {
        super(entityType, world);
    }

    public void startLightningAction(BlockPos pos, World world, boolean entityPlacement) {
        if (!world.isClient) {
            int blockPosX = pos.getX();
            int blockPosZ = pos.getZ();
            int blockPosY = pos.getY();
            if (entityPlacement)
                blockPosY -= 1;

            LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
            lightningEntity.setPos(blockPosX, blockPosZ, blockPosY);
            world.spawnEntity(lightningEntity);
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        // Cause an explosion on impact
        if (!this.world.isClient) {
            world.createExplosion(this, this.getX(), this.getY(), this.getZ(), explosionPower,
                    ExplosionSourceType.NONE);

            startLightningAction(new BlockPos((int) this.getX(), (int) this.getY(), (int) this.getZ()), world, false);
            this.discard(); // Remove fireball after explosion
        }
    }
}
