package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.entity.ai.goal.WyvernAttackGoal;
import mattonfire.dnd.entity.ai.goal.WyvernFlyRandomlyGoal;
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
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WyvernEntity extends TameableEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String currentFlyAnimation = "fly.idle";
    private int flyAnimationTimer = 0;
    private int ticksSinceLastGround = 0;
    private static final int FLY_ANIMATION_DURATION = 20; // ticks - reduced from 120 for responsiveness

    public WyvernEntity(EntityType<? extends TameableEntity> entityType, World world) {
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
            boolean isFlying = !this.isOnGround() && this.ticksSinceLastGround > 5; // Must be airborne for >5 ticks (0.25s) to be considered flying

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
