package mattonfire.dnd.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.ConditionInstance;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Basilisk (design 8, section 7): a slow, heavily armoured eight-legged lizard of the deep caves.
 * <ul>
 * <li><b>Petrifying gaze</b> (passive): every {@value #GAZE_INTERVAL} ticks, each player within
 * {@value #GAZE_RANGE} blocks who can see it and looks within {@value #GAZE_ANGLE}° of its eyes makes a CON save
 * ({@link BasiliskSaves#GAZE}), at most once per {@value #GAZE_ROLL_TICKS} ticks. A failure starts turning them to
 * stone: Restrained (key {@value BasiliskSaves#PETRIFYING_KEY}) with a repeat save that ends it or, failed, turns them
 * Petrified for {@link BasiliskSaves#PETRIFIED_TICKS} ticks. Failing by {@link BasiliskSaves#FAIL_BY} or more
 * petrifies at once. Blind players are safe.</li>
 * <li><b>Shield reflection</b>: a player raising a shield toward it within range makes no save; instead the
 * basilisk saves against its own gaze (at most once per {@value #REFLECT_COOLDOWN} ticks) and on a failure becomes
 * a statue (Petrified indefinitely, shattered by a pickaxe for its loot).</li>
 * <li>Other creatures within range that face it save every {@value #MOB_GAZE_INTERVAL} ticks or become statues.</li>
 * <li><b>Bite</b>: 6 damage and a CON save ({@link BasiliskSaves#BITE}) against Poisoned and Poison I.</li>
 * </ul>
 * The gaze runs from the server tick, not a goal, so it works on a {@code NoAI} basilisk too; it stops while the
 * basilisk itself can't act ({@link Conditions#canAct}).
 */
public class BasiliskEntity extends HostileEntity implements GeoEntity {
    public static final int GAZE_INTERVAL = 20;
    public static final double GAZE_RANGE = 8.0D;
    public static final double GAZE_ANGLE = 30.0D;
    /** One gaze save per player per this many ticks. */
    public static final int GAZE_ROLL_TICKS = 100;
    /** A shield counts as raised at the basilisk when the player faces it within this angle. */
    public static final double SHIELD_ANGLE = 60.0D;
    public static final int REFLECT_COOLDOWN = 200;
    public static final int MOB_GAZE_INTERVAL = 100;
    /** How far in front of its centre the eyes are (the model is longer than the hitbox). */
    private static final double HEAD_FORWARD = 0.85D;
    private static final double EYE_HEIGHT = 0.7D;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation STATUE = RawAnimation.begin().thenLoop("statue");
    public static final String GAZE_ANIM = "gaze";
    public static final String BITE_ANIM = "bite";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Player UUID to the world time their next gaze save may roll. Not saved: a reload just allows a new roll. */
    private final Map<UUID, Long> nextGazeRoll = new HashMap<>();
    private long nextReflect;

    public BasiliskEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 20;
    }

    public static DefaultAttributeContainer.Builder createBasiliskAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 10.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.18D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    /** Natural spawns: in the dark, well underground (below y={@value #MAX_SPAWN_Y}) and out of the sky. */
    public static final int MAX_SPAWN_Y = -16;

    public static boolean canSpawn(EntityType<BasiliskEntity> type, ServerWorldAccess world, SpawnReason reason,
            BlockPos pos, Random random) {
        if (!HostileEntity.canSpawnInDark(type, world, reason, pos, random)) {
            return false;
        }
        return reason != SpawnReason.NATURAL || (pos.getY() < MAX_SPAWN_Y && !world.isSkyVisible(pos));
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return (float) EYE_HEIGHT;
    }

    // ---- Bite ----

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            this.triggerAnim("action", BITE_ANIM);
            if (target instanceof LivingEntity living && living.isAlive()) {
                MobActions.log(this, "bite", living);
                SaveResult save = SavingThrow.of(living, BasiliskSaves.BITE).source(this).tags("poison")
                        .onSuccess(Text.translatable("save.dndclasses.effect.basilisk_bite"))
                        .onFailure(Text.translatable("save.dndclasses.effect.basilisk_bite.failure"))
                        .roll();
                if (save.failed()) {
                    Conditions.of(living, Condition.POISONED).source(this).ticks(BasiliskSaves.POISON_TICKS).apply();
                    living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, BasiliskSaves.POISON_TICKS, 0),
                            this);
                }
            }
        }
        return hit;
    }

    // ---- Gaze ----

    /** Where its eyes are: at the front of the model, ahead of the hitbox centre. */
    public Vec3d eyesPos() {
        float yaw = this.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(this.getX() - MathHelper.sin(yaw) * HEAD_FORWARD, this.getY() + EYE_HEIGHT,
                this.getZ() + MathHelper.cos(yaw) * HEAD_FORWARD);
    }

    /** Whether {@code viewer} looks within {@code degrees} of the basilisk's eyes. */
    public boolean isLookedAtBy(LivingEntity viewer, double degrees) {
        Vec3d toEyes = this.eyesPos().subtract(viewer.getEyePos());
        double len = toEyes.length();
        if (len < 1.0E-4) {
            return true;
        }
        double cos = viewer.getRotationVec(1.0F).dotProduct(toEyes.multiply(1.0D / len));
        return cos >= Math.cos(Math.toRadians(degrees));
    }

    private static boolean isBlind(LivingEntity entity) {
        return Conditions.has(entity, Condition.BLINDED) || entity.hasStatusEffect(StatusEffects.BLINDNESS);
    }

    /** Already turning to stone or a statue: no new save. */
    private static boolean alreadyPetrifying(LivingEntity entity) {
        if (Conditions.has(entity, Condition.PETRIFIED)) {
            return true;
        }
        for (ConditionInstance inst : Conditions.all(entity)) {
            if (inst.condition() == Condition.RESTRAINED && BasiliskSaves.PETRIFYING_KEY.equals(inst.key())) {
                return true;
            }
        }
        return false;
    }

    private void tickGaze(ServerWorld world) {
        long now = world.getTime();
        boolean gazed = false;
        for (PlayerEntity player : world.getPlayers()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()
                    || player.squaredDistanceTo(this) > GAZE_RANGE * GAZE_RANGE || isBlind(player)
                    || !player.canSee(this)) {
                continue;
            }
            if (player.isBlocking() && this.isLookedAtBy(player, SHIELD_ANGLE)) {
                this.reflect(player, now);
                continue;
            }
            if (!this.isLookedAtBy(player, GAZE_ANGLE) || alreadyPetrifying(player)) {
                continue;
            }
            Long next = this.nextGazeRoll.get(player.getUuid());
            if (next != null && now < next) {
                continue;
            }
            this.nextGazeRoll.put(player.getUuid(), now + GAZE_ROLL_TICKS);
            gazed = true;
            this.gaze(player);
        }
        if (gazed) {
            this.triggerAnim("action", GAZE_ANIM);
            world.playSound(null, this.getX(), this.getEyeY(), this.getZ(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
                    SoundCategory.HOSTILE, 0.35F, 1.8F);
        }
        if (this.age % MOB_GAZE_INTERVAL == 0) {
            this.gazeAtMobs(world);
        }
    }

    private void gaze(PlayerEntity player) {
        MobActions.log(this, "gaze", player);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.translatable("entity.dndclasses.basilisk.gaze").formatted(Formatting.GRAY),
                    true);
        }
        SaveResult save = SavingThrow.of(player, BasiliskSaves.GAZE).source(this).tags("petrify")
                .onSuccess(Text.translatable("save.dndclasses.effect.basilisk_gaze"))
                .onFailure(Text.translatable("save.dndclasses.effect.basilisk_gaze.failure"))
                .roll();
        if (save.succeeded()) {
            return;
        }
        if (save.roll().total() <= BasiliskSaves.GAZE_DC - BasiliskSaves.FAIL_BY) {
            Conditions.of(player, Condition.PETRIFIED).source(this).key(BasiliskSaves.PETRIFYING_KEY)
                    .ticks(BasiliskSaves.PETRIFIED_TICKS).apply();
        } else {
            petrifying(player, this);
        }
    }

    /**
     * Starts turning {@code target} to stone: Restrained with a CON repeat save every
     * {@link BasiliskSaves#GAZE_REPEAT_TICKS} ticks that ends it, or on a failure makes it Petrified for
     * {@link BasiliskSaves#PETRIFIED_TICKS} ticks. Public so other petrifying sources can reuse the basilisk's rule.
     */
    public static ConditionInstance petrifying(LivingEntity target, BasiliskEntity source) {
        return Conditions.of(target, Condition.RESTRAINED).source(source).key(BasiliskSaves.PETRIFYING_KEY)
                .ticks(Conditions.INDEFINITE)
                .repeatSave(BasiliskSaves.GAZE, BasiliskSaves.GAZE_REPEAT_TICKS)
                .saveLabel("save.dndclasses.basilisk_petrify")
                .saveTags("petrify")
                .escalateOnFailure(Condition.PETRIFIED, BasiliskSaves.PETRIFIED_TICKS)
                .apply();
    }

    /** A raised shield turns its gaze back on it: it saves against itself or becomes a statue. */
    private void reflect(PlayerEntity player, long now) {
        if (now < this.nextReflect) {
            return;
        }
        this.nextReflect = now + REFLECT_COOLDOWN;
        MobActions.log(this, "reflected gaze", player);
        this.triggerAnim("action", GAZE_ANIM);
        SaveResult save = SavingThrow.of(this, BasiliskSaves.GAZE).source(player).tags("petrify")
                .label("save.dndclasses.basilisk_reflection").roll();
        DnDClasses.LOGGER.info("[Basilisk] {} reflects its gaze back with a shield: {}", player.getEntityName(),
                save.succeeded() ? "it resists" : "it turns to stone");
        if (save.failed()) {
            Conditions.of(this, Condition.PETRIFIED).sourceName(Text.translatable("entity.dndclasses.basilisk.reflection"))
                    .key("reflection").ticks(Conditions.INDEFINITE).apply();
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(Text.translatable("entity.dndclasses.basilisk.reflected", this.getDisplayName())
                        .formatted(Formatting.GOLD), true);
            }
            this.world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_STONE_PLACE,
                    SoundCategory.HOSTILE, 1.5F, 0.6F);
        }
    }

    /** Other creatures within range that face it (and can see it) save or become statues. */
    private void gazeAtMobs(ServerWorld world) {
        for (LivingEntity mob : world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(GAZE_RANGE),
                e -> !(e instanceof PlayerEntity) && !(e instanceof BasiliskEntity) && !(e instanceof ArmorStandEntity)
                        && e.isAlive() && e.squaredDistanceTo(this) <= GAZE_RANGE * GAZE_RANGE)) {
            if (alreadyPetrifying(mob) || isBlind(mob) || Conditions.isImmune(mob, Condition.PETRIFIED, this)
                    || !this.isLookedAtBy(mob, GAZE_ANGLE) || !mob.canSee(this)) {
                continue;
            }
            MobActions.log(this, "gaze", mob);
            if (SavingThrow.of(mob, BasiliskSaves.GAZE).source(this).tags("petrify").roll().failed()) {
                Conditions.of(mob, Condition.PETRIFIED).source(this).key(BasiliskSaves.PETRIFYING_KEY)
                        .ticks(Conditions.INDEFINITE).apply();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isClient && this.isAlive() && this.age % GAZE_INTERVAL == 0 && Conditions.canAct(this)) {
            this.tickGaze((ServerWorld) this.world);
        }
    }

    // ---- Sounds ----

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_HOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_TURTLE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_TURTLE_SHAMBLE, 0.4F, 0.7F);
    }

    @Override
    public float getSoundPitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 0.6F;
    }

    // ---- Animation ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::predicate));
        AnimationController<BasiliskEntity> action = new AnimationController<>(this, "action", 0,
                state -> PlayState.STOP);
        action.triggerableAnim(GAZE_ANIM, RawAnimation.begin().thenPlay(GAZE_ANIM));
        action.triggerableAnim(BITE_ANIM, RawAnimation.begin().thenPlay(BITE_ANIM));
        controllers.add(action);
    }

    private PlayState predicate(AnimationState<BasiliskEntity> state) {
        if (Conditions.has(this, Condition.PETRIFIED)) {
            return state.setAndContinue(STATUE);
        }
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
