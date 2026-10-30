package mattonfire.dnd.entity.kobold;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A Kobold Dragonshield, a pack's leader: 30 health, 7 armour, a spear (5), knockback resistance 0.3, drawn at
 * 1.15x (0.6 x 1.1). It's always brave, and while it lives kobolds within {@value KoboldCourage#RALLY_RANGE}
 * blocks are too (its <b>Rally</b>, {@link KoboldCourage}) and have Speed I. Its shield stops half the arrows that
 * come at it from the front, and the dragon's blessing makes it immune to fire (permanent Fire Resistance).
 */
public class KoboldDragonshieldEntity extends KoboldEntity {
    /** Chance the shield stops a projectile from in front. */
    public static final float SHIELD_BLOCK_CHANCE = 0.5F;
    private static final int RALLY_INTERVAL = 20;

    public KoboldDragonshieldEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return koboldAttributes(30.0D, 7.0D, 0.30D, 5.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3D);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!this.world.isClient && source.isIn(DamageTypeTags.IS_PROJECTILE) && source.getSource() != null
                && this.inFront(source.getSource().getPos()) && this.random.nextFloat() < SHIELD_BLOCK_CHANCE) {
            this.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
            DnDClasses.LOGGER.info("[Kobold] {} blocks a {} with its shield", this.getName().getString(),
                    source.getSource().getType().getUntranslatedName());
            return false;
        }
        return super.damage(source, amount);
    }

    private boolean inFront(Vec3d from) {
        Vec3d facing = this.getRotationVec(1.0F).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3d towards = from.subtract(this.getPos()).multiply(1.0D, 0.0D, 1.0D).normalize();
        return facing.dotProduct(towards) > 0.0D;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.world.isClient || this.age % RALLY_INTERVAL != 0) {
            return;
        }
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 400, 0, true, false));
        for (KoboldEntity kobold : this.world.getEntitiesByClass(KoboldEntity.class,
                this.getBoundingBox().expand(KoboldCourage.RALLY_RANGE),
                k -> k != this && k.isAlive() && k.squaredDistanceTo(this) <= KoboldCourage.RALLY_RANGE * KoboldCourage.RALLY_RANGE)) {
            kobold.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, true, false));
        }
    }
}
