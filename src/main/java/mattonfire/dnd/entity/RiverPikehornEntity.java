package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.entity.ai.goal.FireBreathGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

public class RiverPikehornEntity extends TameableEntity implements GeoEntity, MultipartDragon, FireBreather {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Head, tail and wings stick out past the 1-block hitbox; each part wraps a group of model bones.
    // It never flies, so there is no flight pose.
    private static final DragonPartLayout PART_LAYOUT = new DragonPartLayout("river_pikehorn", 0, 0)
            .part("body", "body_front", "body_back")
            .part("head", "neck", "head", "jaw")
            .part("tail", "tail1", "tail2")
            .pair("wing", "wing_left", "wing_membrane_left", "wing_cont_left");
    private final DragonPart[] parts;
    private static final TrackedData<Integer> BREATH_TICKS = DataTracker.registerData(RiverPikehornEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Vector3f> BREATH_AIM = DataTracker.registerData(RiverPikehornEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private final FireBreath fireBreath;

    public RiverPikehornEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.parts = PART_LAYOUT.createParts(this);
        this.fireBreath = new FireBreath(this, PART_LAYOUT.part(this.parts, "head"), BREATH_TICKS, BREATH_AIM, 7.0, 2.0F);
        this.setId(DragonPartLayout.reserveIds(this.parts));
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

    @Override
    public void tick() {
        super.tick();
        PART_LAYOUT.update(this, this.parts, false);
        this.fireBreath.tick();
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

    public static DefaultAttributeContainer.Builder createRiverPikehornAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new SitGoal(this));
        // Breathes fire from a few blocks away and bites up close
        this.goalSelector.add(3, new FireBreathGoal<>(this, 3.0));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.add(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<RiverPikehornEntity> controller = new AnimationController<>(this, "controller", 0, this::predicate);
        controller.setSoundKeyframeHandler(event -> {});
        controllers.add(controller);
        controllers.add(this.fireBreath.createAnimationController(this));
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> tAnimationState) {
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
