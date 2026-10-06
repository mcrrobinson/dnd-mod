package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.entity.ai.goal.WyvernAttackGoal;
import mattonfire.dnd.entity.ai.goal.WyvernFlyRandomlyGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WyvernEntity extends TameableEntity implements GeoEntity, MultipartDragon {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String currentFlyAnimation = "fly.idle";
    private int flyAnimationTimer = 0;
    private int ticksSinceLastGround = 0;
    private static final int FLY_ANIMATION_DURATION = 20; // ticks - reduced from 120 for responsiveness

    // The model is ~12 blocks wide but the entity hitbox is only 1.5, so wings, neck, head and tail
    // get their own hittable parts (like the ender dragon), each wrapping a group of model bones.
    // Root bone sits 11px lower on the ground and 34.5px lower in flight (wyvern.animation.json).
    private static final DragonPartLayout PART_LAYOUT = new DragonPartLayout("wyvern", -11 / 16.0, -34.5 / 16.0)
            .part("body", "front", "back")
            .part("neck_base", "neck1", "neck2")
            .part("neck_mid", "neck3", "neck4")
            .part("neck_top", "neck5")
            .part("head", "head", "jaw_low")
            .part("tail_base", "tail1", "tail2")
            .part("tail_tip", "tail3", "tail4", "tail5")
            .pair("wing", "wing_left", "membrane_shoulder_left", "wing_wart_left", "membrane_wart1_left", "membrane_wart2_left")
            .pair("wing_arm", "shoulder_arm_left", "membrane_shoulder_arm_left")
            .pair("wing_finger1", "wing_finger1_left", "membrane_wing_finger1_left")
            .pair("wing_finger2", "wing_finger2_left", "membrane_wing_finger2_left")
            .pair("wing_finger3", "wing_finger3_left", "membrane_wing_finger3_left", "wing_finger4_left")
            // Legs hang below the main hitbox in flight
            .pair("leg", "leg_left", "leg_mid_left", "leg_ground_left", "feet_left",
                    "feet_finger1_left", "feet_finger2_left", "feet_finger3_left", "feet_finger4_left");
    private final DragonPart[] parts;

    // Shown like the ender dragon's bar to players near a wild wyvern that is fighting a player.
    // The dragon music flag tells the client to play the dragon fight music (EventMusic).
    private static final double BOSS_BAR_RANGE = 64.0;
    private final ServerBossBar bossBar = (ServerBossBar) new ServerBossBar(this.getDisplayName(),
            BossBar.Color.RED, BossBar.Style.PROGRESS).setDragonMusic(true);

    public WyvernEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, false);
        this.parts = PART_LAYOUT.createParts(this);
        this.setId(DragonPartLayout.reserveIds(this.parts));
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
            if (!(this.moveControl instanceof net.minecraft.entity.ai.control.MoveControl)) {
                this.moveControl = new net.minecraft.entity.ai.control.MoveControl(this);
            }
            // Use ground navigation when on ground
            if (!(this.getNavigation() instanceof net.minecraft.entity.ai.pathing.MobNavigation)) {
                this.navigation = new net.minecraft.entity.ai.pathing.MobNavigation(this, this.world);
            }
        } else {
            this.ticksSinceLastGround++;
            // Use flight movement control when in air
            if (!(this.moveControl instanceof FlightMoveControl)) {
                this.moveControl = new FlightMoveControl(this, 10, false);
            }
            // Use bird navigation when in air
            if (!(this.getNavigation() instanceof net.minecraft.entity.ai.pathing.BirdNavigation)) {
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
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new WyvernAttackGoal(this));
        this.goalSelector.add(3, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.add(4, new WyvernFlyRandomlyGoal(this));
        this.goalSelector.add(5, new WanderAroundGoal(this, 1.0D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public void shoot(LivingEntity target) {
        if (!this.world.isClient) {
            double d = target.getX() - this.getX();
            double e = target.getBodyY(0.5) - this.getBodyY(0.5);
            double f = target.getZ() - this.getZ();
            
            SmallFireballEntity fireball = new SmallFireballEntity(this.world, this, d, e, f);
            fireball.setPosition(this.getX(), this.getEyeY(), this.getZ());
            this.world.spawnEntity(fireball);
            this.playSound(net.minecraft.sound.SoundEvents.ENTITY_BLAZE_SHOOT, 1.0F, 1.0F);
        }
    }

    public static DefaultAttributeContainer.Builder createWyvernAttributes() {
        return TameableEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6);
    }

    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public net.minecraft.world.EntityView method_48926() {
        return this.world;
    }

    @Override
    public java.util.UUID getOwnerUuid() {
        return super.getOwnerUuid();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        AnimationController<WyvernEntity> controller = new AnimationController<>(this, "controller", 0, event -> {
            boolean isFlying = this.isFlying(); // Must be airborne for >5 ticks (0.25s) to be considered flying

            if (isFlying) {
                String desiredAnimation = event.isMoving() ? "fly.straight" : "fly.idle";
                if (!desiredAnimation.equals(currentFlyAnimation)) {
                    flyAnimationTimer++;
                    if (flyAnimationTimer >= FLY_ANIMATION_DURATION) {
                        currentFlyAnimation = desiredAnimation;
                        flyAnimationTimer = 0;
                    }
                } else {
                    flyAnimationTimer = 0;
                }
                return event.setAndContinue(RawAnimation.begin().thenLoop(currentFlyAnimation));
            }
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        });
        controller.setSoundKeyframeHandler(event -> {
        });
        controllerRegistrar.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
