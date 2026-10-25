package mattonfire.dnd.entity.drow;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/**
 * A drow hand-crossbow bolt: {@value DrowEntity#BOLT_DAMAGE} damage and Drow Poison (a CON save,
 * {@link DrowPoison#afflict}) on whatever it hits. It flies through other drow and spiders, and can't be picked up.
 */
public class DrowBoltEntity extends PersistentProjectileEntity {
    public DrowBoltEntity(EntityType<? extends DrowBoltEntity> type, World world) {
        super(type, world);
    }

    public DrowBoltEntity(World world, LivingEntity owner) {
        super(Drow.DROW_BOLT, owner, world);
        this.pickupType = PickupPermission.DISALLOWED;
        this.setShotFromCrossbow(true);
        this.setSound(SoundEvents.ITEM_CROSSBOW_HIT);
    }

    @Override
    protected ItemStack asItemStack() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected boolean canHit(Entity entity) {
        return super.canHit(entity) && !DrowAllies.isAlly(entity);
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        Entity entity = hit.getEntity();
        Entity owner = this.getOwner();
        boolean hurt = entity.damage(this.getDamageSources().arrow(this, owner), DrowEntity.BOLT_DAMAGE);
        if (hurt && !this.world.isClient && entity instanceof LivingEntity living) {
            this.playSound(SoundEvents.ITEM_CROSSBOW_HIT, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            DrowPoison.afflict(living, owner instanceof LivingEntity source ? source : null,
                    owner instanceof DrowEntity drow ? DrowSaves.poisonOf(drow) : DrowSaves.DROW_POISON);
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.inGround && this.inGroundTime > 100) {
            this.discard();
        }
    }
}
