package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.entity.ai.goal.LightningChaserAttackGoal;
import mattonfire.dnd.entity.ai.goal.LightningChaserFlyRandomlyGoal;
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
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

public class LightningChaserEntity extends TameableEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String currentFlyAnimation = "fly.idle";
    private int flyAnimationTimer = 0;
    private int ticksSinceLastGround = 0;
    private static final int FLY_ANIMATION_DURATION = 20; // ticks

    public LightningChaserEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, false);
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
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D);
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

    public void shoot(LivingEntity target) {
        if (!this.world.isClient) {
            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(this.world);
            if (lightning != null) {
                lightning.refreshPositionAfterTeleport(target.getX(), target.getY(), target.getZ());
                this.world.spawnEntity(lightning);
            }
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
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> tAnimationState) {
        boolean isFlying = !this.isOnGround() && this.ticksSinceLastGround > 5; // Must be airborne for >5 ticks to be considered flying

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
