package mattonfire.dnd.entity;

import java.util.EnumSet;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.ConditionEffects;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.SkillChecks.Grapple;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.entity.ai.Blink;
import mattonfire.dnd.entity.boss.Boss;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
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
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
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
 * Mind Flayer: an illithid psion that haunts the deepest caves (and, once it exists, the Underdark).
 * <ul>
 * <li><b>Mind Blast</b>: every {@value #BLAST_COOLDOWN} ticks, with its target in front within
 * {@value #BLAST_RANGE} blocks, a {@value #BLAST_WINDUP}-tick wind-up and then a {@value #BLAST_ANGLE} degree cone:
 * INT save ({@link MindFlayerSaves#MIND_BLAST}) or {@value #BLAST_DAMAGE} psychic damage and Stunned (half damage
 * on a success).</li>
 * <li><b>Tentacles</b>: an incapacitated creature within {@value #GRAB_RANGE} blocks is grabbed
 * ({@link Grapple#hold}, so Restrained too) and squeezed; after {@value #EXTRACT_TICKS} ticks of holding a
 * still-incapacitated victim it <b>extracts its brain</b> ({@value #EXTRACT_DAMAGE} damage through armour).</li>
 * <li><b>Blink</b> away when badly hurt or crowded, and <b>levitate</b> up to targets above it.</li>
 * <li><b>Thralls</b>: spawns with {@value #SPAWN_THRALLS} charmed mobs (from {@link #THRALLS}) and dominates more
 * now and then, up to {@value #MAX_THRALLS}. They go free {@code 100} ticks after it dies.</li>
 * </ul>
 * Specials run from {@link #mobTick}, which the Conditions mixin skips while it's incapacitated.
 */
public class MindFlayerEntity extends HostileEntity implements GeoEntity, Grapple.Holder {
    /** Entity types a Mind Flayer brings along as thralls. Data-driven so the Underdark can add drow and duergar. */
    public static final TagKey<EntityType<?>> THRALLS = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "mind_flayer_thralls"));

    // Mind Blast
    public static final int BLAST_COOLDOWN = 160;
    public static final int BLAST_WINDUP = 20;
    public static final double BLAST_RANGE = 12.0D;
    public static final float BLAST_ANGLE = 60.0F;
    public static final float BLAST_DAMAGE = 6.0F;
    /** Stunned length on a failed Mind Blast save, and the repeat save's interval. */
    public static final int STUN_TICKS = 80;
    public static final int STUN_SAVE_EVERY = 40;
    /** Ticks before the first blast of a fresh Mind Flayer. */
    private static final int FIRST_BLAST = 60;

    // Tentacles
    public static final double GRAB_RANGE = 2.5D;
    public static final int SQUEEZE_INTERVAL = 20;
    public static final float SQUEEZE_DAMAGE = 4.0F;
    public static final int EXTRACT_TICKS = 40;
    public static final float EXTRACT_DAMAGE = 16.0F;
    /** A victim that's no longer incapacitated is let go after this long. */
    public static final int RELEASE_AWAKE_TICKS = 40;
    /** Damage from anyone but the victim that makes it let go. */
    public static final float RESCUE_DAMAGE = 10.0F;
    private static final float GRAB_MAX_WIDTH = 1.5F;
    private static final int GRAB_COOLDOWN = 60;
    public static final float LASH_DAMAGE = 5.0F;

    // Blink and levitate
    public static final int BLINK_COOLDOWN = 160;
    public static final float BLINK_HEALTH = 0.3F;
    public static final double BLINK_CROWD = 2.0D;
    public static final double BLINK_MIN = 8.0D;
    public static final double BLINK_MAX = 14.0D;
    private static final int LEVITATE_TICKS = 20;

    // Thralls
    public static final int SPAWN_THRALLS = 2;
    public static final int MAX_THRALLS = 3;
    public static final int DOMINATE_INTERVAL = 600;
    public static final double DOMINATE_RANGE = 16.0D;
    private static final double THRALL_COUNT_RANGE = 32.0D;
    /**
     * Command tag on the thralls a structure places (the Underdark's Illithid Colony): a Mind Flayer within
     * {@value #COLONY_THRALL_RANGE} blocks charms any of them not already Charmed, checked once a second. The tag stays,
     * so another flayer of the colony takes them over if theirs dies.
     */
    public static final String COLONY_THRALL_TAG = "dndclasses.colony_thrall";
    public static final double COLONY_THRALL_RANGE = 24.0D;

    private static final byte STATE_NONE = 0;
    private static final byte STATE_BLAST = 1;
    private static final byte STATE_GRAB = 2;
    private static final TrackedData<Byte> STATE = DataTracker.registerData(MindFlayerEntity.class,
            TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Boolean> LEVITATING = DataTracker.registerData(MindFlayerEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation BLAST = RawAnimation.begin().thenPlayAndHold("mind_blast");
    private static final RawAnimation GRAB = RawAnimation.begin().thenLoop("grab");
    private static final RawAnimation LEVITATE = RawAnimation.begin().thenLoop("levitate");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Ticks until it may start a Mind Blast; {@code BlastCooldown} in NBT (0 fires at the next chance). */
    private int blastCooldown = FIRST_BLAST;
    private int blastWindup;
    @Nullable
    private LivingEntity held;
    private int holdTicks;
    private float rescueDamage;
    private long nextGrab;
    private long nextBlink;
    private long nextDominate;
    /** Thralls still to call up (set by {@link #initialize}, spawned on the first tick in the world). */
    private int pendingThralls;

    public MindFlayerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 40;
    }

    public static DefaultAttributeContainer.Builder createMindFlayerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, LASH_DAMAGE)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(STATE, STATE_NONE);
        this.dataTracker.startTracking(LEVITATING, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new HoldStillGoal());
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, MindFlayerEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
            @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        this.pendingThralls = SPAWN_THRALLS;
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    /** Monster spawn rule: deep underground (below y={@value #SPAWN_MAX_Y}), in the dark. */
    public static final int SPAWN_MAX_Y = -32;

    public static boolean canSpawn(EntityType<MindFlayerEntity> type, ServerWorldAccess world, SpawnReason reason,
            BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (!HostileEntity.canSpawnInDark(type, world, reason, pos, random)) {
            return false;
        }
        return reason != SpawnReason.NATURAL || (pos.getY() < SPAWN_MAX_Y && !world.isSkyVisible(pos));
    }

    // ---- State ----

    public boolean isBlasting() {
        return this.dataTracker.get(STATE) == STATE_BLAST;
    }

    public boolean isGrabbing() {
        return this.dataTracker.get(STATE) == STATE_GRAB;
    }

    @Nullable
    public LivingEntity getHeld() {
        return this.held;
    }

    private void setState(byte state) {
        this.dataTracker.set(STATE, state);
    }

    /** Whether {@code entity} is one of this Mind Flayer's charmed thralls. */
    public boolean isThrall(LivingEntity entity) {
        return ConditionEffects.hasFrom(entity, Condition.CHARMED, this.getUuid());
    }

    // ---- Tick ----

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isClient && this.held != null && !Conditions.canAct(this)) {
            // Stunned itself: the tentacles go slack. (mobTick doesn't run while it's incapacitated.)
            this.release("stunned");
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        long now = this.world.getTime();
        if (this.pendingThralls > 0) {
            this.spawnThralls((ServerWorld) this.world, this.pendingThralls);
            this.pendingThralls = 0;
        }
        if (this.age % 20 == 7) {
            this.claimColonyThralls();
        }
        if (this.held != null) {
            this.tickHold();
        } else if (this.blastWindup > 0) {
            this.tickWindup();
        } else {
            if (this.blastCooldown > 0) {
                this.blastCooldown--;
            }
            if (this.age % 5 == 0 && now >= this.nextGrab) {
                this.tryGrab();
            }
            if (this.held == null && this.blastCooldown <= 0) {
                this.tryStartBlast();
            }
        }
        if (now >= this.nextBlink && this.age % 5 == 0) {
            this.tryBlink(now);
        }
        if (this.age % 10 == 0) {
            this.tryLevitate();
        }
        this.dataTracker.set(LEVITATING, this.hasStatusEffect(StatusEffects.LEVITATION));
        if (now >= this.nextDominate && this.age > 40) {
            this.nextDominate = now + DOMINATE_INTERVAL;
            this.tryDominate();
        }
    }

    // ---- Mind Blast ----

    private void tryStartBlast() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.squaredDistanceTo(target) > BLAST_RANGE * BLAST_RANGE
                || !this.canSee(target)) {
            return;
        }
        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        if (!this.inCone(this.lookDirection(target), target)) {
            return; // turn to face it first
        }
        this.blastWindup = BLAST_WINDUP;
        this.setState(STATE_BLAST);
        this.getNavigation().stop();
        this.playSound(SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 1.5F, 1.6F);
        MobActions.log(this, "mind blast wind-up", target);
    }

    private void tickWindup() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.getLookControl().lookAt(target, 30.0F, 30.0F);
        }
        this.getNavigation().stop();
        if (this.age % 2 == 0) {
            ((ServerWorld) this.world).spawnParticles(ParticleTypes.WITCH, this.getX(), this.getEyeY(), this.getZ(),
                    4, 0.4D, 0.3D, 0.4D, 0.05D);
        }
        if (--this.blastWindup <= 0) {
            this.fireBlast(target);
        }
    }

    /** Direction of the cone: towards the target when there is one, otherwise where its head faces. */
    private Vec3d lookDirection(@Nullable LivingEntity target) {
        if (target != null && target.isAlive()) {
            Vec3d d = target.getEyePos().subtract(this.getEyePos());
            if (d.lengthSquared() > 1.0E-4) {
                // Only the part of the turn the head has already made: the flayer must face its target.
                Vec3d facing = Vec3d.fromPolar(0.0F, this.headYaw);
                Vec3d flat = new Vec3d(d.x, 0.0D, d.z).normalize();
                double cos = facing.dotProduct(flat);
                if (cos >= Math.cos(Math.toRadians(BLAST_ANGLE / 2.0F))) {
                    return d.normalize();
                }
                return facing;
            }
        }
        return Vec3d.fromPolar(this.getPitch(), this.headYaw);
    }

    private boolean inCone(Vec3d axis, LivingEntity entity) {
        Vec3d to = entity.getBoundingBox().getCenter().subtract(this.getEyePos());
        double distance = to.length();
        if (distance > BLAST_RANGE) {
            return false;
        }
        if (distance < 1.0D) {
            return true;
        }
        return axis.dotProduct(to.normalize()) >= Math.cos(Math.toRadians(BLAST_ANGLE / 2.0F));
    }

    private void fireBlast(@Nullable LivingEntity target) {
        this.blastCooldown = BLAST_COOLDOWN;
        this.setState(STATE_NONE);
        Vec3d axis = this.lookDirection(target);
        ServerWorld world = (ServerWorld) this.world;
        MobActions.log(this, "Mind Blast", target);
        this.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.2F, 1.6F);
        Vec3d eye = this.getEyePos();
        for (int i = 1; i <= 10; i++) {
            Vec3d p = eye.add(axis.multiply(i * 1.2D));
            world.spawnParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 3 + i, i * 0.12D, i * 0.12D, i * 0.12D, 0.0D);
        }
        List<LivingEntity> victims = world.getEntitiesByClass(LivingEntity.class,
                this.getBoundingBox().expand(BLAST_RANGE), e -> this.blastAffects(e) && this.inCone(axis, e)
                        && this.canSee(e));
        for (LivingEntity victim : victims) {
            this.blast(victim);
        }
    }

    /** Players, pets, villagers and the like: not other monsters, its thralls or other Mind Flayers. */
    private boolean blastAffects(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof MindFlayerEntity || this.isThrall(e)) {
            return false;
        }
        if (e instanceof PlayerEntity player) {
            return !player.isSpectator() && !player.getAbilities().invulnerable;
        }
        return !(e instanceof Monster) || e == this.getTarget();
    }

    private void blast(LivingEntity victim) {
        SaveResult save = SavingThrow.of(victim, MindFlayerSaves.MIND_BLAST)
                .source(this)
                .exposure(this, "blast", 20)
                .tags("magic", "psychic")
                .onSuccess(Text.translatable(MindFlayerSaves.MIND_BLAST.effectKey()))
                .onFailure(Text.translatable(MindFlayerSaves.MIND_BLAST.effectKey() + ".failure"))
                .roll();
        float damage = save.damage(BLAST_DAMAGE);
        DnDClasses.LOGGER.info("[MindFlayer] Mind Blast hits {}: {} ({} damage)", victim.getName().getString(),
                save.succeeded() ? "half damage" : "stunned", damage);
        if (damage > 0.0F) {
            victim.timeUntilRegen = 0;
            victim.damage(this.getDamageSources().indirectMagic(this, this), damage);
        }
        if (save.failed() && victim.isAlive()) {
            Conditions.of(victim, Condition.STUNNED)
                    .source(this)
                    .ticks(STUN_TICKS)
                    .repeatSave(MindFlayerSaves.MIND_BLAST, STUN_SAVE_EVERY)
                    .saveTags("magic", "psychic")
                    .apply();
        }
    }

    // ---- Tentacles ----

    private static boolean dazed(LivingEntity e) {
        return Conditions.has(e, Condition.INCAPACITATED) && !Conditions.has(e, Condition.PETRIFIED);
    }

    private void tryGrab() {
        LivingEntity target = this.getTarget();
        LivingEntity victim = null;
        if (target != null && this.canGrab(target)) {
            victim = target;
        } else {
            for (LivingEntity e : this.world.getEntitiesByClass(LivingEntity.class,
                    this.getBoundingBox().expand(GRAB_RANGE), this::canGrab)) {
                victim = e;
                break;
            }
        }
        if (victim != null) {
            this.startHold(victim);
        }
    }

    private boolean canGrab(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof MindFlayerEntity || this.isThrall(e) || !dazed(e)
                || e.getWidth() > GRAB_MAX_WIDTH || e.hasVehicle()) {
            return false;
        }
        if (e instanceof PlayerEntity player && (player.isSpectator() || player.getAbilities().invulnerable)) {
            return false;
        }
        return this.distanceTo(e) <= GRAB_RANGE + e.getWidth() / 2.0D;
    }

    private void startHold(LivingEntity victim) {
        this.held = victim;
        this.holdTicks = 0;
        this.rescueDamage = 0.0F;
        this.setState(STATE_GRAB);
        this.getNavigation().stop();
        MobActions.log(this, "tentacle grab", victim);
        DnDClasses.LOGGER.info("[Grapple] the mind flayer grabs {}", victim.getName().getString());
        Grapple.hold(victim, this, MindFlayerSaves.GRAPPLE_DC, "grapple.dndclasses.mind_flayer");
        this.playSound(SoundEvents.ENTITY_SQUID_SQUIRT, 1.2F, 0.6F);
    }

    private void release(String why) {
        LivingEntity victim = this.held;
        this.held = null;
        this.holdTicks = 0;
        this.setState(STATE_NONE);
        this.nextGrab = this.world.getTime() + GRAB_COOLDOWN;
        if (victim != null) {
            Grapple.release(victim, this);
            DnDClasses.LOGGER.info("[Grapple] the mind flayer lets go of {} ({})", victim.getName().getString(), why);
        }
    }

    private void tickHold() {
        LivingEntity victim = this.held;
        if (victim == null || !victim.isAlive() || victim.isRemoved() || victim.world != this.world
                || this.squaredDistanceTo(victim) > 16.0D
                || (victim instanceof PlayerEntity player && (player.isSpectator() || player.getAbilities().invulnerable))) {
            this.release("lost hold");
            return;
        }
        this.holdTicks++;
        this.getLookControl().lookAt(victim, 30.0F, 30.0F);
        float yaw = this.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        double reach = this.getWidth() / 2.0D + victim.getWidth() / 2.0D + 0.4D;
        Vec3d hold = new Vec3d(this.getX() - MathHelper.sin(yaw) * reach, this.getY(),
                this.getZ() + MathHelper.cos(yaw) * reach);
        Vec3d pull = hold.subtract(victim.getPos());
        victim.setVelocity(pull.x * 0.5D, Math.min(victim.getVelocity().y, 0.0D) + pull.y * 0.3D, pull.z * 0.5D);
        victim.velocityModified = true;

        boolean dazed = dazed(victim);
        if (this.holdTicks >= EXTRACT_TICKS && dazed) {
            this.extractBrain(victim);
            return;
        }
        if (!dazed && this.holdTicks >= RELEASE_AWAKE_TICKS) {
            this.release("came to");
            return;
        }
        if (this.holdTicks % SQUEEZE_INTERVAL == 0) {
            victim.timeUntilRegen = 0;
            if (victim.damage(this.getDamageSources().mobAttack(this), SQUEEZE_DAMAGE)) {
                this.playSound(SoundEvents.ENTITY_SQUID_HURT, 1.0F, 0.6F);
            }
        }
    }

    private void extractBrain(LivingEntity victim) {
        MobActions.log(this, "Extract Brain", victim);
        DnDClasses.LOGGER.info("[MindFlayer] Extract Brain on {} ({} damage)", victim.getName().getString(),
                EXTRACT_DAMAGE);
        this.triggerAnim("action_controller", "extract");
        victim.timeUntilRegen = 0;
        victim.damage(this.getDamageSources().indirectMagic(this, this), EXTRACT_DAMAGE);
        this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.5F, 0.5F);
        ((ServerWorld) this.world).spawnParticles(ParticleTypes.DAMAGE_INDICATOR, victim.getX(),
                victim.getEyeY(), victim.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.1D);
        this.release("brain extracted");
    }

    @Override
    public boolean isHolding(LivingEntity victim) {
        return this.held == victim && this.isAlive() && !this.isRemoved();
    }

    @Override
    public void breakFree(LivingEntity victim) {
        if (this.held == victim) {
            this.release("struggled free");
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (this.held != null || this.blastWindup > 0) {
            return false;
        }
        boolean hit = super.tryAttack(target);
        if (hit) {
            this.triggerAnim("action_controller", "lash");
        }
        return hit;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt && this.held != null && source.getAttacker() != this.held) {
            this.rescueDamage += amount;
            if (this.rescueDamage >= RESCUE_DAMAGE) {
                this.release("driven off");
            }
        }
        return hurt;
    }

    // ---- Blink and levitate ----

    private void tryBlink(long now) {
        boolean hurt = this.getHealth() < this.getMaxHealth() * BLINK_HEALTH;
        boolean crowded = !this.world.getEntitiesByClass(PlayerEntity.class,
                this.getBoundingBox().expand(BLINK_CROWD), p -> p != this.held && p.isAlive() && !p.isSpectator()
                        && !p.getAbilities().invulnerable && !Conditions.has(p, Condition.INCAPACITATED)
                        && this.distanceTo(p) <= BLINK_CROWD).isEmpty();
        if (!hurt && !crowded) {
            return;
        }
        this.nextBlink = now + BLINK_COOLDOWN;
        LivingEntity target = this.getTarget();
        if (this.held != null) {
            this.release("blinked away");
        }
        this.blastWindup = 0;
        this.setState(STATE_NONE);
        Vec3d from = this.getPos();
        boolean moved = Blink.blink(this, this.getBlockPos(), BLINK_MIN, BLINK_MAX, target, ParticleTypes.WITCH)
                || Blink.blink(this, this.getBlockPos(), BLINK_MIN, BLINK_MAX, null, ParticleTypes.WITCH);
        if (moved) {
            this.triggerAnim("action_controller", "blink");
        }
        MobActions.log(this, moved ? "blink" : "blink (no spot)", target);
        DnDClasses.LOGGER.info("[MindFlayer] blink ({}): {} -> {} ({} blocks)", hurt ? "hurt" : "crowded",
                BlockPos.ofFloored(from).toShortString(), this.getBlockPos().toShortString(),
                Math.round(from.distanceTo(this.getPos())));
    }

    private void tryLevitate() {
        LivingEntity target = this.getTarget();
        if (target == null || this.held != null) {
            return;
        }
        double dy = target.getY() - this.getY();
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        if (dy >= 3.0D && dx * dx + dz * dz <= 64.0D) {
            if (!this.hasStatusEffect(StatusEffects.LEVITATION)) {
                MobActions.log(this, "levitate", target);
            }
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, LEVITATE_TICKS, 0, false, false));
        }
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false; // it floats down
    }

    // ---- Thralls ----

    /** How many thralls of this Mind Flayer are nearby. */
    public int thrallCount() {
        return this.world.getEntitiesByClass(MobEntity.class, this.getBoundingBox().expand(THRALL_COUNT_RANGE),
                this::isThrall).size();
    }

    /**
     * Calls up {@code count} thralls round it, from {@link #THRALLS} (zombies and skeletons unless a datapack or
     * the Underdark adds more), each Charmed by it until it dies. Public so structures can give it more.
     */
    public void spawnThralls(ServerWorld world, int count) {
        List<RegistryEntry<EntityType<?>>> types = Registries.ENTITY_TYPE.getEntryList(THRALLS)
                .map(list -> list.stream().toList()).orElse(List.of());
        if (types.isEmpty()) {
            return;
        }
        for (int i = 0; i < count; i++) {
            EntityType<?> type = types.get(this.random.nextInt(types.size())).value();
            Entity entity = type.create(world);
            if (!(entity instanceof MobEntity thrall)) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            thrall.refreshPositionAndAngles(this.getX() + Math.cos(angle) * 1.5D, this.getY(),
                    this.getZ() + Math.sin(angle) * 1.5D, this.getYaw(), 0.0F);
            thrall.initialize(world, world.getLocalDifficulty(thrall.getBlockPos()), SpawnReason.REINFORCEMENT,
                    null, null);
            world.spawnEntityAndPassengers(thrall);
            this.enthrall(thrall);
        }
    }

    /** Thralls to call up on its first tick ({@value #SPAWN_THRALLS} after {@link #initialize}); 0 for none. */
    public void setPendingThralls(int count) {
        this.pendingThralls = Math.max(0, count);
    }

    /** Charms the {@link #COLONY_THRALL_TAG colony thralls} near it that nobody holds. Returns how many. */
    public int claimColonyThralls() {
        int claimed = 0;
        for (MobEntity mob : this.world.getEntitiesByClass(MobEntity.class,
                this.getBoundingBox().expand(COLONY_THRALL_RANGE), m -> m.isAlive()
                        && m.getCommandTags().contains(COLONY_THRALL_TAG) && !Conditions.has(m, Condition.CHARMED))) {
            if (this.enthrall(mob)) {
                claimed++;
            }
        }
        return claimed;
    }

    /** Makes {@code mob} this Mind Flayer's thrall: Charmed, indefinitely, until the flayer is gone. */
    public boolean enthrall(LivingEntity mob) {
        boolean charmed = Conditions.of(mob, Condition.CHARMED).source(this).endsWithSource()
                .ticks(Conditions.INDEFINITE).apply() != null;
        if (charmed) {
            MobActions.log(this, "thrall", mob);
        }
        return charmed;
    }

    /** Dominate: a hostile, non-boss mob nearby saves (WIS) or becomes a thrall. Never players. */
    private void tryDominate() {
        if (this.thrallCount() >= MAX_THRALLS) {
            return;
        }
        List<HostileEntity> candidates = this.world.getEntitiesByClass(HostileEntity.class,
                this.getBoundingBox().expand(DOMINATE_RANGE), m -> m != this && m.isAlive() && !(m instanceof Boss)
                        && !(m instanceof MindFlayerEntity) && !Conditions.has(m, Condition.CHARMED)
                        && !Conditions.isImmune(m, Condition.CHARMED, this) && this.distanceTo(m) <= DOMINATE_RANGE);
        if (candidates.isEmpty()) {
            return;
        }
        HostileEntity mob = candidates.get(this.random.nextInt(candidates.size()));
        SaveResult save = SavingThrow.of(mob, MindFlayerSaves.DOMINATE).source(this).tags("magic", "psychic").roll();
        MobActions.log(this, "dominate", mob);
        DnDClasses.LOGGER.info("[MindFlayer] dominate {}: {}", mob.getName().getString(),
                save.succeeded() ? "resists" : "enthralled");
        if (save.failed()) {
            this.enthrall(mob);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        if (this.held != null) {
            this.release("died");
        }
        super.onDeath(source);
    }

    /** Keeps it rooted while it holds someone or winds up a blast. */
    private class HoldStillGoal extends Goal {
        HoldStillGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP));
        }

        @Override
        public boolean canStart() {
            return MindFlayerEntity.this.held != null || MindFlayerEntity.this.blastWindup > 0;
        }

        @Override
        public void start() {
            MindFlayerEntity.this.getNavigation().stop();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            MindFlayerEntity.this.getNavigation().stop();
            Vec3d v = MindFlayerEntity.this.getVelocity();
            MindFlayerEntity.this.setVelocity(0.0D, v.y, 0.0D);
        }
    }

    // ---- Sounds ----

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_SQUID_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_SQUID_SQUIRT, 0.05F, 1.6F);
    }

    @Override
    public float getSoundPitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 1.3F;
    }

    // ---- Animation ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::predicate));
        AnimationController<MindFlayerEntity> action = new AnimationController<>(this, "action_controller", 0,
                state -> PlayState.STOP);
        for (String name : new String[] {"lash", "extract", "blink"}) {
            action.triggerableAnim(name, RawAnimation.begin().thenPlay(name));
        }
        controllers.add(action);
    }

    private PlayState predicate(AnimationState<MindFlayerEntity> state) {
        if (this.isGrabbing()) {
            return state.setAndContinue(GRAB);
        }
        if (this.isBlasting()) {
            return state.setAndContinue(BLAST);
        }
        if (this.dataTracker.get(LEVITATING)) {
            return state.setAndContinue(LEVITATE);
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
        nbt.putInt("BlastCooldown", this.blastCooldown);
        if (this.pendingThralls > 0) {
            nbt.putInt("PendingThralls", this.pendingThralls);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("BlastCooldown")) {
            this.blastCooldown = nbt.getInt("BlastCooldown");
        }
        this.pendingThralls = nbt.getInt("PendingThralls");
    }
}
