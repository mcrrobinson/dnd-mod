package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.entity.ai.goal.LightningChaserAttackGoal;
import mattonfire.dnd.entity.ai.goal.LightningChaserFlyRandomlyGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LightningChaserEntity extends TameableEntity implements GeoEntity, MultipartDragon, FireBreather {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String currentFlyAnimation = "fly.idle";
    private int flyAnimationTimer = 0;
    private int ticksSinceLastGround = 0;
    private static final int FLY_ANIMATION_DURATION = 20; // ticks

    // The model's wingspan is ~18 blocks but the entity hitbox is only 1.5, so wings, neck, head and
    // tail get their own hittable parts, each wrapping a group of model bones. Root bone sits 4.25px
    // higher on the ground and 18.75px lower in flight (lightning_chaser.animation.json).
    private static final DragonPartLayout PART_LAYOUT = new DragonPartLayout("lightning_chaser", 4.25 / 16.0, -18.75 / 16.0)
            .part("body", "front", "back")
            .part("neck_base", "neck1")
            .part("neck_mid", "neck2")
            .part("neck_top", "neck3")
            .part("head", "head", "jaw")
            .part("tail_base", "tail1")
            .part("tail_mid", "tail2")
            .part("tail_tip", "tail3", "tail4")
            .pair("wing", "wing_left", "membrane_shoulder_left", "membrane1_elbow_left", "membrane2_elbow_left")
            .pair("wing_arm", "shoulder_arm_left", "membrane_shoulder_arm_left", "fingers_left", "wing_finger4_left", "wing_finger5_left")
            .pair("wing_finger1", "wing_finger1_left", "membrane_wing_finger1_left")
            .pair("wing_finger2", "wing_finger2_left", "membrane_wing_finger2_left")
            .pair("wing_finger3", "wing_finger3_left", "membrane_wing_finger3_left")
            // Legs hang below the main hitbox in flight
            .pair("leg", "leg_left", "leg_mid_left", "leg_ground_left", "feet_left",
                    "feet_finger1_left", "feet_finger2_left", "feet_finger3_left", "feet_finger4_left");
    private final DragonPart[] parts;
    private static final TrackedData<Integer> BREATH_TICKS = DataTracker.registerData(LightningChaserEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Vector3f> BREATH_AIM = DataTracker.registerData(LightningChaserEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private final FireBreath fireBreath;

    private static final double BOSS_BAR_RANGE = 64.0;
    private final ServerBossBar bossBar = (ServerBossBar) new ServerBossBar(this.getDisplayName(),
            BossBar.Color.YELLOW, BossBar.Style.PROGRESS).setDragonMusic(true);

    /** How far the storm's extra bolts land from the target. */
    private static final double STORM_SPREAD = 3.0;

    // Lightning Chasers live in lairs on the mountain peaks (see DragonLairStructure) and keep
    // circling back to them.
    @Nullable
    private BlockPos lair;

    public LightningChaserEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, false);
        this.parts = PART_LAYOUT.createParts(this);
        this.fireBreath = new FireBreath(this, PART_LAYOUT.part(this.parts, "head"), BREATH_TICKS, BREATH_AIM, 12.0, 4.0F);
        this.setId(DragonPartLayout.reserveIds(this.parts));
        this.experiencePoints = 80;
    }

    @Nullable
    public BlockPos getLair() {
        return this.lair;
    }

    public void setLair(@Nullable BlockPos lair) {
        this.lair = lair;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.lair != null) {
            nbt.put("Lair", NbtHelper.fromBlockPos(this.lair));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.lair = nbt.contains("Lair") ? NbtHelper.toBlockPos(nbt.getCompound("Lair")) : null;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        FireBreath.track(this.dataTracker, BREATH_TICKS, BREATH_AIM);
    }

    @Override
    public FireBreath getFireBreath() {
        return this.fireBreath;
    }

    @Override
    public DragonPart[] getParts() {
        return this.parts;
    }

    @Override
    public DragonPartLayout getPartLayout() {
        return PART_LAYOUT;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        DragonPartLayout.assignIds(this.parts, id);
    }

    private boolean isFlying() {
        return !this.isOnGround() && this.ticksSinceLastGround > 5;
    }

    public void switchToFlightMode() {
        this.moveControl = new FlightMoveControl(this, 10, false);
        BirdNavigation birdNav = new BirdNavigation(this, this.world);
        birdNav.setCanPathThroughDoors(false);
        birdNav.setCanSwim(true);
        this.navigation = birdNav;
    }

    @Override
    public void tick() {
        super.tick();
        PART_LAYOUT.update(this, this.parts, this.isFlying());
        if (this.isOnGround()) {
            this.ticksSinceLastGround = 0;
            // Use ground movement control when on ground
            if (!(this.moveControl instanceof MoveControl)) {
                this.moveControl = new MoveControl(this);
            }
            // Use ground navigation when on ground
            if (!(this.getNavigation() instanceof MobNavigation)) {
                this.navigation = new MobNavigation(this, this.world);
            }
        } else {
            this.ticksSinceLastGround++;
            // Use flight movement control when in air
            if (!(this.moveControl instanceof FlightMoveControl)) {
                this.moveControl = new FlightMoveControl(this, 10, false);
            }
            // Use bird navigation when in air
            if (!(this.getNavigation() instanceof BirdNavigation)) {
                BirdNavigation birdNav = new BirdNavigation(this, this.world);
                birdNav.setCanPathThroughDoors(false);
                birdNav.setCanSwim(true);
                this.navigation = birdNav;
            }
        }

        if (!this.world.isClient && !this.isOnGround() && this.getVelocity().y < 0.0D) {
            this.setVelocity(this.getVelocity().multiply(1.0D, 0.6D, 1.0D));
        }

        if (!this.world.isClient) {
            this.updateBossBar();
        }
        this.fireBreath.tick();
    }

    private boolean isFightingPlayer() {
        return this.isAlive() && !this.isTamed()
                && (this.getTarget() instanceof PlayerEntity || this.getAttacker() instanceof PlayerEntity);
    }

    private boolean inBossBarRange(ServerPlayerEntity player) {
        return player.world == this.world && player.isAlive() && !player.isSpectator()
                && player.squaredDistanceTo(this) < BOSS_BAR_RANGE * BOSS_BAR_RANGE;
    }

    private void updateBossBar() {
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        this.bossBar.setName(this.getDisplayName());

        boolean fighting = this.isFightingPlayer();
        for (ServerPlayerEntity player : java.util.List.copyOf(this.bossBar.getPlayers())) {
            if (!fighting || !this.inBossBarRange(player)) {
                this.bossBar.removePlayer(player);
            }
        }
        if (fighting) {
            for (ServerPlayerEntity player : ((ServerWorld) this.world).getPlayers(this::inBossBarRange)) {
                this.bossBar.addPlayer(player);
            }
        }
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossBar.clearPlayers();
    }

    // It calls the storm down itself, so its own lightning (and anyone else's) doesn't hurt it.
    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
    }

    // Flying creature: landing after a flight (or a dive) shouldn't hurt it
    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, net.minecraft.entity.damage.DamageSource damageSource) {
        return false;
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation birdNavigation = new BirdNavigation(this, world);
        birdNavigation.setCanPathThroughDoors(false);
        birdNavigation.setCanSwim(true);
        return birdNavigation;
    }

    @Override
    public LivingEntity getOwner() {
        UUID uuid = this.getOwnerUuid();
        return uuid == null ? null : this.world.getPlayerByUuid(uuid);
    }

    @Override
    public net.minecraft.world.EntityView method_48926() {
        return this.world;
    }

    public static DefaultAttributeContainer.Builder createLightningChaserAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 14.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 12.0D)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 6.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new LightningChaserAttackGoal(this));
        this.goalSelector.add(3, new LightningChaserFlyRandomlyGoal(this));
        this.goalSelector.add(4, new WanderAroundGoal(this, 1.0D));
        this.goalSelector.add(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /** Calls a storm down on the target: one bolt on it and two more close by. */
    public void shoot(LivingEntity target) {
        if (this.world.isClient) {
            return;
        }
        this.strike(target.getX(), target.getY(), target.getZ());
        for (int i = 0; i < 2; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 1.5D + this.random.nextDouble() * STORM_SPREAD;
            this.strike(target.getX() + Math.cos(angle) * distance, target.getY(), target.getZ() + Math.sin(angle) * distance);
        }
    }

    private void strike(double x, double y, double z) {
        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(this.world);
        if (lightning != null) {
            lightning.refreshPositionAfterTeleport(x, y, z);
            this.world.spawnEntity(lightning);
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null; // Not breedable by default as per request
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<LightningChaserEntity> controller = new AnimationController<>(this, "controller", 0, this::predicate);
        controller.setSoundKeyframeHandler(event -> {});
        controllers.add(controller);
        controllers.add(this.fireBreath.createAnimationController(this));
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> tAnimationState) {
        boolean isFlying = this.isFlying(); // Must be airborne for >5 ticks to be considered flying

        if (isFlying) {
            String desiredAnimation = tAnimationState.isMoving() ? "fly.straight" : "fly.idle";
            if (!desiredAnimation.equals(currentFlyAnimation)) {
                flyAnimationTimer++;
                if (flyAnimationTimer >= FLY_ANIMATION_DURATION) {
                    currentFlyAnimation = desiredAnimation;
                    flyAnimationTimer = 0;
                }
            } else {
                flyAnimationTimer = 0;
            }
            return tAnimationState.setAndContinue(RawAnimation.begin().thenLoop(currentFlyAnimation));
        }
        if (tAnimationState.isMoving()) {
            return tAnimationState.setAndContinue(RawAnimation.begin().thenLoop("walk"));
        }
        return tAnimationState.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
