package mattonfire.dnd.entity.kobold;

import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.ai.goal.FactionAllyGoal;
import mattonfire.dnd.entity.kobold.ai.KoboldFleeGoal;
import mattonfire.dnd.entity.kobold.ai.KoboldKeepDistanceGoal;
import mattonfire.dnd.entity.kobold.ai.KoboldMeleeGoal;
import mattonfire.dnd.faction.Provokable;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
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
 * A kobold (design 10, section 3; docs/mobs/kobolds.md): small, scaly, cowardly and clever. What all four share:
 * <ul>
 * <li>A skin variant (0-4), the 0.5 x 0.95 body and the GeckoLib model.</li>
 * <li>Courage ({@link KoboldCourage}): a brave kobold fights in melee; a craven one keeps 10-14 blocks from its
 * target ({@link KoboldKeepDistanceGoal}) and runs from a player who hits it, and below 30% health any kobold runs
 * for its pack ({@link KoboldFleeGoal}).</li>
 * <li>Who it fights ({@link KoboldHostility}): by the player's standing with the Kobold Tribes, and anyone who
 * strikes a kobold (every kobold within {@link #RALLY_RANGE} blocks holds the grudge for {@link #GRUDGE_TICKS}).
 * At Exalted it joins its friend's fights ({@link FactionAllyGoal}).</li>
 * <li>Pack Tactics and Sunlight Sensitivity, through {@code ConditionEffects.registerMobAttackMode}.</li>
 * <li>Kobolds never hurt each other.</li>
 * </ul>
 * Kobolds placed by a structure are persistent; natural ones despawn like other monsters.
 */
public abstract class KoboldEntity extends HostileEntity implements GeoEntity, Provokable {
    public static final int VARIANTS = 5;
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(KoboldEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> COWERING = DataTracker.registerData(KoboldEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    /** Kobolds this close to a struck kobold hold the grudge too. */
    public static final double RALLY_RANGE = 16.0D;
    /** How long a kobold holds a grudge against a player who struck one of its pack (30 s). */
    public static final int GRUDGE_TICKS = 20 * 30;
    /** A craven kobold hit by a player runs for this long (5 s). */
    public static final int SCARED_TICKS = 100;
    /** Below this share of its health a kobold runs for its pack. */
    public static final float LOW_HEALTH = 0.3F;
    /** The "wyrm-killer!" warning, at most once per this many ticks per kobold (30 s). */
    public static final int WARN_COOLDOWN = 20 * 30;

    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation COWER = RawAnimation.begin().thenLoop("cower");
    protected static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    protected static final RawAnimation SLING = RawAnimation.begin().thenPlay("sling");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID grudge;
    private int grudgeUntil;
    @Nullable
    private UUID scaredOf;
    private int scaredUntil;
    private int lastWarnAt = -WARN_COOLDOWN;
    @Nullable
    private BlockPos home;
    /** Courage is asked every tick by several goals: cached for {@link #COURAGE_CACHE_TICKS}. */
    private static final int COURAGE_CACHE_TICKS = 10;
    private int courageAt = -100;
    @Nullable
    private UUID courageAgainst;
    private boolean courage;

    protected KoboldEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 4;
    }

    protected static DefaultAttributeContainer.Builder koboldAttributes(double health, double armor, double speed,
            double damage) {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, health)
                .add(EntityAttributes.GENERIC_ARMOR, armor)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, speed)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, damage)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        // Frightened: ConditionFleeGoal (priority 1), added to every path mob when it loads
        this.goalSelector.add(1, new KoboldFleeGoal(this));
        this.initCombatGoals();
        this.goalSelector.add(3, new KoboldKeepDistanceGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, KoboldEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                player -> KoboldHostility.attacksOnSight(this, (PlayerEntity) player)));
        this.targetSelector.add(3, new FactionAllyGoal(this, KoboldHostility.ALLY_RANGE, KoboldHostility::fightsBeside,
                Kobolds::isKobold));
    }

    /** The fighting goal (priority 2): a melee jab while brave. Slingers sling instead. */
    protected void initCombatGoals() {
        this.goalSelector.add(2, new KoboldMeleeGoal(this, 1.2D));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(COWERING, false);
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.84F;
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, Math.floorMod(variant, VARIANTS));
    }

    public boolean isCowering() {
        return this.dataTracker.get(COWERING);
    }

    public void setCowering(boolean cowering) {
        this.dataTracker.set(COWERING, cowering);
    }

    /** A spot it keeps near (a warren den, a lair post; K2 and D2 set it). Null for wild kobolds. */
    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    public void setHome(BlockPos home, int range) {
        this.home = home.toImmutable();
        this.setPositionTarget(this.home, range);
    }

    // ---- Courage ----

    /** Whether this kobold is brave against {@code against} (or in general, for null): {@link KoboldCourage}. */
    public boolean isBrave(@Nullable LivingEntity against) {
        UUID id = against instanceof PlayerEntity ? against.getUuid() : null;
        if (this.age - this.courageAt >= COURAGE_CACHE_TICKS || !java.util.Objects.equals(id, this.courageAgainst)) {
            this.courage = KoboldCourage.isBrave(this, against instanceof PlayerEntity p ? p : null);
            this.courageAt = this.age;
            this.courageAgainst = id;
        }
        return this.courage;
    }

    public boolean isLowHealth() {
        return this.getHealth() < this.getMaxHealth() * LOW_HEALTH;
    }

    /** Brave against its target and not badly hurt: it closes in to fight. */
    public boolean wantsMelee() {
        LivingEntity target = this.getTarget();
        return target != null && !this.isLowHealth() && this.isBrave(target);
    }

    /** The player it's running from right now (it was hit while craven), or null. */
    @Nullable
    public PlayerEntity scaredOf() {
        if (this.scaredOf == null || this.age >= this.scaredUntil) {
            return null;
        }
        return this.world.getPlayerByUuid(this.scaredOf);
    }

    // ---- Who it fights ----

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && !this.world.isClient && target != this.getTarget() && !this.mayTarget(target)) {
            return;
        }
        super.setTarget(target);
    }

    protected boolean mayTarget(LivingEntity target) {
        if (Kobolds.isKobold(target)) {
            return false;
        }
        if (target instanceof PlayerEntity player) {
            return KoboldHostility.mayFight(this, player, false);
        }
        return true;
    }

    @Override
    public boolean provokedBy(PlayerEntity player) {
        return this.grudge != null && this.grudge.equals(player.getUuid()) && this.age < this.grudgeUntil;
    }

    /** {@code player} wronged the pack: this kobold goes for them, whatever their standing, for 30 s. */
    public void provoke(PlayerEntity player) {
        this.grudge = player.getUuid();
        this.grudgeUntil = this.age + GRUDGE_TICKS;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Kobolds never hurt each other.
        if (source.getAttacker() != null && Kobolds.isKobold(source.getAttacker())) {
            return false;
        }
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient && source.getAttacker() instanceof PlayerEntity player
                && !player.isCreative() && !player.isSpectator()) {
            this.onStruckBy(player);
        }
        return hurt;
    }

    /** A player hit it: the pack holds a grudge, and a craven kobold runs. */
    protected void onStruckBy(PlayerEntity player) {
        for (KoboldEntity other : this.world.getEntitiesByClass(KoboldEntity.class,
                new Box(this.getBlockPos()).expand(RALLY_RANGE), KoboldEntity::isAlive)) {
            other.provoke(player);
        }
        if (!this.isBrave(player)) {
            if (this.scaredOf() == null) {
                DnDClasses.LOGGER.info("[Kobold] {} loses its nerve and runs from {}", this.getName().getString(),
                        player.getEntityName());
            }
            this.scaredOf = player.getUuid();
            this.scaredUntil = this.age + SCARED_TICKS;
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.world.isClient) {
            return;
        }
        if (this.grudge != null && this.getTarget() instanceof PlayerEntity target && this.provokedBy(target)) {
            this.grudgeUntil = Math.max(this.grudgeUntil, this.age + 100);
        }
        if (this.age % 20 == 0) {
            // Stand down if the player's standing changed mid-fight.
            if (this.getTarget() instanceof PlayerEntity player && !KoboldHostility.mayFight(this, player, true)) {
                super.setTarget(null);
            }
            this.warnOfWyrmKiller();
        }
    }

    /** Kobolds that see a dragon-slayer (in the last 3 days) chirp a warning, once per 30 s. */
    private void warnOfWyrmKiller() {
        if (this.age - this.lastWarnAt < WARN_COOLDOWN) {
            return;
        }
        for (ServerPlayerEntity player : ((ServerWorld) this.world).getPlayers(p -> p.isAlive() && !p.isSpectator()
                && p.squaredDistanceTo(this) <= 16 * 16 && KoboldTribute.isDreaded(p) && this.canSee(p))) {
            this.lastWarnAt = this.age;
            this.playSound(SoundEvents.ENTITY_FOX_SCREECH, 1.0F, 1.6F);
            player.sendMessage(Text.translatable("entity.dndclasses.kobold.wyrm_killer", this.getDisplayName())
                    .formatted(Formatting.GOLD), true);
            DnDClasses.LOGGER.info("[Kobold] {} spots the wyrm-killer {}", this.getName().getString(),
                    player.getEntityName());
            return;
        }
    }

    // ---- Spawning ----

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
            @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.setVariant(random.nextInt(VARIANTS));
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (spawnReason == SpawnReason.NATURAL && world instanceof ServerWorld serverWorld) {
            KoboldPacks.spawnPack(serverWorld, this, random);
        }
        return data;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    // ---- Sounds: a fox's yips, higher ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 1.5F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_FOX_DEATH;
    }

    // ---- Animation ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::movementPredicate));
        controllers.add(new AnimationController<KoboldEntity>(this, "action_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("sling", SLING));
    }

    private PlayState movementPredicate(AnimationState<KoboldEntity> state) {
        if (this.isCowering()) {
            return state.setAndContinue(COWER);
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
        nbt.putInt("Variant", this.getVariant());
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
            nbt.putInt("HomeRange", (int) this.getPositionTargetRange());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setVariant(nbt.getInt("Variant"));
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")), nbt.contains("HomeRange") ? nbt.getInt("HomeRange") : 16);
        }
    }

    /** Lets TierEffects count a kobold struck by the player as provocation for its kin, like other factions. */
    static boolean provokedByStrike(KoboldEntity kobold, PlayerEntity player) {
        return TierEffects.provoked(kobold, player);
    }
}
