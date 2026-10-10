package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.entity.ai.goal.LairDragonAttackGoal;
import mattonfire.dnd.entity.ai.goal.LairDragonFlyRandomlyGoal;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.boss.BossFight;
import mattonfire.dnd.world.gen.lair.LairPiece;
import mattonfire.dnd.world.gen.lair.LairRespawns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A boss dragon that guards a lair on a mountain summit (see DragonLairStructure): the Lightning
 * Chaser and the Frost Drake. They share the Lightning Chaser's model, hit shapes, stats, flight
 * and attack goals; each brings its own breath ({@link #createBreath}), storm ({@link #shoot}), boss
 * bar colour and lair structure.
 */
public abstract class LairDragonEntity extends TameableEntity implements GeoEntity, MultipartDragon, FireBreather, Boss {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int ticksSinceLastGround = 0;

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
                    "feet_finger1_left", "feet_finger2_left", "feet_finger3_left", "feet_finger4_left")
            // Server-side parts follow these, in step with the client (see DragonFlightAnimation)
            .animations(DragonFlightAnimation.NAMES);
    private final DragonPart[] parts;
    /** DEX save DC against the breath (Lightning Chaser and Frost Drake). */
    public static final int BREATH_SAVE_DC = 14;
    private static final TrackedData<Integer> BREATH_TICKS = DataTracker.registerData(LairDragonEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Vector3f> BREATH_AIM = DataTracker.registerData(LairDragonEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Byte> BODY_ANIMATION = DataTracker.registerData(LairDragonEntity.class, TrackedDataHandlerRegistry.BYTE);
    private final DragonFlightAnimation bodyAnimation = new DragonFlightAnimation(this, BODY_ANIMATION);
    private final FireBreath fireBreath;

    private final BossFight bossFight;

    /** How far the storm's extra strikes land from the target (1.5 blocks plus up to this). */
    protected static final double STORM_SPREAD = 3.0;

    // They live in lairs on the mountain peaks (see DragonLairStructure) and keep circling back to them.
    @Nullable
    private BlockPos lair;

    protected LairDragonEntity(EntityType<? extends TameableEntity> entityType, World world, BossBar.Color bossBarColor) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, false);
        this.parts = PART_LAYOUT.createParts(this);
        this.fireBreath = this.createBreath(PART_LAYOUT.part(this.parts, "head"), BREATH_TICKS, BREATH_AIM);
        this.setId(DragonPartLayout.reserveIds(this.parts));
        this.bossFight = new BossFight(this, bossBarColor, BossBar.Style.PROGRESS)
                .range(64.0)
                .activeWhen(() -> !this.isTamed())
                .music(ModSounds.MUSIC_DRAGON_FIGHT)
                .xp(80);
    }

    /** This dragon's breath. Called from the constructor, before the subclass's own fields are set. */
    protected abstract FireBreath createBreath(DragonPart head, TrackedData<Integer> ticksKey, TrackedData<Vector3f> aimKey);

    /** The structure this dragon's lair belongs to. */
    protected abstract RegistryKey<Structure> lairStructure();

    /** Calls its storm down on the target (lightning or hail): its ranged attack, in turn with the breath. */
    public abstract void shoot(LivingEntity target);

    @Nullable
    public BlockPos getLair() {
        return this.lair;
    }

    public void setLair(@Nullable BlockPos lair) {
        this.lair = lair;
    }

    /** The centre of the {@code structure} lair containing {@code pos}, or null outside one. */
    @Nullable
    public static BlockPos findLair(ServerWorld world, BlockPos pos, RegistryKey<Structure> structure) {
        StructureStart start = world.getStructureAccessor().getStructureContaining(pos, structure);
        if (start.hasChildren()) {
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof LairPiece lairPiece) {
                    return lairPiece.getCenter();
                }
            }
        }
        return null;
    }

    // The lair's first dragons get their lair from LairPiece; the ones its spawn_overrides send later
    // (and any summoned inside one) look it up here, so they circle back to it too.
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        if (this.lair == null && spawnReason != SpawnReason.STRUCTURE) {
            this.lair = findLair(world.toServerWorld(), this.getBlockPos(), this.lairStructure());
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.bossFight.writeNbt(nbt);
        if (this.lair != null) {
            nbt.put("Lair", NbtHelper.fromBlockPos(this.lair));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.bossFight.readNbt(nbt);
        this.lair = nbt.contains("Lair") ? NbtHelper.toBlockPos(nbt.getCompound("Lair")) : null;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        FireBreath.track(this.dataTracker, BREATH_TICKS, BREATH_AIM);
        DragonFlightAnimation.track(this.dataTracker, BODY_ANIMATION);
    }

    @Override
    public int getBreathSaveDc() {
        return BREATH_SAVE_DC;
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

    @Override
    public void tick() {
        super.tick();
        this.bodyAnimation.tick(this.isFlying());
        PART_LAYOUT.update(this, this.parts, this.bodyAnimation.current(), this.isFlying());
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
        // The lair waits a while before it sends a new one (see ModSpawns)
        if (this.world instanceof ServerWorld serverWorld && this.lair != null && !this.isTamed()) {
            LairRespawns.get(serverWorld).onChaserKilled(this.lair, serverWorld.getTime());
        }
    }

    // Flying creature: landing after a flight (or a dive) shouldn't hurt it
    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
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

    public static DefaultAttributeContainer.Builder createLairDragonAttributes() {
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
        this.goalSelector.add(2, new LairDragonAttackGoal(this));
        this.goalSelector.add(3, new LairDragonFlyRandomlyGoal(this));
        this.goalSelector.add(4, new WanderAroundGoal(this, 1.0D));
        this.goalSelector.add(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this));
        // Wild ones hunt players; tamed ones leave everyone alone unless the owner fights them
        this.targetSelector.add(4, new UntamedActiveTargetGoal<>(this, PlayerEntity.class, true, null));
    }

    /** Whether a strike of its storm hurts {@code e}: never itself, its friends or its own kind. */
    protected boolean isStormVictim(LivingEntity e) {
        return e.isAlive() && e != this && !TamedDragons.isFriend(this, e) && e.getType() != this.getType();
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return !TamedDragons.isFriend(this, target) && super.canTarget(target);
    }

    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        return TamedDragons.canAttackWithOwner(target, owner);
    }

    // Wild ones leave in peaceful, like monsters (the lair sends a new one when you switch back)
    @Override
    protected boolean isDisallowedInPeaceful() {
        return !this.isTamed();
    }

    // Can't breed, so wheat shouldn't put it in love mode (and use the wheat up)
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    // AnimalEntity gives 1-3 XP whatever experiencePoints says; this is the 80 from BossFight.xp
    @Override
    public int getXpToDrop() {
        return this.experiencePoints;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<LairDragonEntity> controller = this.bodyAnimation.createController(this);
        controller.setSoundKeyframeHandler(event -> {});
        controllers.add(controller);
        controllers.add(this.fireBreath.createAnimationController(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
