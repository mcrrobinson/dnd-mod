package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.entity.ai.goal.FireBreathGoal;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.event.GameEvent;
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
    /** Chance per raw fish that a wild one is tamed, like a wolf and its bones. */
    private static final int TAME_CHANCE = 3;
    /** Health a tamed one gets back from each raw fish. */
    private static final float FISH_HEAL = 4.0F;

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
        // Wild ones keep to themselves but the group fights back when one is hit
        this.targetSelector.add(3, new RevengeGoal(this).setGroupRevenge());
    }

    /** A river dragon: tamed and healed with raw fish. */
    public static boolean isTamingItem(ItemStack stack) {
        return stack.isOf(Items.COD) || stack.isOf(Items.SALMON) || stack.isOf(Items.TROPICAL_FISH);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (this.world.isClient) {
            boolean handled = this.isOwner(player) || this.isTamed() || isTamingItem(stack) && !this.isTamed();
            return handled ? ActionResult.CONSUME : ActionResult.PASS;
        }
        if (this.isTamed()) {
            if (isTamingItem(stack) && this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                this.heal(FISH_HEAL);
                this.emitGameEvent(GameEvent.EAT, this);
                return ActionResult.SUCCESS;
            }
            if (this.isOwner(player) && stack.isEmpty()) {
                this.setSitting(!this.isSitting());
                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
                return ActionResult.SUCCESS;
            }
            return super.interactMob(player, hand);
        }
        if (isTamingItem(stack)) {
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            if (this.random.nextInt(TAME_CHANCE) == 0) {
                this.setOwner(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setSitting(true);
                this.world.sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
            } else {
                this.world.sendEntityStatus(this, EntityStatuses.ADD_NEGATIVE_PLAYER_REACTION_PARTICLES);
            }
            return ActionResult.SUCCESS;
        }
        return super.interactMob(player, hand);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        // Stands up to defend itself, like a wolf
        if (!this.world.isClient) {
            this.setSitting(false);
        }
        return super.damage(source, amount);
    }

    /** Never turns on its owner, even when the owner hits it. */
    @Override
    public boolean canTarget(LivingEntity target) {
        return !this.isOwner(target) && super.canTarget(target);
    }

    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        if (target instanceof CreeperEntity || target instanceof GhastEntity) {
            return false;
        }
        if (target instanceof TameableEntity tameable) {
            return !tameable.isTamed() || tameable.getOwner() != owner;
        }
        if (target instanceof PlayerEntity player && owner instanceof PlayerEntity ownerPlayer
                && !ownerPlayer.shouldDamagePlayer(player)) {
            return false;
        }
        return !(target instanceof AbstractHorseEntity horse && horse.isTame());
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isTamed() || this.isLeashed();
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
        if (this.isInSittingPose()) {
            return tAnimationState.setAndContinue(RawAnimation.begin().thenLoop("sit"));
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
