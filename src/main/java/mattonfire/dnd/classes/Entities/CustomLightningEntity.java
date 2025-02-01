package mattonfire.dnd.classes;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;

public class CustomLightningEntity extends AbstractFireballEntity {
    private static final int explosionPower = 4; // Adjust explosion power (default is often 1 or 2)

    public CustomLightningEntity(EntityType<? extends AbstractFireballEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        Vec3d hitPos = hitResult.getPos();

        // Cause an explosion on impact
        if (!this.world.isClient) {
            world.createExplosion(this, hitPos.x, hitPos.y, hitPos.z, explosionPower, ExplosionSourceType.NONE);

            // Spawn lightning at impact location
            LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
            lightningEntity.setPos(hitPos.x, hitPos.y, hitPos.z);
            world.spawnEntity(lightningEntity);

            this.discard(); // Remove fireball after explosion
        }
    }

}
