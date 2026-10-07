package mattonfire.dnd.entity;

import java.util.EnumSet;
import mattonfire.dnd.entity.ai.goal.OwlbearChargeGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Owlbear: a hostile bear with an owl's head that prowls dark and old-growth forests. Besides its
 * claw swipes it has two big moves:
 * <ul>
 * <li><b>Charge</b> ({@link OwlbearChargeGoal}): from a few blocks off it roars, then barrels
 * straight at where its target stood, bowling over whatever it hits. Miss and run into a wall and
 * it's stunned for a couple of seconds.</li>
 * <li><b>Bear hug</b>: a claw hit sometimes turns into a hug. It rears up, pins the target in front
 * of it and crushes it every half second until it lets go, or until the target hits it hard
 * enough to break free.</li>
 * </ul>
 * Druids that kill one can take its form (it's in {@code #dndclasses:druid_forms}).
 */
public class OwlbearEntity extends HostileEntity implements GeoEntity {
    private static final TrackedData<Boolean> CHARGING = DataTracker.registerData(OwlbearEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HUGGING = DataTracker.registerData(OwlbearEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation HUG = RawAnimation.begin().thenLoop("hug");
    private static final String[] SWIPES = {"swipe", "swipe_left"};
    public static final String ROAR = "roar";

    // Bear hug
    private static final float HUG_CHANCE = 0.35F;
    private static final int HUG_TICKS = 60;
    private static final int HUG_CRUSH_INTERVAL = 10;
    private static final float HUG_CRUSH_DAMAGE = 3.0F;
    /** Damage the hugged target has to deal back to break free early. */
    private static final float HUG_BREAK_DAMAGE = 6.0F;
    private static final int HUG_COOLDOWN = 160;
    /** Targets wider than this (other big mobs) can't be hugged. */
    private static final float HUG_MAX_WIDTH = 1.5F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private LivingEntity hugged;
    private int hugTicks;
    private float hugDamageTaken;
    private long nextHugTime;
    private int stunTicks;

    public OwlbearEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 15;
        // Lets the charge carry it over roots and single blocks.
        this.setStepHeight(1.0F);
    }

    public static DefaultAttributeContainer.Builder createOwlbearAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.5D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CHARGING, false);
        this.dataTracker.startTracking(HUGGING, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new HoldStillGoal());
        this.goalSelector.add(2, new OwlbearChargeGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
        // Hungry: it goes after livestock and deer-sized game too, but only now and then.
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, AnimalEntity.class, 40, true, false,
                e -> !(e instanceof OwlbearEntity) && e.getWidth() <= HUG_MAX_WIDTH));
    }

    // ---- State ----

    public boolean isCharging() {
        return this.dataTracker.get(CHARGING);
    }

    public void setCharging(boolean charging) {
        this.dataTracker.set(CHARGING, charging);
    }

    public boolean isHugging() {
        return this.dataTracker.get(HUGGING);
    }

    public boolean isStunned() {
        return this.stunTicks > 0;
    }

    /** Dazed after charging head first into a wall: it can't move or attack for a while. */
    public void stun(int ticks) {
        this.stunTicks = ticks;
        this.getNavigation().stop();
        this.playSound(SoundEvents.ENTITY_RAVAGER_STUNNED, 1.0F, 0.9F);
    }

    // ---- Attacks ----

    @Override
    public boolean tryAttack(Entity target) {
        if (this.isStunned() || this.isHugging()) {
            return false;
        }
        boolean hit = super.tryAttack(target);
        if (hit) {
            this.triggerAnim("attack_controller", SWIPES[this.random.nextInt(SWIPES.length)]);
            if (target instanceof LivingEntity living && living.isAlive() && this.canHug(living)
                    && this.random.nextFloat() < HUG_CHANCE) {
                this.startHug(living);
            }
        }
        return hit;
    }

    private boolean canHug(LivingEntity target) {
        return this.world.getTime() >= this.nextHugTime && target.getWidth() <= HUG_MAX_WIDTH
                && !target.hasVehicle() && !(target instanceof PlayerEntity player && player.getAbilities().invulnerable);
    }

    private void startHug(LivingEntity target) {
        this.hugged = target;
        this.hugTicks = HUG_TICKS;
        this.hugDamageTaken = 0.0F;
        this.dataTracker.set(HUGGING, true);
        this.getNavigation().stop();
        this.playSound(SoundEvents.ENTITY_POLAR_BEAR_WARNING, 1.5F, 0.8F);
    }

    private void endHug() {
        LivingEntity target = this.hugged;
        this.hugged = null;
        this.hugTicks = 0;
        this.dataTracker.set(HUGGING, false);
        this.nextHugTime = this.world.getTime() + HUG_COOLDOWN;
        if (target != null && target.isAlive()) {
            // Tossed aside.
            Vec3d away = target.getPos().subtract(this.getPos()).multiply(1, 0, 1);
            if (away.lengthSquared() > 1.0E-4) {
                away = away.normalize();
                target.takeKnockback(0.6D, -away.x, -away.z);
            }
        }
    }

    /** Pins the hugged target just in front of the owlbear and crushes it every so often. */
    private void tickHug() {
        LivingEntity target = this.hugged;
        if (target == null || !target.isAlive() || target.isRemoved() || target.world != this.world
                || this.squaredDistanceTo(target) > 16.0D || --this.hugTicks <= 0
                || (target instanceof PlayerEntity player && (player.isSpectator() || player.getAbilities().invulnerable))) {
            this.endHug();
            return;
        }

        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        float yaw = this.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        // The model reaches further forward than the hitbox: hold the target at the end of its arms.
        double reach = this.getWidth() / 2.0D + target.getWidth() / 2.0D + 0.6D;
        Vec3d hold = new Vec3d(this.getX() - MathHelper.sin(yaw) * reach, this.getY() + 0.3D,
                this.getZ() + MathHelper.cos(yaw) * reach);
        Vec3d pull = hold.subtract(target.getPos());
        target.setVelocity(pull.x * 0.5D, Math.min(target.getVelocity().y, 0.0D) + pull.y * 0.3D, pull.z * 0.5D);
        target.velocityModified = true;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 6, false, false));

        if (this.hugTicks % HUG_CRUSH_INTERVAL == 0) {
            target.timeUntilRegen = 0;
            if (target.damage(this.getDamageSources().mobAttack(this), HUG_CRUSH_DAMAGE)) {
                this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 0.6F);
                ((ServerWorld) this.world).spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(),
                        target.getBodyY(0.5D), target.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.1D);
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt && this.hugged != null && source.getAttacker() == this.hugged) {
            this.hugDamageTaken += amount;
            if (this.hugDamageTaken >= HUG_BREAK_DAMAGE) {
                // Fought its way out.
                this.endHug();
            }
        }
        return hurt;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.hugged != null) {
            this.tickHug();
        }
        if (this.stunTicks > 0) {
            this.stunTicks--;
            if (this.age % 5 == 0) {
                ((ServerWorld) this.world).spawnParticles(ParticleTypes.CRIT, this.getX(), this.getEyeY() + 0.4D,
                        this.getZ(), 2, 0.4D, 0.1D, 0.4D, 0.0D);
            }
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        if (this.hugged != null) {
            this.endHug();
        }
        super.onDeath(source);
    }

    /** Keeps the owlbear rooted while it's hugging something or stunned. */
    private class HoldStillGoal extends Goal {
        HoldStillGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return OwlbearEntity.this.isHugging() || OwlbearEntity.this.isStunned();
        }

        @Override
        public void start() {
            OwlbearEntity.this.getNavigation().stop();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            OwlbearEntity.this.getNavigation().stop();
            OwlbearEntity.this.setVelocity(OwlbearEntity.this.getVelocity().multiply(0.0D, 1.0D, 0.0D));
        }
    }

    // ---- Sounds ----

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_POLAR_BEAR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_POLAR_BEAR_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_POLAR_BEAR_STEP, 0.15F, 1.0F);
    }

    @Override
    public float getSoundPitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 0.75F;
    }

    // ---- Animation ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::predicate));
        AnimationController<OwlbearEntity> attack = new AnimationController<>(this, "attack_controller", 0,
                state -> PlayState.STOP);
        for (String swipe : SWIPES) {
            attack.triggerableAnim(swipe, RawAnimation.begin().thenPlay(swipe));
        }
        attack.triggerableAnim(ROAR, RawAnimation.begin().thenPlay(ROAR));
        controllers.add(attack);
    }

    private PlayState predicate(AnimationState<OwlbearEntity> state) {
        if (this.isHugging()) {
            return state.setAndContinue(HUG);
        }
        if (this.isCharging()) {
            return state.setAndContinue(RUN);
        }
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ---- Saving ----

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("StunTicks", this.stunTicks);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.stunTicks = nbt.getInt("StunTicks");
    }
}
