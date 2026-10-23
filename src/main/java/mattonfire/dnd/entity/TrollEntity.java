package mattonfire.dnd.entity;

import java.util.EnumSet;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.ai.goal.TrollComboGoal;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
 * Troll: a big, hunched brute of the swamps, taigas and dark forests that regenerates every second
 * and won't stay down.
 * <ul>
 * <li><b>Rend</b> ({@link TrollComboGoal}): a left claw, then a right claw {@link #COMBO_GAP} ticks
 * later; if both land the second one rends for {@link #REND_BONUS} more and throws the target back.</li>
 * <li><b>Regeneration</b>: {@link #REGEN_AMOUNT} HP every {@link #REGEN_INTERVAL} ticks, unless it took
 * {@link #TROLL_BANE} (fire or acid) damage in the last {@link #BANE_TICKS} ticks or is burning.</li>
 * <li><b>Felled</b>: a blow that would kill it, unless it's fire or acid (or it's burning, or the
 * damage bypasses death saves, like {@code /kill}), drops it to 1 HP instead. It lies there ignoring
 * everything but fire and acid, and gets up {@link #FELLED_TICKS} ticks later with
 * {@link #RISE_HEALTH} HP. Fire, acid or flint and steel while it's down kill it for good, and only
 * that true death ({@link #isBurned()}) drops loot and XP.</li>
 * </ul>
 */
public class TrollEntity extends HostileEntity implements GeoEntity {
    private static final TrackedData<Boolean> FELLED = DataTracker.registerData(TrollEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);

    /** Fire and acid: stops the regeneration, and finishes off a felled troll. */
    public static final TagKey<DamageType> TROLL_BANE = TagKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "troll_bane"));
    /** Damage that ends a creature outright, with no Downed state or felling (void, {@code /kill}). */
    public static final TagKey<DamageType> BYPASSES_DEATH_SAVES = TagKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "bypasses_death_saves"));

    public static final float REGEN_AMOUNT = 3.0F;
    public static final int REGEN_INTERVAL = 20;
    public static final int BANE_TICKS = 100;
    public static final int FELLED_TICKS = 200;
    public static final float RISE_HEALTH = 20.0F;
    /** How long a player's hit still earns them the kill (loot, XP) when someone else burns it. */
    public static final int CREDIT_TICKS = 600;

    // Rend
    public static final double COMBO_REACH = 2.8D;
    public static final double COMBO_FOLLOW_REACH = 3.2D;
    public static final int COMBO_GAP = 8;
    public static final int COMBO_COOLDOWN = 30;
    public static final float REND_BONUS = 4.0F;
    public static final double REND_KNOCKBACK = 0.8D;

    public static final String CLAW_LEFT = "claw_left";
    public static final String CLAW_RIGHT = "claw_right";
    public static final String REND = "rend";
    public static final String RISE = "rise";
    public static final String ROAR = "roar";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation FELLED_LOOP = RawAnimation.begin().thenLoop("felled");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** World time of the last fire or acid damage, or {@code Long.MIN_VALUE / 2} if never. */
    private long lastBaneTime = Long.MIN_VALUE / 2;
    private int felledTicks;
    private boolean burned;

    // Combo state: the right claw follows COMBO_GAP ticks after the left.
    @Nullable
    private LivingEntity comboTarget;
    private int comboTicks;
    private boolean firstClawLanded;
    private long nextComboTime;

    // Who gets the credit when it finally burns.
    @Nullable
    private UUID lastPlayerHitter;
    private long lastPlayerHitTime;

    public TrollEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 25;
        this.setStepHeight(1.0F);
    }

    public static DefaultAttributeContainer.Builder createTrollAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 70.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FELLED, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LieStillGoal());
        this.goalSelector.add(2, new TrollComboGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    // ---- State ----

    public boolean isFelled() {
        return this.dataTracker.get(FELLED);
    }

    /** True once it has died for good to fire or acid: the only death that drops loot and XP. */
    public boolean isBurned() {
        return this.burned;
    }

    public static boolean isBane(DamageSource source) {
        return source.isIn(TROLL_BANE);
    }

    /** Whether the regeneration is held back: burning, or fire/acid damage in the last {@link #BANE_TICKS}. */
    public boolean isRegenSuppressed() {
        return this.isOnFire() || this.world.getTime() - this.lastBaneTime < BANE_TICKS;
    }

    /** Mobs (iron golems, a Bodyguard retarget) leave a felled troll alone. */
    @Override
    public boolean canTakeDamage() {
        return !this.isFelled() && super.canTakeDamage();
    }

    @Override
    public boolean cannotDespawn() {
        return this.isFelled() || super.cannotDespawn();
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return !this.isFelled() && super.canImmediatelyDespawn(distanceSquared);
    }

    // ---- Damage, felling and burning ----

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.world.isClient || this.isInvulnerableTo(source)) {
            return super.damage(source, amount);
        }
        boolean bane = isBane(source);
        if (bane) {
            this.lastBaneTime = this.world.getTime();
        }
        if (source.getAttacker() instanceof PlayerEntity player) {
            this.lastPlayerHitter = player.getUuid();
            this.lastPlayerHitTime = this.world.getTime();
        }
        if (this.isFelled()) {
            if (bane) {
                return this.burnAway(source, source.getAttacker() instanceof PlayerEntity p ? p : null);
            }
            if (source.isIn(BYPASSES_DEATH_SAVES)) {
                this.timeUntilRegen = 0;
                return super.damage(source, Math.max(amount, this.getHealth() + 1.0F));
            }
            // Ignores everything else while it's down.
            return false;
        }
        return super.damage(source, amount);
    }

    /**
     * Where a lethal blow turns into a fall instead: after armour, so it's the damage it really takes. A fire
     * or acid blow, or one while it's burning, kills it for good.
     */
    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        if (this.getHealth() > 0.0F || this.world.isClient || this.isFelled() || this.burned) {
            return;
        }
        if (isBane(source) || this.isOnFire()) {
            this.markBurned(source.getAttacker() instanceof PlayerEntity p ? p : null);
        } else if (!source.isIn(BYPASSES_DEATH_SAVES)) {
            this.fell(source);
        }
    }

    private void fell(DamageSource source) {
        this.setHealth(1.0F);
        this.dataTracker.set(FELLED, true);
        this.felledTicks = FELLED_TICKS;
        this.endCombo();
        this.setTarget(null);
        this.getNavigation().stop();
        Entity attacker = source.getAttacker();
        DnDClasses.LOGGER.info("[Troll] felled by {}", attacker != null ? attacker.getName().getString() : source.getName());
        MobActions.log(this, "felled", attacker instanceof LivingEntity living ? living : null);
        this.playSound(SoundEvents.ENTITY_RAVAGER_STUNNED, 1.2F, 0.6F);
    }

    private void rise() {
        this.dataTracker.set(FELLED, false);
        this.felledTicks = 0;
        this.setHealth(Math.max(this.getHealth(), RISE_HEALTH));
        DnDClasses.LOGGER.info("[Troll] rises ({} HP)", Math.round(this.getHealth()));
        MobActions.log(this, "rise", null);
        this.triggerAnim("attack_controller", RISE);
        this.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.5F, 0.7F);
    }

    /** Fire or acid on a felled troll: it dies for good, with its loot, credited to {@code burner} if known. */
    private boolean burnAway(DamageSource source, @Nullable PlayerEntity burner) {
        this.markBurned(burner);
        this.timeUntilRegen = 0;
        this.dataTracker.set(FELLED, false);
        // It's at 1 HP and bane damage isn't felled again, so this kills it.
        return super.damage(source, Math.max(this.getHealth() + 1.0F, 100.0F));
    }

    private void markBurned(@Nullable PlayerEntity burner) {
        this.burned = true;
        PlayerEntity credit = burner;
        if (credit == null && this.lastPlayerHitter != null
                && this.world.getTime() - this.lastPlayerHitTime <= CREDIT_TICKS) {
            credit = this.world.getPlayerByUuid(this.lastPlayerHitter);
        }
        if (credit != null) {
            // Loot that needs a player kill (and the XP) checks this, which a plain hit only keeps for 5 s.
            this.setAttacking(credit);
        }
        DnDClasses.LOGGER.info("[Troll] burned by {}", credit != null ? credit.getName().getString() : "fire");
        MobActions.log(this, "burned", credit);
    }

    /** Flint and steel or a fire charge on a felled troll sets it alight, which finishes it. */
    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (this.isFelled() && (stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE))) {
            if (!this.world.isClient) {
                if (stack.isOf(Items.FLINT_AND_STEEL)) {
                    this.playSound(SoundEvents.ITEM_FLINTANDSTEEL_USE, 1.0F, 1.0F);
                    stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                } else {
                    this.playSound(SoundEvents.ITEM_FIRECHARGE_USE, 1.0F, 1.0F);
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                }
                this.setOnFireFor(8);
                this.lastPlayerHitter = player.getUuid();
                this.lastPlayerHitTime = this.world.getTime();
                this.burnAway(this.getDamageSources().inFire(), player);
            }
            return ActionResult.success(this.world.isClient);
        }
        return super.interactMob(player, hand);
    }

    @Override
    public boolean shouldDropXp() {
        return this.burned && super.shouldDropXp();
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        if (this.burned) {
            super.dropLoot(source, causedByPlayer);
        }
    }

    // ---- Rend ----

    public boolean canStartCombo(LivingEntity target) {
        return !this.isFelled() && this.comboTarget == null && this.world.getTime() >= this.nextComboTime
                && this.squaredDistanceTo(target) <= COMBO_REACH * COMBO_REACH;
    }

    /** The left claw, now; the right one follows in {@link #COMBO_GAP} ticks. */
    public void startCombo(LivingEntity target) {
        this.comboTarget = target;
        this.comboTicks = COMBO_GAP;
        this.triggerAnim("attack_controller", CLAW_LEFT);
        this.swingHand(Hand.MAIN_HAND);
        this.firstClawLanded = this.claw(target, 0.0F, 0.0D);
    }

    public boolean isInCombo() {
        return this.comboTarget != null;
    }

    private void tickCombo() {
        LivingEntity target = this.comboTarget;
        if (target == null) {
            return;
        }
        if (!target.isAlive() || target.world != this.world) {
            this.endCombo();
            return;
        }
        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        if (--this.comboTicks > 0) {
            return;
        }
        if (this.squaredDistanceTo(target) <= COMBO_FOLLOW_REACH * COMBO_FOLLOW_REACH) {
            boolean rend = this.firstClawLanded;
            this.triggerAnim("attack_controller", rend ? REND : CLAW_RIGHT);
            this.swingHand(Hand.OFF_HAND);
            // The right claw comes inside the left's hit cooldown; let it through.
            target.timeUntilRegen = 0;
            boolean hit = this.claw(target, rend ? REND_BONUS : 0.0F, rend ? REND_KNOCKBACK : 0.0D);
            if (hit && rend) {
                MobActions.log(this, "rend", target);
                this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 0.6F);
                ((ServerWorld) this.world).spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(),
                        target.getBodyY(0.5D), target.getZ(), 4, 0.3D, 0.3D, 0.3D, 0.1D);
            }
        }
        this.endCombo();
    }

    private void endCombo() {
        if (this.comboTarget != null) {
            this.nextComboTime = this.world.getTime() + COMBO_COOLDOWN;
        }
        this.comboTarget = null;
        this.comboTicks = 0;
        this.firstClawLanded = false;
    }

    /** One claw: the attack damage plus {@code bonus}, and a shove of {@code knockback} away from the troll. */
    private boolean claw(LivingEntity target, float bonus, double knockback) {
        float damage = (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) + bonus;
        boolean hit = target.damage(this.getDamageSources().mobAttack(this), damage);
        if (hit) {
            this.applyDamageEffects(this, target);
            this.onAttacking(target);
            if (knockback > 0.0D) {
                Vec3d away = target.getPos().subtract(this.getPos()).multiply(1, 0, 1);
                if (away.lengthSquared() > 1.0E-4) {
                    away = away.normalize();
                    target.takeKnockback(knockback, -away.x, -away.z);
                    target.velocityModified = true;
                }
            }
        }
        return hit;
    }

    @Override
    public boolean tryAttack(Entity target) {
        // Its claws only strike through the combo.
        return !this.isFelled() && target instanceof LivingEntity living && this.claw(living, 0.0F, 0.0D);
    }

    // ---- Ticking ----

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!this.isFelled()) {
            this.tickCombo();
        }
    }

    /**
     * Regeneration and the felled timer run in {@code tick}, not {@code mobTick}, so they also work on a
     * {@code NoAI} troll (frozen for tests, or chained up as a champion).
     */
    @Override
    public void tick() {
        super.tick();
        if (this.world.isClient || !this.isAlive()) {
            return;
        }
        ServerWorld server = (ServerWorld) this.world;
        if (this.isFelled()) {
            if (this.isOnFire()) {
                // Caught fire while down (lava, a fire block, a flaming arrow's burn): that's the end of it.
                this.burnAway(this.getDamageSources().onFire(), null);
                return;
            }
            this.setTarget(null);
            if (--this.felledTicks <= 0) {
                this.rise();
            } else if (this.age % 10 == 0) {
                server.spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.4D, this.getZ(),
                        2, 0.6D, 0.2D, 0.6D, 0.0D);
            }
            return;
        }
        if (this.age % REGEN_INTERVAL == 0) {
            if (this.isRegenSuppressed()) {
                server.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getBodyY(0.6D), this.getZ(),
                        4, 0.4D, 0.5D, 0.4D, 0.01D);
            } else if (this.getHealth() < this.getMaxHealth()) {
                this.heal(REGEN_AMOUNT);
                server.spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getBodyY(0.6D), this.getZ(),
                        4, 0.5D, 0.6D, 0.5D, 0.0D);
            }
        }
    }

    /** Keeps a felled troll on the ground: no moving, jumping or looking about. */
    private class LieStillGoal extends Goal {
        LieStillGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return TrollEntity.this.isFelled();
        }

        @Override
        public void start() {
            TrollEntity.this.getNavigation().stop();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            TrollEntity.this.getNavigation().stop();
            TrollEntity.this.setVelocity(TrollEntity.this.getVelocity().multiply(0.0D, 1.0D, 0.0D));
        }
    }

    // ---- Sounds (placeholders, pitched down) ----

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isFelled() ? null : SoundEvents.ENTITY_RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_RAVAGER_STEP, 0.3F, 0.8F);
    }

    @Override
    public float getSoundPitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 0.7F;
    }

    // ---- Animation ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::predicate));
        AnimationController<TrollEntity> attack = new AnimationController<>(this, "attack_controller", 0,
                state -> PlayState.STOP);
        for (String name : new String[] {CLAW_LEFT, CLAW_RIGHT, REND, RISE, ROAR}) {
            attack.triggerableAnim(name, RawAnimation.begin().thenPlay(name));
        }
        controllers.add(attack);
    }

    private PlayState predicate(AnimationState<TrollEntity> state) {
        if (this.isFelled()) {
            return state.setAndContinue(FELLED_LOOP);
        }
        if (state.isMoving()) {
            return state.setAndContinue(this.getTarget() != null || this.isAttacking() ? RUN : WALK);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ---- Saving ----

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Felled", this.isFelled());
        nbt.putInt("FelledTicks", this.felledTicks);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(FELLED, nbt.getBoolean("Felled"));
        this.felledTicks = nbt.getInt("FelledTicks");
    }
}
