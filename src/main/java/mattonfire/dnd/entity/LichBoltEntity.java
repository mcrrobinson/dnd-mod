package mattonfire.dnd.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * A wither skull whose blast hurts but never breaks blocks (the Lich lives in libraries). It has
 * no entity type of its own, so it isn't saved: it would come back as a vanilla wither skull.
 */
final class LichBoltEntity extends WitherSkullEntity {
    LichBoltEntity(World world, LivingEntity owner, double dx, double dy, double dz) {
        super(world, owner, dx, dy, dz);
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult) hitResult);
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            this.onBlockHit((BlockHitResult) hitResult);
        }
        if (!this.world.isClient) {
            this.world.createExplosion(this, this.getX(), this.getY(), this.getZ(), 1.0F, false,
                    World.ExplosionSourceType.NONE);
            this.discard();
        }
    }
}
