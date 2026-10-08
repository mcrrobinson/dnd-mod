package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.entity.ai.goal.WyvernAttackGoal;
import mattonfire.dnd.entity.ai.goal.WyvernFlyRandomlyGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.boss.BossFight;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WyvernEntity extends TameableEntity implements GeoEntity, MultipartDragon, FireBreather, Boss {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String currentFlyAnimation = "fly.idle";
    private int flyAnimationTimer = 0;
    private int ticksSinceLastGround = 0;
    private static final int FLY_ANIMATION_DURATION = 20; // ticks

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
    private static final TrackedData<Integer> BREATH_TICKS = DataTracker.registerData(WyvernEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Vector3f> BREATH_AIM = DataTracker.registerData(WyvernEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private final FireBreath fireBreath;

    // Boss bar and fight music for players near a wild wyvern that is fighting a player.
    private final BossFight bossFight;

    public WyvernEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, false);
        this.parts = PART_LAYOUT.createParts(this);
        this.fireBreath = new FireBreath(this, PART_LAYOUT.part(this.parts, "head"), BREATH_TICKS, BREATH_AIM, 12.0, 4.0F);
        this.setId(DragonPartLayout.reserveIds(this.parts));
        this.bossFight = new BossFight(this, BossBar.Color.RED, BossBar.Style.PROGRESS)
                .range(64.0)
                .activeWhen(() -> this.hasBossFight() && !this.isTamed())
                .music(ModSounds.MUSIC_DRAGON_FIGHT);
        // Dragon Slayer comes from the advancement's player_killed_entity criterion (#dndclasses:dragons).
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        FireBreath.track(this.dataTracker, BREATH_TICKS, BREATH_AIM);
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

    @Override
    public void tick() {
        super.tick();
        PART_LAYOUT.update(this, this.parts, this.isFlying());
        // One BirdNavigation and FlightMoveControl for both ground and air (like the parrot), so the
        // current path survives every landing and take-off.
        if (this.isOnGround()) {
            this.ticksSinceLastGround = 0;
        } else {
            this.ticksSinceLastGround++;
        }

        if (!this.world.isClient && !this.isOnGround() && this.getVelocity().y < 0.0D) {
            this.setVelocity(this.getVelocity().multiply(1.0D, 0.6D, 1.0D));
        }

        this.bossFight.tick();
        this.fireBreath.tick();
    }

    /** Whether a fight shows the boss bar and plays the fight music (not for the common Ember Wyvern). */
    protected boolean hasBossFight() {
        return true;
    }

    /** The model texture; variants override it. */
    public Identifier getTexture() {
        return new Identifier(DnDClasses.MOD_ID, "textures/entity/wyvern/green.png");
    }

    /** Ticks between bites. */
    public int getMeleeCooldown() {
        return 20;
    }

    /** Ticks after a fire breath ends before the next attack. */
    public int getBreathCooldown() {
        return 60;
    }

    @Override
    public FireBreath getFireBreath() {
        return this.fireBreath;
    }

    @Override
    public BossFight getBossFight() {
        return this.bossFight;
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

    @Override
    public void onDeath(DamageSource source) {
        this.bossFight.onDeath();
        super.onDeath(source);
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
        // Wild ones hunt players; tamed ones leave everyone alone unless the owner fights them
        this.targetSelector.add(4, new UntamedActiveTargetGoal<>(this, PlayerEntity.class, true, null));
    }

    public static DefaultAttributeContainer.Builder createWyvernAttributes() {
        return TameableEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
                // tryAttack reads it; without it the server crashes on the first melee hit
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return !TamedDragons.isFriend(this, target) && super.canTarget(target);
    }

    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        return TamedDragons.canAttackWithOwner(target, owner);
    }

    // Wild ones leave in peaceful, like monsters (they'd still start boss fights there)
    @Override
    protected boolean isDisallowedInPeaceful() {
        return !this.isTamed();
    }

    // Can't breed, so wheat shouldn't put it in love mode (and use the wheat up)
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    // AnimalEntity gives 1-3 XP whatever experiencePoints says
    @Override
    public int getXpToDrop() {
        return 20;
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
        controllerRegistrar.add(this.fireBreath.createAnimationController(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
