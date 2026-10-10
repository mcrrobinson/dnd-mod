package mattonfire.dnd.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** A cobblestone shot from the Halfling Sling: 4 damage to what it hits, then it breaks into dust. */
public class SlingStoneEntity extends ThrownItemEntity {
    public static final float DAMAGE = 4.0F;

    public SlingStoneEntity(EntityType<? extends SlingStoneEntity> type, World world) {
        super(type, world);
    }

    public SlingStoneEntity(World world, LivingEntity owner) {
        super(ModEntityTypes.SLING_STONE, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        hit.getEntity().damage(this.getDamageSources().thrown(this, this.getOwner()), DAMAGE);
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (this.world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.COBBLESTONE.getDefaultState()),
                    this.getX(), this.getY(), this.getZ(), 8, 0.1D, 0.1D, 0.1D, 0.05D);
            this.discard();
        }
    }
}
