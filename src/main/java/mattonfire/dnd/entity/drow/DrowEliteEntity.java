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
 * <p>
 * When the Matron Mother calls the elites ({@link DrowMatronEntity}, below a quarter of her health) it
 * {@link #answerCall answers}: Strength I for {@value #CALL_STRENGTH_TICKS} ticks, a longer follow range, and it runs to
 * her Sanctum ({@link AnswerCallGoal}) before it fights anything else, then stays near it.
 */
public class DrowEliteEntity extends DrowEntity {
    public static final float PARRY_CHANCE = 0.25F;
    public static final int LEVITATE_COOLDOWN = 200;
    /** How long the Matron's call gives Strength I. */
    public static final int CALL_STRENGTH_TICKS = 2400;
    /** Within this many blocks of the Sanctum a called elite has arrived and fights as usual. */
    public static final double CALL_ARRIVED = 5.0D;
    /** Extra follow range while called, so it can path to the Sanctum from anywhere in the Spire. */
    public static final double CALL_FOLLOW_BONUS = 32.0D;
    private static final java.util.UUID CALL_FOLLOW_ID = java.util.UUID.fromString("7c1e5b0a-3f2d-4a8e-9b61-0d4c2e8f1a35");
    private int levitateCooldown;
    /** Where the Matron Mother called it, or null. */
    @org.jetbrains.annotations.Nullable
    private net.minecraft.util.math.BlockPos calledTo;

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
        this.goalSelector.add(1, new AnswerCallGoal(this));
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

    // ---- Call the Elites ----

    /** Where it was called to, or null. */
    @org.jetbrains.annotations.Nullable
    public net.minecraft.util.math.BlockPos calledTo() {
        return this.calledTo;
    }

    /**
     * The Matron Mother calls it to {@code sanctum}: Strength I, a longer follow range, and it heads there now, then
     * takes on {@code target} (her foe) when it arrives.
     */
    public void answerCall(net.minecraft.util.math.BlockPos sanctum, @org.jetbrains.annotations.Nullable LivingEntity target) {
        this.calledTo = sanctum.toImmutable();
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, CALL_STRENGTH_TICKS, 0));
        var follow = this.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
        if (follow != null && follow.getModifier(CALL_FOLLOW_ID) == null) {
            follow.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(CALL_FOLLOW_ID,
                    "Matron's call", CALL_FOLLOW_BONUS, net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADDITION));
        }
        this.setPositionTarget(this.calledTo, 8);
        if (target != null && target.isAlive()) {
            this.setTarget(target);
        }
        this.getNavigation().startMovingTo(sanctum.getX() + 0.5D, sanctum.getY(), sanctum.getZ() + 0.5D, 1.3D);
        MobActions.log(this, "answer the call", target);
    }

    /** Whether it's still on its way to where it was called. */
    boolean enRoute() {
        return this.calledTo != null && !this.calledTo.isWithinDistance(this.getPos(), CALL_ARRIVED);
    }

    /** A called elite runs to the Sanctum before anything else (the other goals take over once it's there). */
    static final class AnswerCallGoal extends net.minecraft.entity.ai.goal.Goal {
        private final DrowEliteEntity elite;
        private int repath;

        AnswerCallGoal(DrowEliteEntity elite) {
            this.elite = elite;
            this.setControls(java.util.EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return this.elite.enRoute();
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void tick() {
            if (--this.repath <= 0 || this.elite.getNavigation().isIdle()) {
                this.repath = 20;
                net.minecraft.util.math.BlockPos to = this.elite.calledTo;
                if (to != null) {
                    this.elite.getNavigation().startMovingTo(to.getX() + 0.5D, to.getY(), to.getZ() + 0.5D, 1.3D);
                }
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.calledTo != null) {
            nbt.put("CalledTo", net.minecraft.nbt.NbtHelper.fromBlockPos(this.calledTo));
        }
    }

    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("CalledTo")) {
            this.calledTo = net.minecraft.nbt.NbtHelper.toBlockPos(nbt.getCompound("CalledTo"));
            this.setPositionTarget(this.calledTo, 8);
        }
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
