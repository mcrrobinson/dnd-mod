package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.SkillChecks.Study;
import mattonfire.dnd.world.underdark.Underdark;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A duergar of the Grey Clans (design 9 T5; see docs/mobs/duergar.md). The base class holds what they all share:
 * a grey skin variant, a dwarven name, a home spot (structure and summoned ones), and the guard rules of a
 * {@link SettlementGuard}: they attack players Hostile with the Grey Clans on sight (Dwarves start there), anyone
 * who strikes a duergar (every duergar within {@link #DEFENCE_RANGE} joins in) and the clans' enemies
 * ({@link #CLAN_ENEMIES}). Everyone else is glowered at and left alone.
 *
 * <p>Racial traits, shared by every duergar:
 * <ul>
 * <li><b>Enlarge</b> ({@link #canEnlarge()}): once per fight, when it first takes damage or its target comes within
 * {@link #ENLARGE_TRIGGER_RANGE} blocks, it grows to {@link #ENLARGE_SCALE}x for {@link #ENLARGE_TICKS} ticks: a
 * bigger hitbox, +{@link #ENLARGE_DAMAGE} attack damage, +{@link #ENLARGE_KNOCKBACK} knockback resistance and
 * +{@link #ENLARGE_REACH} reach. Tracked ({@code Enlarged} in NBT) so the renderer scales with it.</li>
 * <li><b>Invisibility</b>: once per fight, when it drops below half health, it turns invisible for
 * {@link #INVISIBLE_TICKS} ticks and backs off about {@link #RETREAT_DISTANCE} blocks; mobs fighting it lose it
 * as a target, and it doesn't pick a target itself for {@link #RETREAT_TICKS} ticks.</li>
 * <li><b>Duergar Resilience</b>: poison effects on it last half as long ({@link #isPoison}).</li>
 * <li><b>Sunlight Sensitivity</b>: under the open sky by day (sky light {@link #SUNLIGHT_LEVEL}+), Weakness I and
 * Slowness I, refreshed every second.</li>
 * </ul>
 * A fight ends (and Enlarge and Invisibility are ready again) after {@link #FIGHT_RESET_TICKS} ticks without a
 * target. Special actions log {@code [MobAction]} lines at DEBUG level ({@link MobActions}).
 */
public abstract class DuergarEntity extends SettlementNpcEntity implements GuardAnger {
    public static final int VARIANTS = 6;

    public static final float ENLARGE_SCALE = 1.5F;
    public static final int ENLARGE_TICKS = 15 * 20;
    public static final double ENLARGE_TRIGGER_RANGE = 6.0D;
    public static final double ENLARGE_DAMAGE = 4.0D;
    public static final double ENLARGE_KNOCKBACK = 0.3D;
    public static final double ENLARGE_REACH = 1.0D;
    /** Ticks the renderer takes to grow or shrink. */
    public static final int ENLARGE_EASE_TICKS = 10;
    public static final UUID ENLARGE_DAMAGE_ID = UUID.fromString("4b0d3c55-6a5e-4b8e-9f4e-1d7a2f0c9e01");
    public static final UUID ENLARGE_KNOCKBACK_ID = UUID.fromString("4b0d3c55-6a5e-4b8e-9f4e-1d7a2f0c9e02");

    public static final int INVISIBLE_TICKS = 6 * 20;
    public static final int RETREAT_TICKS = 3 * 20;
    public static final int RETREAT_DISTANCE = 8;
    /** Mobs this close that were fighting a duergar lose it when it turns invisible. */
    public static final double LOSE_TARGET_RANGE = 32.0D;

    public static final int SUNLIGHT_LEVEL = 12;
    public static final int FIGHT_RESET_TICKS = 10 * 20;
    /** Duergar this close to a struck duergar come to its defence. */
    public static final double DEFENCE_RANGE = 16.0D;
    /** Duergar keep within this many blocks of home (structure and summoned ones). */
    public static final int HOME_RANGE = 16;

    /** Creatures duergar fight on sight: the drow (T4) and whatever a data pack adds. */
    public static final TagKey<EntityType<?>> CLAN_ENEMIES = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "duergar_enemies"));

    private static final TrackedData<Boolean> ENLARGED = DataTracker.registerData(DuergarEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);

    private static final String[] FIRST_NAMES = {
            "Brakk", "Duerg", "Grimmak", "Horgar", "Khazgrim", "Morgrath", "Thrum", "Ulgrim", "Vorn", "Yurk",
            "Ashka", "Dregga", "Grisla", "Kragna", "Mordra", "Sootha", "Thrava", "Varga"
    };
    private static final String[] CLAN_NAMES = {
            "Ashforge", "Blackvein", "Cinderhelm", "Deepscar", "Gloomhammer", "Greyhand", "Ironsoot", "Slagborn",
            "Stonegrudge", "Underbrand"
    };

    private final GuardAnger.State anger = new GuardAnger.State();
    private int enlargeTicks;
    private boolean enlargeSpent;
    private boolean invisibilitySpent;
    private int retreatTicks;
    private int calmTicks;
    // Client: how far the renderer has grown, 0..ENLARGE_EASE_TICKS.
    private int easeTicks;
    private int prevEaseTicks;

    protected DuergarEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world, HOME_RANGE);
        this.experiencePoints = 8;
    }

    /** Registers the Study weakness; call once after the entity types. */
    public static void registerStudy() {
        Text weakness = Text.translatable("study.dndclasses.weakness.duergar");
        for (EntityType<?> type : DuergarEntities.all()) {
            Study.registerWeakness(type, weakness);
        }
    }

    @Override
    protected int variants() {
        return VARIANTS;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.initCombatGoals();
        this.goalSelector.add(3, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        SettlementGuard.addTargetGoals(this.targetSelector, this, DuergarEntity.class, DuergarEntity::isClanEnemy);
    }

    /** The fighting goals (priority 1-2): a melee swing with the Enlarge reach by default. */
    protected void initCombatGoals() {
        this.goalSelector.add(2, new DuergarMeleeGoal(this, 1.1D, 0.0D));
    }

    /** The Grey Clans' enemies, fought on sight: {@code #dndclasses:duergar_enemies}. */
    public static boolean isClanEnemy(LivingEntity entity) {
        return entity.getType().isIn(CLAN_ENEMIES);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.setVariant(random.nextInt(VARIANTS));
        this.setCustomName(Text.literal(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + CLAN_NAMES[random.nextInt(CLAN_NAMES.length)]));
        // Natural patrols roam and despawn like monsters; Hold folk and summoned ones stay and keep home.
        if (spawnReason != SpawnReason.NATURAL) {
            this.setPersistent();
            if (this.getHome() == null) {
                this.setHome(this.getBlockPos());
            }
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return !this.isPersistent() && this.getHome() == null;
    }

    // ---- Who it fights ----

    @Override
    protected boolean mayTarget(LivingEntity target) {
        return this.retreatTicks <= 0 && SettlementGuard.mayTarget(this, target, DuergarEntity.class);
    }

    /** Angry at the player it holds a grudge against, and at anyone Hostile with the Grey Clans, on sight. */
    @Override
    public boolean shouldAngerAt(LivingEntity entity) {
        return GuardAnger.super.shouldAngerAt(entity) || SettlementGuard.attacksOnSight(this, entity);
    }

    @Override
    protected boolean isFriendlyFire(DamageSource source) {
        return source.getAttacker() instanceof DuergarEntity;
    }

    /** Strike a duergar and every duergar nearby turns on you. */
    @Override
    protected void callGuards(PlayerEntity player) {
        defend(this, player, DuergarEntity.class);
    }

    @Override
    public void provokeShout() {
        this.playSound(SoundEvents.ENTITY_VINDICATOR_CELEBRATE, 1.0F, this.getSoundPitch());
    }

    @Override
    protected void afterHurt(DamageSource source) {
        if (!this.isAlive()) {
            return;
        }
        if (this.getTarget() != null || source.getAttacker() instanceof LivingEntity) {
            this.tryEnlarge(source.getAttacker() instanceof LivingEntity living ? living : this.getTarget());
        }
        if (this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.tryVanish(source.getAttacker() instanceof LivingEntity living ? living : this.getTarget());
        }
    }

    // ---- Enlarge ----

    /** Whether this kind of duergar uses Enlarge (not the factor). */
    protected boolean canEnlarge() {
        return true;
    }

    public boolean isEnlarged() {
        return this.dataTracker.get(ENLARGED);
    }

    /** Grows for {@link #ENLARGE_TICKS} ticks, once per fight, if there's room. */
    public boolean tryEnlarge(@Nullable LivingEntity target) {
        if (!this.canEnlarge() || this.enlargeSpent || this.isEnlarged() || this.world.isClient) {
            return false;
        }
        EntityDimensions big = enlarged(this.getType().getDimensions());
        Box box = big.getBoxAt(this.getPos());
        if (!this.world.isSpaceEmpty(this, box)) {
            return false;
        }
        this.enlargeSpent = true;
        this.setEnlarged(true);
        this.enlargeTicks = ENLARGE_TICKS;
        this.playSound(SoundEvents.ENTITY_IRON_GOLEM_REPAIR, 1.0F, 0.6F);
        if (this.world instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getBodyY(0.5D), this.getZ(), 20,
                    0.5D, 0.8D, 0.5D, 0.02D);
        }
        MobActions.log(this, "enlarge", target);
        return true;
    }

    public void setEnlarged(boolean enlarged) {
        this.dataTracker.set(ENLARGED, enlarged);
        modifier(this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), ENLARGE_DAMAGE_ID, "Duergar Enlarge",
                ENLARGE_DAMAGE, enlarged);
        modifier(this.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), ENLARGE_KNOCKBACK_ID,
                "Duergar Enlarge", ENLARGE_KNOCKBACK, enlarged);
        if (!enlarged) {
            this.enlargeTicks = 0;
        }
    }

    private static void modifier(@Nullable EntityAttributeInstance attribute, UUID id, String name, double amount, boolean on) {
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(id);
        if (on) {
            attribute.addPersistentModifier(new EntityAttributeModifier(id, name, amount, EntityAttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        EntityDimensions dimensions = super.getDimensions(pose);
        return this.isEnlarged() ? enlarged(dimensions) : dimensions;
    }

    // EntityDimensions.scaled leaves fixed dimensions (every entity type's) as they are, so scale by hand.
    private static EntityDimensions enlarged(EntityDimensions dimensions) {
        return EntityDimensions.fixed(dimensions.width * ENLARGE_SCALE, dimensions.height * ENLARGE_SCALE);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (ENLARGED.equals(data)) {
            this.calculateDimensions();
        }
    }

    /** The renderer's scale this frame: 1 to {@link #ENLARGE_SCALE}, eased over {@link #ENLARGE_EASE_TICKS} ticks. */
    public float renderScale(float tickDelta) {
        float ease = MathHelper.lerp(tickDelta, this.prevEaseTicks, this.easeTicks) / ENLARGE_EASE_TICKS;
        return 1.0F + (ENLARGE_SCALE - 1.0F) * ease;
    }

    // ---- Invisibility ----

    /** Turns invisible and backs off from {@code threat}, once per fight. */
    public boolean tryVanish(@Nullable LivingEntity threat) {
        if (this.invisibilitySpent || this.world.isClient) {
            return false;
        }
        this.invisibilitySpent = true;
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, INVISIBLE_TICKS, 0, false, false, true));
        this.retreatTicks = RETREAT_TICKS;
        super.setTarget(null);
        for (MobEntity mob : this.world.getEntitiesByClass(MobEntity.class, this.getBoundingBox().expand(LOSE_TARGET_RANGE),
                mob -> mob.getTarget() == this)) {
            mob.setTarget(null);
        }
        if (threat != null) {
            Vec3d away = NoPenaltyTargeting.findFrom(this, RETREAT_DISTANCE + 4, 4, threat.getPos());
            if (away != null) {
                this.getNavigation().startMovingTo(away.x, away.y, away.z, 1.3D);
            }
        }
        this.world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
                SoundCategory.HOSTILE, 1.0F, 0.8F);
        if (this.world instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getBodyY(0.5D), this.getZ(), 30,
                    0.4D, 0.6D, 0.4D, 0.03D);
        }
        MobActions.log(this, "invisibility", threat);
        return true;
    }

    /** Whether it's backing off after turning invisible (it picks no target meanwhile). */
    public boolean isRetreating() {
        return this.retreatTicks > 0;
    }

    // ---- Duergar Resilience ----

    /** Poison effects (vanilla Poison and any effect with "poison" in its id, e.g. Drow Poison) last half as long. */
    @Override
    public boolean addStatusEffect(StatusEffectInstance effect, @Nullable net.minecraft.entity.Entity source) {
        if (isPoison(effect.getEffectType()) && effect.getDuration() > 1) {
            effect = new StatusEffectInstance(effect.getEffectType(), Math.max(1, effect.getDuration() / 2),
                    effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
        }
        return super.addStatusEffect(effect, source);
    }

    public static boolean isPoison(StatusEffect effect) {
        if (effect == StatusEffects.POISON) {
            return true;
        }
        Identifier id = Registries.STATUS_EFFECT.getId(effect);
        return id != null && id.getPath().contains("poison");
    }

    // ---- Ticking ----

    @Override
    public void tick() {
        super.tick();
        if (this.world.isClient) {
            this.prevEaseTicks = this.easeTicks;
            this.easeTicks = MathHelper.clamp(this.easeTicks + (this.isEnlarged() ? 1 : -1), 0, ENLARGE_EASE_TICKS);
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.world.isClient) {
            return;
        }
        this.tickAngerLogic((ServerWorld) this.world, true);
        LivingEntity target = this.getTarget();
        if (this.retreatTicks > 0) {
            this.retreatTicks--;
        }
        if (this.enlargeTicks > 0 && --this.enlargeTicks == 0) {
            this.setEnlarged(false);
            this.playSound(SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, 0.6F, 1.4F);
        }
        if (target != null) {
            this.calmTicks = 0;
            if (target.isAlive() && this.squaredDistanceTo(target) <= ENLARGE_TRIGGER_RANGE * ENLARGE_TRIGGER_RANGE) {
                this.tryEnlarge(target);
            }
        } else if (!this.isRetreating() && ++this.calmTicks >= FIGHT_RESET_TICKS) {
            this.enlargeSpent = false;
            this.invisibilitySpent = false;
        }
        if (this.age % 20 == 0) {
            this.tickSunlight();
        }
    }

    /** Sunlight Sensitivity: Weakness I and Slowness I under a bright sky by day. */
    private void tickSunlight() {
        if (!this.isInSunlight()) {
            return;
        }
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0, true, true));
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, true));
    }

    public boolean isInSunlight() {
        World world = this.world;
        if (Underdark.isUnderdark(world) || !world.getDimension().hasSkyLight() || !world.isDay()) {
            return false;
        }
        BlockPos eyes = BlockPos.ofFloored(this.getX(), this.getEyeY(), this.getZ());
        return world.getLightLevel(LightType.SKY, eyes) >= SUNLIGHT_LEVEL;
    }

    // ---- Angerable ----

    @Override
    public GuardAnger.State angerState() {
        return this.anger;
    }

    // ---- Sounds: a vindicator's, deeper ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 0.75F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VINDICATOR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VINDICATOR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VINDICATOR_DEATH;
    }

    // ---- Saving ----

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Enlarged", this.isEnlarged());
        nbt.putInt("EnlargeTicks", this.enlargeTicks);
        nbt.putBoolean("EnlargeSpent", this.enlargeSpent);
        nbt.putBoolean("InvisibilitySpent", this.invisibilitySpent);
        this.writeAngerToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setEnlarged(nbt.getBoolean("Enlarged"));
        this.enlargeTicks = nbt.getInt("EnlargeTicks");
        if (this.isEnlarged() && this.enlargeTicks <= 0) {
            this.enlargeTicks = ENLARGE_TICKS;
        }
        this.enlargeSpent = nbt.getBoolean("EnlargeSpent");
        this.invisibilitySpent = nbt.getBoolean("InvisibilitySpent");
        this.readAngerFromNbt(this.world, nbt);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ENLARGED, false);
    }

    /** A melee swing whose reach grows by {@link #ENLARGE_REACH} while enlarged, plus {@code extraReach} (a whip). */
    public static class DuergarMeleeGoal extends MeleeAttackGoal {
        private final DuergarEntity duergar;
        private final double extraReach;

        public DuergarMeleeGoal(DuergarEntity duergar, double speed, double extraReach) {
            super(duergar, speed, true);
            this.duergar = duergar;
            this.extraReach = extraReach;
        }

        @Override
        protected double getSquaredMaxAttackDistance(LivingEntity target) {
            double reach = this.duergar.getWidth() * 2.0F + (this.duergar.isEnlarged() ? ENLARGE_REACH : 0.0D) + this.extraReach;
            return reach * reach + target.getWidth();
        }
    }
}
