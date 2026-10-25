package mattonfire.dnd.entity;

import java.util.EnumSet;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.ConditionInstance;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.boss.BossFight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
 * The Medusa, lair boss of the Gorgon's Temple (1 in 3 desert pyramids and jungle temples, see
 * {@link mattonfire.dnd.entity.boss.StructureBosses#placeMedusa}).
 *
 * <ul>
 *   <li><b>Petrifying Gaze</b> ({@link #gaze}): every {@value #GAZE_INTERVAL} ticks ({@value #ENRAGED_GAZE_INTERVAL}
 *   enraged), each player within {@value #GAZE_RANGE} blocks who meets her eyes makes a CON save; a failure starts
 *   them turning to stone (Restrained with a repeat save that escalates to Petrified), a failure by 5 or more is
 *   Petrified at once. Looking away avoids it; a raised shield turns the gaze back on her (Stunned, 4 damage).
 *   Mobs that face her save too and become statues.</li>
 *   <li><b>Snake hair</b>: a bite within 3 blocks, then a CON save or Poisoned.</li>
 *   <li><b>Longbow</b>: volleys of 3 arrows (5 enraged) from 6-24 blocks. She keeps 8-14 blocks away.</li>
 * </ul>
 * The gaze runs from {@link #tick()}, not a goal, so it works on a {@code NoAI} Medusa too (tests).
 */
public class MedusaEntity extends HostileEntity implements GeoEntity, Boss, RangedAttackMob {
    public static final Identifier LOOT_TABLE = new Identifier("dndclasses", "entities/medusa");
    /** Command tag on a Medusa placed in a Gorgon's Temple. */
    public static final String LAIR_TAG = "dndclasses.medusa_lair";
    public static final int XP = 80;

    public static final int GAZE_INTERVAL = 20;
    public static final int ENRAGED_GAZE_INTERVAL = 10;
    public static final double GAZE_RANGE = 24.0D;
    public static final double MOB_GAZE_RANGE = 16.0D;
    /** A player meets her gaze when they look within this many degrees of her eyes... */
    public static final double MEET_ANGLE = 30.0D;
    /** ...and she faces them within this many degrees. Also how squarely a shield has to face her. */
    public static final double FACE_ANGLE = 60.0D;
    /** One reflection per this many ticks (her boss Stun cap and shrug-off match it). */
    public static final int REFLECT_COOLDOWN = 200;
    public static final int REFLECT_STUN_TICKS = 20;
    public static final float REFLECT_DAMAGE = 4.0F;

    public static final double BITE_RANGE = 3.0D;
    public static final int BITE_COOLDOWN = 30;
    public static final double VOLLEY_MIN = 6.0D;
    public static final double VOLLEY_MAX = 24.0D;
    public static final int VOLLEY_COOLDOWN = 60;
    public static final int ENRAGED_VOLLEY_COOLDOWN = 45;
    public static final int VOLLEY_ARROWS = 3;
    public static final int ENRAGED_VOLLEY_ARROWS = 5;
    /** Total spread of a volley, in degrees. */
    public static final float VOLLEY_SPREAD = 10.0F;
    /** Arrow speed 1.6 x damage 2.5 = 4 damage per arrow. */
    private static final float ARROW_SPEED = 1.6F;
    private static final double ARROW_DAMAGE = 2.5D;
    private static final int DRAW_TICKS = 15;

    public static final double KEEP_MIN = 8.0D;
    public static final double KEEP_MAX = 14.0D;
    /** She leashes this close to her lair. */
    public static final double LEASH = 24.0D;
    private static final double SPEECH_RANGE = 32.0D;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation GAZE = RawAnimation.begin().thenPlay("gaze");
    private static final RawAnimation BOW_DRAW = RawAnimation.begin().thenPlay("bow_draw");
    private static final RawAnimation BOW_RELEASE = RawAnimation.begin().thenPlay("bow_release");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("bite");
    private static final RawAnimation REEL = RawAnimation.begin().thenPlay("reel");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final BossFight bossFight;
    @Nullable
    private BlockPos home;
    private long nextReflect;
    private long nextBite;
    private long nextVolley;

    public MedusaEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.bossFight = new BossFight(this, BossBar.Color.GREEN, BossBar.Style.NOTCHED_10)
                .range(SPEECH_RANGE)
                .phase(0.5F, BossBar.Color.RED, this::enrage)
                .lootTable(LOOT_TABLE)
                .xp(XP);
    }

    public static DefaultAttributeContainer.Builder createMedusaAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 140.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 2.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    // ---------------------------------------------------------------- goals

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new CombatGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    public boolean isEnraged() {
        return this.bossFight.getPhase() >= 1;
    }

    public int gazeDc() {
        return this.isEnraged() ? MedusaSaves.ENRAGED_GAZE_DC : MedusaSaves.GAZE_DC;
    }

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    /** Her lair: she keeps within {@value #LEASH} blocks of it. */
    public void setHome(BlockPos home) {
        this.home = home.toImmutable();
        this.setPositionTarget(this.home, (int) KEEP_MAX);
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        super.tick();
        this.bossFight.tick();
        if (this.world instanceof ServerWorld world && this.isAlive()) {
            if (this.home == null) {
                this.setHome(this.getBlockPos());
            }
            int interval = this.isEnraged() ? ENRAGED_GAZE_INTERVAL : GAZE_INTERVAL;
            if (this.age % interval == 0 && Conditions.canAct(this)) {
                this.gaze(world);
            }
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.home != null && this.age % 20 == 0
                && Math.sqrt(this.getBlockPos().getSquaredDistance(this.home)) > LEASH) {
            // Lured too far from her temple: drop the chase and walk home.
            this.setTarget(null);
            this.getNavigation().startMovingTo(this.home.getX() + 0.5D, this.home.getY(), this.home.getZ() + 0.5D, 1.1D);
        }
    }

    private void enrage() {
        this.broadcast(Text.translatable("chat.dndclasses.medusa.enrage", this.getDisplayName())
                .formatted(Formatting.DARK_GREEN, Formatting.ITALIC));
        this.playSound(SoundEvents.ENTITY_CAT_HISS, 2.0F, 0.6F);
        MobActions.log(this, "enrage", this.getTarget());
    }

    private void broadcast(Text message) {
        if (this.world instanceof ServerWorld world) {
            for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(this) < SPEECH_RANGE * SPEECH_RANGE)) {
                player.sendMessage(message, false);
            }
        }
    }

    // ---------------------------------------------------------------- the gaze

    /** Her look direction (head yaw: what her face points at). */
    private Vec3d lookVec() {
        return Vec3d.fromPolar(this.getPitch(), this.getHeadYaw());
    }

    private static boolean within(Vec3d look, Vec3d to, double degrees) {
        return look.normalize().dotProduct(to.normalize()) >= Math.cos(Math.toRadians(degrees));
    }

    /** She faces {@code entity}: it's within {@value #FACE_ANGLE} degrees of where her face points. */
    public boolean faces(Entity entity) {
        return within(this.lookVec(), entity.getEyePos().subtract(this.getEyePos()), FACE_ANGLE);
    }

    /** {@code player} looks her in the eyes: their look is within {@value #MEET_ANGLE} degrees of her eyes. */
    public boolean meetsGaze(LivingEntity viewer) {
        return within(viewer.getRotationVec(1.0F), this.getEyePos().subtract(viewer.getEyePos()), MEET_ANGLE);
    }

    /** A raised shield turned toward her (within {@value #FACE_ANGLE} degrees). */
    public boolean shieldsFrom(PlayerEntity player) {
        return player.isBlocking()
                && within(player.getRotationVec(1.0F), this.getEyePos().subtract(player.getEyePos()), FACE_ANGLE);
    }

    private static boolean blind(LivingEntity entity) {
        return Conditions.has(entity, Condition.BLINDED) || entity.hasStatusEffect(StatusEffects.BLINDNESS);
    }

    /** Already turning to stone (or stone) from her gaze: no new save. */
    private boolean petrifying(LivingEntity entity) {
        if (Conditions.has(entity, Condition.PETRIFIED)) {
            return true;
        }
        for (ConditionInstance inst : Conditions.all(entity)) {
            if (inst.condition() == Condition.RESTRAINED && "petrifying".equals(inst.key())) {
                return true;
            }
        }
        return false;
    }

    /** One gaze check: every player and mob in range who meets her eyes. */
    void gaze(ServerWorld world) {
        boolean met = false;
        for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
                && p.squaredDistanceTo(this) <= GAZE_RANGE * GAZE_RANGE)) {
            if (!this.faces(player) || !this.canSee(player) || blind(player) || this.petrifying(player)) {
                continue;
            }
            if (this.shieldsFrom(player)) {
                met |= this.reflect(world, player);
                continue;
            }
            if (!this.meetsGaze(player)) {
                continue;
            }
            met |= this.gazeAt(player);
        }
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, this.getBoundingBox().expand(MOB_GAZE_RANGE),
                m -> m != this && m.isAlive() && !(m instanceof Boss) && !this.isTeammate(m))) {
            if (this.squaredDistanceTo(mob) > MOB_GAZE_RANGE * MOB_GAZE_RANGE || blind(mob) || this.petrifying(mob)
                    || !within(Vec3d.fromPolar(mob.getPitch(), mob.getHeadYaw()),
                            this.getEyePos().subtract(mob.getEyePos()), FACE_ANGLE)
                    || !this.canSee(mob) || Conditions.isImmune(mob, Condition.PETRIFIED, this)) {
                continue;
            }
            // Mobs roll silently with +1; a failure makes a statue around her lair.
            SaveResult save = SavingThrow.of(mob, MedusaSaves.GAZE).dc(this.gazeDc()).source(this)
                    .exposure(this, "gaze", MedusaSaves.GAZE_WINDOW).tags("magic", "petrify").roll();
            if (save.fresh() && save.failed()) {
                Conditions.of(mob, Condition.PETRIFIED).source(this).key("petrifying").ticks(Conditions.INDEFINITE)
                        .apply();
                MobActions.log(this, "gaze", mob);
                met = true;
            }
        }
        if (met) {
            this.triggerAnim("action", "gaze");
            this.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.0F, 1.5F);
        }
    }

    /** A player meets her eyes: a fresh CON save each {@value MedusaSaves#GAZE_WINDOW} ticks. */
    private boolean gazeAt(ServerPlayerEntity player) {
        int dc = this.gazeDc();
        SaveResult save = SavingThrow.of(player, MedusaSaves.GAZE).dc(dc).source(this)
                .exposure(this, "gaze", MedusaSaves.GAZE_WINDOW)
                .tags("magic", "petrify")
                .onSuccess(Text.translatable(MedusaSaves.GAZE.effectKey()))
                .onFailure(Text.translatable(MedusaSaves.GAZE.effectKey() + ".failure"))
                .roll();
        if (!save.fresh()) {
            return false;
        }
        MobActions.log(this, "gaze", player);
        player.sendMessage(Text.translatable("entity.dndclasses.medusa.gaze_warning").formatted(Formatting.DARK_GREEN),
                true);
        if (save.failed()) {
            if (save.roll().total() <= dc - MedusaSaves.FAIL_BY_PETRIFIES) {
                Conditions.of(player, Condition.PETRIFIED).source(this).key("petrifying")
                        .ticks(MedusaSaves.PETRIFIED_TICKS).apply();
            } else {
                Conditions.of(player, Condition.RESTRAINED).source(this).key("petrifying")
                        .ticks(Conditions.INDEFINITE)
                        .repeatSave(MedusaSaves.GAZE.ability(), dc, MedusaSaves.PETRIFY_REPEAT_TICKS)
                        .saveLabel("save.dndclasses.medusa_petrify")
                        .saveTags("magic", "petrify")
                        .escalateOnFailure(Condition.PETRIFIED, MedusaSaves.PETRIFIED_TICKS)
                        .apply();
            }
        }
        return true;
    }

    /** A shield raised at her turns the gaze back: Stunned and hurt, once per {@value #REFLECT_COOLDOWN} ticks. */
    private boolean reflect(ServerWorld world, ServerPlayerEntity player) {
        long now = world.getTime();
        if (now < this.nextReflect) {
            return false;
        }
        this.nextReflect = now + REFLECT_COOLDOWN;
        MobActions.log(this, "reflect", player);
        this.broadcast(Text.translatable("chat.dndclasses.medusa.reflect", this.getDisplayName())
                .formatted(Formatting.GREEN, Formatting.ITALIC));
        Conditions.of(this, Condition.STUNNED).source(player).key("reflection").ticks(REFLECT_STUN_TICKS).apply();
        this.damage(world.getDamageSources().indirectMagic(player, player), REFLECT_DAMAGE);
        this.triggerAnim("action", "reel");
        world.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getEyeY(), this.getZ(), 12, 0.3D, 0.2D, 0.3D,
                0.02D);
        return true;
    }

    // ---------------------------------------------------------------- snakes and bow

    /** Snake hair bite: 6 damage, then a CON save or Poisoned. */
    void bite(LivingEntity target) {
        this.triggerAnim("action", "bite");
        if (!this.tryAttack(target)) {
            return;
        }
        MobActions.log(this, "bite", target);
        SaveResult save = SavingThrow.of(target, MedusaSaves.SNAKES).source(this).tags("poison")
                .onSuccess(Text.translatable(MedusaSaves.SNAKES.effectKey()))
                .onFailure(Text.translatable(MedusaSaves.SNAKES.effectKey() + ".failure"))
                .roll();
        if (save.failed()) {
            Conditions.of(target, Condition.POISONED).source(this).key("snakes").ticks(MedusaSaves.SNAKE_POISON_TICKS)
                    .apply();
            // No source: the data-driven poison save would roll a second time for the same bite.
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, MedusaSaves.SNAKE_POISON_TICKS, 0));
        }
    }

    /** A volley: 3 arrows (5 enraged) fanned over {@value #VOLLEY_SPREAD} degrees. */
    @Override
    public void attack(LivingEntity target, float pullProgress) {
        int arrows = this.isEnraged() ? ENRAGED_VOLLEY_ARROWS : VOLLEY_ARROWS;
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        for (int i = 0; i < arrows; i++) {
            float offset = arrows == 1 ? 0.0F : -VOLLEY_SPREAD / 2.0F + VOLLEY_SPREAD * i / (arrows - 1);
            double rad = Math.toRadians(offset);
            double rx = dx * Math.cos(rad) - dz * Math.sin(rad);
            double rz = dx * Math.sin(rad) + dz * Math.cos(rad);
            ArrowEntity arrow = new ArrowEntity(this.world, this);
            double dy = target.getBodyY(0.3333333333333333D) - arrow.getY();
            arrow.setVelocity(rx, dy + flat * 0.2D, rz, ARROW_SPEED, 1.0F);
            arrow.setDamage(ARROW_DAMAGE);
            arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
            this.world.spawnEntity(arrow);
        }
        this.triggerAnim("action", "bow_release");
        this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 0.8F);
        MobActions.log(this, "volley", target);
    }

    /**
     * Fights at range: keeps {@value #KEEP_MIN}-{@value #KEEP_MAX} blocks away so the gaze and the bow matter,
     * draws and looses volleys, and bites anyone who sticks close.
     */
    static class CombatGoal extends Goal {
        private final MedusaEntity medusa;
        private int drawing = -1;
        private int closeTicks;

        CombatGoal(MedusaEntity medusa) {
            this.medusa = medusa;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.medusa.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            this.drawing = -1;
            this.closeTicks = 0;
            this.medusa.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.medusa.getTarget();
            if (target == null) {
                return;
            }
            long now = this.medusa.world.getTime();
            this.medusa.getLookControl().lookAt(target, 30.0F, 30.0F);
            double distance = this.medusa.distanceTo(target);
            boolean sees = this.medusa.getVisibilityCache().canSee(target);

            if (distance <= BITE_RANGE) {
                this.closeTicks++;
                if (now >= this.medusa.nextBite) {
                    this.medusa.nextBite = now + BITE_COOLDOWN;
                    this.medusa.bite(target);
                }
            } else {
                this.closeTicks = 0;
            }

            if (this.drawing >= 0) {
                if (--this.drawing == 0) {
                    this.drawing = -1;
                    if (sees && distance <= VOLLEY_MAX + 4.0D) {
                        this.medusa.attack(target, 1.0F);
                    }
                }
            } else if (sees && distance >= VOLLEY_MIN && distance <= VOLLEY_MAX && now >= this.medusa.nextVolley) {
                this.medusa.nextVolley = now + (this.medusa.isEnraged() ? ENRAGED_VOLLEY_COOLDOWN : VOLLEY_COOLDOWN);
                this.drawing = DRAW_TICKS;
                this.medusa.triggerAnim("action", "bow_draw");
            }

            if (this.medusa.age % 10 != 0) {
                return;
            }
            if (!sees || distance > KEEP_MAX) {
                this.medusa.getNavigation().startMovingTo(target, 1.0D);
            } else if (distance < BITE_RANGE + 1.0D || this.closeTicks > 40) {
                // They stick to her: close in and bite.
                this.medusa.getNavigation().startMovingTo(target, 1.1D);
            } else if (distance < KEEP_MIN) {
                Vec3d away = NoPenaltyTargeting.findFrom(this.medusa, 10, 4, target.getPos());
                if (away != null) {
                    this.medusa.getNavigation().startMovingTo(away.x, away.y, away.z, 1.15D);
                }
            } else {
                this.medusa.getNavigation().stop();
            }
        }
    }

    // ---------------------------------------------------------------- misc

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_CAT_HISS;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITCH_DEATH;
    }

    @Override
    public float getSoundPitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 1.2F;
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 160;
    }

    @Override
    public BossFight getBossFight() {
        return this.bossFight;
    }

    @Override
    protected Identifier getLootTableId() {
        return this.bossFight.getLootTable(super.getLootTableId());
    }

    @Override
    public void onDeath(DamageSource source) {
        if (!this.world.isClient && !this.dead && !this.isRemoved()) {
            this.bossFight.onDeath();
            if (this.world instanceof ServerWorld world) {
                world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, this.getX(), this.getBodyY(0.6D), this.getZ(), 30,
                        0.4D, 0.8D, 0.4D, 0.05D);
                world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_CAT_HISS,
                        SoundCategory.HOSTILE, 2.0F, 0.5F);
            }
        }
        super.onDeath(source);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.bossFight.writeNbt(nbt);
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.bossFight.readNbt(nbt);
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")));
        }
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossFight.onStoppedTrackingBy(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossFight.onRemoved();
    }

    // ---------------------------------------------------------------- animation

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::movementPredicate));
        controllers.add(new AnimationController<MedusaEntity>(this, "action", 0, state -> PlayState.STOP)
                .triggerableAnim("gaze", GAZE)
                .triggerableAnim("bow_draw", BOW_DRAW)
                .triggerableAnim("bow_release", BOW_RELEASE)
                .triggerableAnim("bite", BITE)
                .triggerableAnim("reel", REEL));
    }

    private PlayState movementPredicate(AnimationState<MedusaEntity> state) {
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
