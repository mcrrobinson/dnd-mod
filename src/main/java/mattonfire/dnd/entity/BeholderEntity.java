package mattonfire.dnd.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.entity.ai.goal.BeholderAttackGoal;
import mattonfire.dnd.entity.ai.goal.BeholderDriftGoal;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.boss.BossFight;
import mattonfire.dnd.world.gen.beholder.BeholderCavernPiece;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Beholder: a floating eye tyrant that guards its lair deep underground (see BeholderLairStructure).
 *
 * <p>The fight alternates between two modes:
 * <ul>
 *   <li><b>Gaze</b>: its great central eye is open and projects an anti-magic cone. Players caught in
 *       it get {@link ModEffects#ANTI_MAGIC} (no class specials, class buffs dispelled). It closes in to bite.</li>
 *   <li><b>Rays</b>: it shuts the central eye (the cone would suppress its own magic too) and hangs back,
 *       charging and firing eye rays from its eight eyestalks: slowing, levitation, enervation (damage)
 *       and fear, two stalks of each. A ray is aimed where the target stood when the eye began to glow,
 *       so it can be dodged.</li>
 * </ul>
 * Below half health it enrages: rays fire in pairs and more often. Each eyestalk is its own hit shape
 * ({@link MultipartDragon}); a solid hit to a stalk shuts that eye for a while.
 */
public class BeholderEntity extends HostileEntity implements GeoEntity, MultipartDragon, Boss {
    /** The four kinds of eye ray, in eyestalk-pair order. */
    public enum Ray {
        SLOW("slow", 0x468CFF),
        LEVITATION("levitation", 0xC878FF),
        DAMAGE("damage", 0xFF3228),
        FEAR("fear", 0xF0DC3C);

        public final String id;
        private final DustParticleEffect dust;

        Ray(String id, int color) {
            this.id = id;
            this.dust = new DustParticleEffect(new Vector3f(((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F), 1.2F);
        }
    }

    private static final DragonPartLayout PART_LAYOUT = new DragonPartLayout("beholder", 0.0, 0.0)
            .pair("eye_slow", "stalk_slow_left", "stalk_slow_upper_left", "eye_slow_left", "lid_slow_left")
            .pair("eye_levitation", "stalk_levitation_left", "stalk_levitation_upper_left", "eye_levitation_left", "lid_levitation_left")
            .pair("eye_damage", "stalk_damage_left", "stalk_damage_upper_left", "eye_damage_left", "lid_damage_left")
            .pair("eye_fear", "stalk_fear_left", "stalk_fear_upper_left", "eye_fear_left", "lid_fear_left");
    /** Eyestalks, two of each ray in PART_LAYOUT's order. */
    public static final int STALKS = 8;

    public static final Identifier ADVANCEMENT = new Identifier(DnDClasses.MOD_ID, "beholder_slayer");
    private static final RegistryKey<Structure> LAIR_STRUCTURE =
            RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(DnDClasses.MOD_ID, "beholder_lair"));

    // Bit i set: eyestalk i is shut.
    private static final TrackedData<Integer> CLOSED_EYES = DataTracker.registerData(BeholderEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> CENTRAL_EYE_SHUT = DataTracker.registerData(BeholderEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    /** Anti-magic cone: reach, half-angle and how long the effect lingers after leaving it. */
    public static final double CONE_RANGE = 20.0;
    private static final double CONE_COS = Math.cos(Math.toRadians(30.0));
    private static final int ANTI_MAGIC_TICKS = 30;
    private static final DustParticleEffect CONE_DUST = new DustParticleEffect(new Vector3f(0.55F, 0.5F, 0.7F), 1.0F);

    private static final int GAZE_TICKS = 120;
    private static final int RAYS_TICKS = 160;
    private static final int ENRAGED_GAZE_TICKS = 80;
    private static final int ENRAGED_RAYS_TICKS = 200;
    private static final int CHARGE_TICKS = 16;
    private static final int RAY_COOLDOWN = 30;
    private static final int ENRAGED_RAY_COOLDOWN = 18;
    private static final double RAY_RANGE = 32.0;
    /** A hit this strong on an eyestalk shuts that eye for EYE_SHUT_TICKS. */
    private static final float EYE_SHUT_DAMAGE = 3.0F;
    private static final int EYE_SHUT_TICKS = 200;
    /** Frightened creatures this close get driven back. */
    private static final double FEAR_RADIUS = 7.0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final DragonPart[] parts;
    private final BossFight bossFight;

    private final int[] eyeReopenAt = new int[STALKS];
    private final List<Charge> charges = new ArrayList<>();
    private boolean gazing = true;
    private int modeTicks = GAZE_TICKS;
    private int rayCooldown = RAY_COOLDOWN;
    @Nullable
    private Ray lastRay;

    /** Where it lurks and drifts back to: its lair's cavern, or wherever it first appeared. */
    @Nullable
    private BlockPos home;

    private static final class Charge {
        final int stalk;
        final Vec3d aim;
        int ticks;

        Charge(int stalk, Vec3d aim, int ticks) {
            this.stalk = stalk;
            this.aim = aim;
            this.ticks = ticks;
        }
    }

    public BeholderEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FloatMoveControl(this);
        this.setNoGravity(true);
        this.parts = PART_LAYOUT.createParts(this);
        this.setId(DragonPartLayout.reserveIds(this.parts));
        this.bossFight = new BossFight(this, BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10)
                .range(48.0)
                .music(ModSounds.MUSIC_DRAGON_FIGHT)
                .phase(0.5F, BossBar.Color.RED, this::enrage)
                .xp(120)
                .advancement(ADVANCEMENT);
    }

    public static DefaultAttributeContainer.Builder createBeholderAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 250.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 2.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(2, new BeholderAttackGoal(this));
        this.goalSelector.add(5, new BeholderDriftGoal(this));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CLOSED_EYES, 0);
        this.dataTracker.startTracking(CENTRAL_EYE_SHUT, false);
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation navigation = new BirdNavigation(this, world);
        navigation.setCanPathThroughDoors(false);
        return navigation;
    }

    // The whole body is the head: it turns to face where it looks, so the cone and the model follow its gaze.
    @Override
    protected BodyControl createBodyControl() {
        return new BodyControl(this) {
            @Override
            public void tick() {
            }
        };
    }

    // ------------------------------------------------------------------ lair

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    public void setHome(@Nullable BlockPos home) {
        this.home = home;
    }

    // The lair's first Beholder gets its home from BeholderCavernPiece; later ones (spawn_overrides)
    // look the cavern up here.
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        if (this.home == null && spawnReason != SpawnReason.STRUCTURE) {
            StructureStart start = world.toServerWorld().getStructureAccessor()
                    .getStructureContaining(this.getBlockPos(), LAIR_STRUCTURE);
            if (start.hasChildren()) {
                for (StructurePiece piece : start.getChildren()) {
                    if (piece instanceof BeholderCavernPiece cavern) {
                        this.home = cavern.getHome();
                        break;
                    }
                }
            }
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // ------------------------------------------------------------------ eyes

    public boolean isEyeClosed(int stalk) {
        return (this.dataTracker.get(CLOSED_EYES) & (1 << stalk)) != 0;
    }

    public boolean isCentralEyeShut() {
        return this.dataTracker.get(CENTRAL_EYE_SHUT);
    }

    public boolean isGazing() {
        return this.gazing;
    }

    public boolean isEnraged() {
        return this.bossFight.getPhase() >= 1;
    }

    private void setEyeClosed(int stalk, boolean closed) {
        int mask = this.dataTracker.get(CLOSED_EYES);
        this.dataTracker.set(CLOSED_EYES, closed ? mask | (1 << stalk) : mask & ~(1 << stalk));
    }

    /** Where eyestalk {@code stalk}'s eye is, from its hit shape (rest pose on the server). */
    private Vec3d stalkEye(int stalk) {
        Box box = this.parts[stalk].getBoundingBox();
        if (box.minY < this.getY() - 100.0) {
            // Not placed yet
            return this.getPos().add(0.0, this.getHeight() + 0.8, 0.0);
        }
        return new Vec3d((box.minX + box.maxX) / 2.0, box.maxY - 0.2, (box.minZ + box.maxZ) / 2.0);
    }

    /** Where the central eye looks: its head yaw and pitch (it has no separate head). */
    public Vec3d getGazeDirection() {
        return this.getRotationVector(this.getPitch(), this.getHeadYaw());
    }

    public Vec3d getCentralEye() {
        return this.getPos().add(0.0, 1.25, 0.0).add(this.getGazeDirection().multiply(1.2));
    }

    public boolean isInCone(Entity entity) {
        Vec3d eye = this.getCentralEye();
        Vec3d to = entity.getBoundingBox().getCenter().subtract(eye);
        double distance = to.length();
        if (distance > CONE_RANGE) {
            return false;
        }
        if (distance > 1.5 && to.multiply(1.0 / distance).dotProduct(this.getGazeDirection()) < CONE_COS) {
            return false;
        }
        return this.canSee(entity);
    }

    @Override
    public boolean damagePart(DragonPart part, DamageSource source, float amount) {
        boolean hit = this.damage(source, amount);
        if (hit && !this.world.isClient && amount >= EYE_SHUT_DAMAGE) {
            for (int i = 0; i < this.parts.length; i++) {
                if (this.parts[i] == part && !this.isEyeClosed(i)) {
                    this.setEyeClosed(i, true);
                    this.eyeReopenAt[i] = this.age + EYE_SHUT_TICKS;
                    final int stalk = i;
                    this.charges.removeIf(charge -> charge.stalk == stalk);
                    this.playSound(SoundEvents.ENTITY_GUARDIAN_HURT_LAND, 1.5F, 0.6F);
                }
            }
        }
        return hit;
    }

    // ------------------------------------------------------------------ fight

    private void enrage() {
        this.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 2.0F, 0.8F);
        // It opens every eye in its fury
        for (int i = 0; i < STALKS; i++) {
            this.setEyeClosed(i, false);
        }
        this.gazing = false;
        this.modeTicks = ENRAGED_RAYS_TICKS;
    }

    @Override
    public void tick() {
        super.tick();
        // Face where it looks (it has no body control, on either side)
        this.bodyYaw = this.headYaw;
        if (!this.world.isClient) {
            this.setYaw(this.headYaw);
        }
        PART_LAYOUT.update(this, this.parts, false);
        this.bossFight.tick();
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.home == null) {
            this.home = this.getBlockPos();
        }
        for (int i = 0; i < STALKS; i++) {
            if (this.isEyeClosed(i) && this.age >= this.eyeReopenAt[i]) {
                this.setEyeClosed(i, false);
            }
        }

        LivingEntity target = this.getTarget();
        boolean fighting = target != null && target.isAlive();
        if (fighting && --this.modeTicks <= 0) {
            this.gazing = !this.gazing;
            boolean enraged = this.isEnraged();
            this.modeTicks = this.gazing ? (enraged ? ENRAGED_GAZE_TICKS : GAZE_TICKS) : (enraged ? ENRAGED_RAYS_TICKS : RAYS_TICKS);
            this.charges.clear();
            this.rayCooldown = 10;
            this.playSound(this.gazing ? SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE : SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, 1.5F, 0.7F);
        } else if (!fighting && !this.gazing) {
            // Out of a fight its great eye stays open
            this.gazing = true;
            this.modeTicks = GAZE_TICKS;
            this.charges.clear();
        }
        this.dataTracker.set(CENTRAL_EYE_SHUT, !this.gazing);

        if (this.gazing) {
            this.tickCone();
        } else if (fighting) {
            this.tickRays(target);
        }
        if (this.age % 10 == 0) {
            this.driveBackFrightened();
        }
    }

    private void tickCone() {
        ServerWorld world = (ServerWorld) this.world;
        if (this.age % 5 == 0) {
            for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(this) < (CONE_RANGE + 4) * (CONE_RANGE + 4))) {
                if (!player.isSpectator() && !player.isCreative() && this.isInCone(player)) {
                    player.addStatusEffect(new StatusEffectInstance(ModEffects.ANTI_MAGIC, ANTI_MAGIC_TICKS, 0, false, true, true), this);
                }
            }
        }
        if (this.age % 2 == 0 && this.getTarget() != null) {
            this.spawnConeParticles(world);
        }
    }

    private void spawnConeParticles(ServerWorld world) {
        Vec3d eye = this.getCentralEye();
        Vec3d dir = this.getGazeDirection();
        Vec3d side = Math.abs(dir.y) > 0.95 ? new Vec3d(1, 0, 0) : dir.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d up = side.crossProduct(dir).normalize();
        double tan = Math.tan(Math.acos(CONE_COS));
        for (int i = 0; i < 10; i++) {
            double t = 1.0 + this.random.nextDouble() * (CONE_RANGE - 1.0);
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double r = t * tan * Math.sqrt(this.random.nextDouble());
            Vec3d p = eye.add(dir.multiply(t)).add(side.multiply(Math.cos(angle) * r)).add(up.multiply(Math.sin(angle) * r));
            world.spawnParticles(CONE_DUST, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void tickRays(LivingEntity target) {
        ServerWorld world = (ServerWorld) this.world;
        Iterator<Charge> it = this.charges.iterator();
        while (it.hasNext()) {
            Charge charge = it.next();
            Vec3d eye = this.stalkEye(charge.stalk);
            world.spawnParticles(rayOf(charge.stalk).dust, eye.x, eye.y, eye.z, 3, 0.15, 0.15, 0.15, 0.0);
            if (--charge.ticks <= 0) {
                it.remove();
                this.fireRay(world, charge);
            }
        }

        if (this.charges.isEmpty() && --this.rayCooldown <= 0 && this.canSee(target)) {
            int volley = this.isEnraged() ? 2 : 1;
            for (int i = 0; i < volley; i++) {
                int stalk = this.pickStalk();
                if (stalk < 0) {
                    break;
                }
                // Aimed where the target is now: it has CHARGE_TICKS to get out of the way.
                this.charges.add(new Charge(stalk, target.getBoundingBox().getCenter(), CHARGE_TICKS));
                this.lastRay = rayOf(stalk);
            }
            if (!this.charges.isEmpty()) {
                this.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.6F);
            }
            this.rayCooldown = this.isEnraged() ? ENRAGED_RAY_COOLDOWN : RAY_COOLDOWN;
        }
    }

    private static Ray rayOf(int stalk) {
        return Ray.values()[stalk / 2];
    }

    /** A random open eye, preferring a different ray from the last one; -1 if every eye is shut. */
    private int pickStalk() {
        List<Integer> open = new ArrayList<>();
        List<Integer> fresh = new ArrayList<>();
        for (int i = 0; i < STALKS; i++) {
            final int stalk = i;
            if (this.isEyeClosed(i) || this.charges.stream().anyMatch(c -> rayOf(c.stalk) == rayOf(stalk))) {
                continue;
            }
            open.add(i);
            if (rayOf(i) != this.lastRay) {
                fresh.add(i);
            }
        }
        List<Integer> pool = fresh.isEmpty() ? open : fresh;
        return pool.isEmpty() ? -1 : pool.get(this.random.nextInt(pool.size()));
    }

    private void fireRay(ServerWorld world, Charge charge) {
        Ray ray = rayOf(charge.stalk);
        Vec3d from = this.stalkEye(charge.stalk);
        Vec3d dir = charge.aim.subtract(from);
        if (dir.lengthSquared() < 1.0E-4) {
            return;
        }
        Vec3d to = from.add(dir.normalize().multiply(RAY_RANGE));
        BlockHitResult block = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, this));
        if (block.getType() != HitResult.Type.MISS) {
            to = block.getPos();
        }
        EntityHitResult hit = ProjectileUtil.getEntityCollision(world, this, from, to, new Box(from, to).expand(1.0),
                EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.and(e -> e instanceof LivingEntity && e.isAlive()
                        && !(e instanceof DragonPart) && e != this));
        if (hit != null) {
            to = hit.getPos();
        }

        double length = from.distanceTo(to);
        Vec3d step = to.subtract(from).normalize().multiply(0.35);
        Vec3d p = from;
        for (double d = 0; d < length; d += 0.35) {
            world.spawnParticles(ray.dust, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            p = p.add(step);
        }
        world.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.5F, 1.4F);

        if (hit != null && hit.getEntity() instanceof LivingEntity victim) {
            this.applyRay(ray, victim);
        }
    }

    private void applyRay(Ray ray, LivingEntity victim) {
        switch (ray) {
            case SLOW -> {
                victim.damage(this.world.getDamageSources().indirectMagic(this, this), 2.0F);
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 2), this);
            }
            case LEVITATION -> victim.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 50, 1), this);
            case DAMAGE -> victim.damage(this.world.getDamageSources().indirectMagic(this, this), this.isEnraged() ? 10.0F : 8.0F);
            case FEAR -> {
                victim.addStatusEffect(new StatusEffectInstance(ModEffects.FRIGHTENED, 160, 0), this);
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0), this);
            }
        }
    }

    /** Frightened creatures can't bring themselves to come near it. */
    private void driveBackFrightened() {
        for (LivingEntity entity : this.world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(FEAR_RADIUS),
                e -> e != this && e.hasStatusEffect(ModEffects.FRIGHTENED))) {
            Vec3d away = entity.getPos().subtract(this.getPos()).multiply(1.0, 0.0, 1.0);
            away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : away.normalize();
            entity.addVelocity(away.x * 0.7, 0.25, away.z * 0.7);
            entity.velocityModified = true;
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            this.triggerAnim("attack_controller", "bite");
        }
        return hit;
    }

    // ------------------------------------------------------------------ boss plumbing

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
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ELDER_GUARDIAN_HURT_LAND;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH_LAND;
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
        this.home = nbt.contains("Home") ? NbtHelper.toBlockPos(nbt.getCompound("Home")) : null;
        this.setNoGravity(true);
    }

    // ------------------------------------------------------------------ animation

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, state -> state.setAndContinue(IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("bite", RawAnimation.begin().thenPlay("bite")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /** Moves straight at its destination, like a vex or a ghast, whichever way it's facing. */
    private static class FloatMoveControl extends MoveControl {
        FloatMoveControl(MobEntity entity) {
            super(entity);
        }

        @Override
        public void tick() {
            if (this.state != MoveControl.State.MOVE_TO) {
                return;
            }
            Vec3d delta = new Vec3d(this.targetX - this.entity.getX(), this.targetY - this.entity.getY(), this.targetZ - this.entity.getZ());
            double distance = delta.length();
            if (distance < 0.5) {
                this.state = MoveControl.State.WAIT;
                this.entity.setVelocity(this.entity.getVelocity().multiply(0.5));
                return;
            }
            double speed = this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_FLYING_SPEED);
            this.entity.setVelocity(this.entity.getVelocity().add(delta.multiply(speed * 0.05 / distance)));
        }
    }
}
