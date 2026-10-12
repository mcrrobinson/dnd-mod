package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.boss.BossFight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A tameable flying dragon with a breath weapon and a boss fight: what the Wyverns ({@link WyvernEntity})
 * and the lair dragons ({@link LairDragonEntity}) share. One BirdNavigation and FlightMoveControl for
 * ground and air, hit shapes from a {@link DragonPartLayout} that follow the flight animation
 * ({@link DragonFlightAnimation}), a breath ({@link #createBreath}), and a boss bar with the dragon
 * fight music while it's wild ({@link #isBossFightActive}).
 *
 * <p>The attack and wander goals ({@code DragonAttackGoal}, {@code DragonFlyRandomlyGoal}) read the
 * timings and the lair from here; each subclass picks its own goals, stats and XP.
 */
public abstract class FlyingDragonEntity extends TameableEntity implements GeoEntity, MultipartDragon, FireBreather, Boss {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int ticksSinceLastGround = 0;

    private final DragonPartLayout partLayout;
    private final DragonPart[] parts;
    private static final TrackedData<Integer> BREATH_TICKS = DataTracker.registerData(FlyingDragonEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Vector3f> BREATH_AIM = DataTracker.registerData(FlyingDragonEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Byte> BODY_ANIMATION = DataTracker.registerData(FlyingDragonEntity.class, TrackedDataHandlerRegistry.BYTE);
    private final DragonFlightAnimation bodyAnimation = new DragonFlightAnimation(this, BODY_ANIMATION);
    private final FireBreath fireBreath;

    // Boss bar and fight music for players near a wild dragon that is fighting a player.
    private final BossFight bossFight;

    /** {@code partLayout} makes the hit shapes; the breath comes out of its "head" part. */
    protected FlyingDragonEntity(EntityType<? extends TameableEntity> entityType, World world,
            DragonPartLayout partLayout, BossBar.Color bossBarColor) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, false);
        this.partLayout = partLayout;
        this.parts = partLayout.createParts(this);
        this.fireBreath = this.createBreath(partLayout.part(this.parts, "head"), BREATH_TICKS, BREATH_AIM);
        this.setId(DragonPartLayout.reserveIds(this.parts));
        this.bossFight = new BossFight(this, bossBarColor, BossBar.Style.PROGRESS)
                .range(64.0)
                .activeWhen(this::isBossFightActive)
                .music(ModSounds.MUSIC_DRAGON_FIGHT);
    }

    /** This dragon's breath. Called from the constructor, before the subclass's own fields are set. */
    protected abstract FireBreath createBreath(DragonPart head, TrackedData<Integer> ticksKey, TrackedData<Vector3f> aimKey);

    /** Whether a fight shows the boss bar and plays the fight music: while it's wild. */
    protected boolean isBossFightActive() {
        return !this.isTamed();
    }

    /** The lair it keeps circling back to, or null for a dragon without one. */
    @Nullable
    public BlockPos getLair() {
        return null;
    }

    /** Ticks between bites. */
    public int getMeleeCooldown() {
        return 20;
    }

    /** Bites inside this many blocks, breathes fire further out. */
    public double getBiteRange() {
        return 5.0;
    }

    /** Ticks after a ranged attack (for a breath, once it ends) before the next attack. */
    public int getBreathCooldown() {
        return 60;
    }

    /** Whether it has a storm ({@link #shoot}) to call down in turn with its breath. */
    public boolean hasStorm() {
        return false;
    }

    /** Calls its storm down on the target; nothing for a dragon without one ({@link #hasStorm}). */
    public void shoot(LivingEntity target) {
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        FireBreath.track(this.dataTracker, BREATH_TICKS, BREATH_AIM);
        DragonFlightAnimation.track(this.dataTracker, BODY_ANIMATION);
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
        return this.partLayout;
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
        this.getPartLayout().update(this, this.parts, this.bodyAnimation.current(), this.isFlying());
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

    @Override
    public boolean canTarget(LivingEntity target) {
        return !TamedDragons.isFriend(this, target) && super.canTarget(target);
    }

    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        return TamedDragons.canAttackWithOwner(target, owner);
    }

    // Wild ones leave in peaceful, like monsters (a lair sends a new one when you switch back)
    @Override
    protected boolean isDisallowedInPeaceful() {
        return !this.isTamed();
    }

    // Can't breed, so wheat shouldn't put it in love mode (and use the wheat up)
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<FlyingDragonEntity> controller = this.bodyAnimation.createController(this);
        controller.setSoundKeyframeHandler(event -> {});
        controllers.add(controller);
        controllers.add(this.fireBreath.createAnimationController(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
