package mattonfire.dnd.classes;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;

public class CustomFireballEntity extends FireballEntity {
    private int explosionPower = 4; // Adjust explosion power (default is often 1 or 2)

    public CustomFireballEntity(EntityType<? extends FireballEntity> entityType, World world) {
        super(entityType, world);
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
