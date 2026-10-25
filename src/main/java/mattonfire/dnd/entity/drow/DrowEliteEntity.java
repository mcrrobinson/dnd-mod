package mattonfire.dnd.entity.drow;

import mattonfire.dnd.entity.MobActions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * A Drow Elite, a guard of the Drow Spire: 40 health, 8 armour (a chain shirt), twin scimitars (7) and the hand
 * crossbow. <b>Parry</b>: {@value #PARRY_CHANCE} of melee hits from the front glance off. It levitates up to 4 blocks
 * (Levitation for 1 s) to reach a target on a ledge, once every {@value #LEVITATE_COOLDOWN} ticks.
 */
public class DrowEliteEntity extends DrowEntity {
    public static final float PARRY_CHANCE = 0.25F;
    public static final int LEVITATE_COOLDOWN = 200;
    private int levitateCooldown;

    public DrowEliteEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 20;
    }

    public static DefaultAttributeContainer.Builder createDrowEliteAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void initCombatGoals() {
        this.goalSelector.add(2, new HandCrossbowGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.2D, true));
    }

    @Override
    protected boolean hasHandCrossbow() {
        return true;
    }

    @Override
    protected void equip(Random random) {
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        CrossbowItem.setCharged(crossbow, true);
        this.equipStack(EquipmentSlot.MAINHAND, crossbow);
        this.equipStack(EquipmentSlot.OFFHAND, blade(Items.IRON_SWORD));
        // The armour attribute is the stat; the chain shirt is for the look.
        ItemStack shirt = new ItemStack(Items.CHAINMAIL_CHESTPLATE);
        shirt.getOrCreateNbt().put("AttributeModifiers", new net.minecraft.nbt.NbtList());
        this.equipStack(EquipmentSlot.CHEST, shirt);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!this.world.isClient && this.parries(source)) {
            this.world.playSound(null, this.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1.0F, 1.4F);
            MobActions.log(this, "parry", source.getAttacker() instanceof LivingEntity living ? living : null);
            return false;
        }
        return super.damage(source, amount);
    }

    /** A melee hit (the attacker struck it directly) from the front, {@value #PARRY_CHANCE} of the time. */
    private boolean parries(DamageSource source) {
        Entity attacker = source.getAttacker();
        return attacker != null && source.getSource() == attacker && attacker instanceof LivingEntity
                && !DrowAllies.isAlly(attacker) && this.isInFront(source) && this.random.nextFloat() < PARRY_CHANCE;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.levitateCooldown > 0) {
            this.levitateCooldown--;
        }
        LivingEntity target = this.getTarget();
        if (target != null && this.levitateCooldown <= 0 && this.isOnGround()) {
            double up = target.getY() - this.getY();
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            if (up >= 2.0D && up <= 4.5D && dx * dx + dz * dz <= 36.0D && this.getNavigation().isIdle()) {
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 20, 3, false, false));
                this.levitateCooldown = LEVITATE_COOLDOWN;
                MobActions.log(this, "levitate", target);
            }
        }
    }
}
