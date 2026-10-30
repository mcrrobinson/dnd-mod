package mattonfire.dnd.entity;

import mattonfire.dnd.entity.kobold.Kobolds;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * A cobblestone shot from a sling: {@link #DAMAGE} (the Halfling Sling) or what the sling sets (a kobold's
 * {@value #KOBOLD_DAMAGE}) to what it hits, then it breaks into dust. A kobold's stone flies through other kobolds.
 */
public class SlingStoneEntity extends ThrownItemEntity {
    public static final float DAMAGE = 4.0F;
    /** The Kobold Sling's and a Kobold Slinger's stones. */
    public static final float KOBOLD_DAMAGE = 3.0F;

    private float damage = DAMAGE;

    public SlingStoneEntity(EntityType<? extends SlingStoneEntity> type, World world) {
        super(type, world);
    }

    public SlingStoneEntity(World world, LivingEntity owner) {
        super(ModEntityTypes.SLING_STONE, owner, world);
    }

    public SlingStoneEntity(World world, LivingEntity owner, float damage) {
        this(world, owner);
        this.damage = damage;
    }

    public float getDamage() {
        return this.damage;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected boolean canHit(Entity entity) {
        // Kobolds never hurt each other: their stones pass through the pack.
        if (this.getOwner() != null && Kobolds.isKobold(this.getOwner()) && Kobolds.isKobold(entity)) {
            return false;
        }
        return super.canHit(entity);
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        hit.getEntity().damage(this.getDamageSources().thrown(this, this.getOwner()), this.damage);
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

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat("Damage", this.damage);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Damage")) {
            this.damage = nbt.getFloat("Damage");
        }
    }
}
