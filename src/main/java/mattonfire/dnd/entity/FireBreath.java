package mattonfire.dnd.entity;

import java.util.UUID;

import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.particle.ModParticles;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * A dragon's fire breath: a wind-up (rearing back with a growl), then a cone of flame from the mouth
 * that slowly swings after the target, so players can dodge by moving sideways.
 * <p>
 * The dragon registers the two tracked values (they must be registered on the dragon's own class),
 * calls {@link #track} from initDataTracker and {@link #tick} every tick, and adds
 * {@link #createAnimationController}, which plays the model's "attack.breath" animation.
 * <p>
 * Subclasses change what the breath is made of ({@link FrostBreath}) by overriding {@link #hit},
 * {@link #affectGround}, {@link #spawnParticles} and the sounds; the timing and cone stay the same.
 */
public class FireBreath {
    public static final int WINDUP = 12;
    public static final int DURATION = 40;
    // Half-angle of the flame: a narrow jet that fans out a little towards the end, like a flamethrower
    protected static final double CONE_TAN = Math.tan(Math.toRadians(10.0));
    // How far the aim moves towards the target each tick while breathing (0..1)
    private static final float TRACKING = 0.12F;
    /** Seconds alight after a hit, and after a hit on a successful DEX save. */
    public static final int BURN_SECONDS = 5;
    public static final int BURN_SECONDS_SAVED = 2;

    protected final TameableEntity dragon;
    private final DragonPart head;
    // Ticks left of the whole breath (wind-up + flame); 0 when not breathing
    private final TrackedData<Integer> ticksKey;
    // Unit vector the flame points along
    private final TrackedData<Vector3f> aimKey;
    public final double range;
    protected final float damage;

    public FireBreath(TameableEntity dragon, DragonPart head, TrackedData<Integer> ticksKey, TrackedData<Vector3f> aimKey,
            double range, float damage) {
        this.dragon = dragon;
        this.head = head;
        this.ticksKey = ticksKey;
        this.aimKey = aimKey;
        this.range = range;
        this.damage = damage;
    }

    public static void track(DataTracker tracker, TrackedData<Integer> ticksKey, TrackedData<Vector3f> aimKey) {
        tracker.startTracking(ticksKey, 0);
        tracker.startTracking(aimKey, new Vector3f(0, 0, 1));
    }

    public boolean isBreathing() {
        return this.dragon.getDataTracker().get(this.ticksKey) > 0;
    }

    /** Layered over the base animation: rears the head back, then holds the jaw open while breathing */
    public <T extends GeoEntity> AnimationController<T> createAnimationController(T animatable) {
        return new AnimationController<>(animatable, "breath", 2, event -> {
            if (!this.isBreathing()) {
                event.getController().forceAnimationReset();
                return PlayState.STOP;
            }
            return event.setAndContinue(RawAnimation.begin().thenPlay("attack.breath"));
        });
    }

    public void start(LivingEntity target) {
        if (this.dragon.world.isClient || this.isBreathing() || this.isMouthUnderwater()) {
            return;
        }
        this.dragon.getDataTracker().set(this.ticksKey, WINDUP + DURATION);
        this.setAim(this.directionTo(target));
        this.dragon.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 1.4F);
    }

    protected Vec3d getAim() {
        return new Vec3d(this.dragon.getDataTracker().get(this.aimKey));
    }

    private void setAim(Vec3d aim) {
        this.dragon.getDataTracker().set(this.aimKey, aim.toVector3f());
    }

    /** Where the flame comes out: the front of the head, following the animated model on the client */
    public Vec3d getMouthPos() {
        Box box = this.head.getBoundingBox();
        Vec3d forward = Vec3d.fromPolar(0, this.dragon.bodyYaw);
        double reach = Math.max(box.getXLength(), box.getZLength()) / 2;
        return box.getCenter().add(forward.multiply(reach * 0.8));
    }

    private boolean isMouthUnderwater() {
        return this.dragon.world.getFluidState(BlockPos.ofFloored(this.getMouthPos())).isIn(FluidTags.WATER);
    }

    private Vec3d directionTo(LivingEntity target) {
        Vec3d aimAt = target.getPos().add(0, target.getHeight() * 0.5, 0);
        Vec3d dir = aimAt.subtract(this.getMouthPos());
        return dir.lengthSquared() < 1.0E-4 ? Vec3d.fromPolar(this.dragon.getPitch(), this.dragon.headYaw) : dir.normalize();
    }

    public void tick() {
        int ticks = this.dragon.getDataTracker().get(this.ticksKey);
        if (ticks <= 0) {
            return;
        }
        boolean flame = ticks <= DURATION && !this.isMouthUnderwater();
        if (this.dragon.world.isClient) {
            if (flame) {
                this.spawnParticles();
            }
            return;
        }

        LivingEntity target = this.dragon.getTarget();
        boolean hasTarget = target != null && target.isAlive();
        if (!this.dragon.isAlive() || (ticks > DURATION && !hasTarget)) {
            // Lost its target during the wind-up: don't breathe at nothing
            this.dragon.getDataTracker().set(this.ticksKey, 0);
            return;
        }
        if (hasTarget) {
            Vec3d desired = this.directionTo(target);
            // Snap on during the wind-up, then lag behind so the flame can be outrun
            this.setAim(ticks > DURATION ? desired : this.getAim().lerp(desired, TRACKING).normalize());
        }

        if (flame) {
            if (ticks == DURATION) {
                this.playStartSound();
            }
            if (ticks % 4 == 0) {
                this.playLoopSound();
            }
            this.burnCone();
        }
        this.dragon.getDataTracker().set(this.ticksKey, ticks - 1);
    }

    /** Played once as the flame starts. */
    protected void playStartSound() {
        this.dragon.playSound(SoundEvents.ITEM_FIRECHARGE_USE, 2.0F, 0.6F);
    }

    /** Played every 4 ticks while the flame pours out. */
    protected void playLoopSound() {
        this.dragon.world.playSound(null, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.5F,
                0.5F + this.dragon.getRandom().nextFloat() * 0.2F);
    }

    /**
     * Something caught in the cone (damage cooldowns limit how often this lands). It makes a DEX save once per
     * breath: a success halves the damage and burns for {@link #BURN_SECONDS_SAVED} instead of
     * {@link #BURN_SECONDS}.
     */
    protected void hit(LivingEntity living) {
        DamageSource source = this.dragon.getDamageSources().create(DamageTypes.IN_FIRE, this.dragon);
        if (DragonSaves.isUnaffected(living, source)) {
            return;
        }
        SaveResult save = this.save(living);
        if (living.damage(source, save.damage(this.damage))) {
            living.setOnFireFor(save.succeeded() ? BURN_SECONDS_SAVED : BURN_SECONDS);
        }
    }

    /** The victim's DEX save against this breath: rolled on the first hit, reused for the rest of the breath. */
    protected SaveResult save(LivingEntity living) {
        return DragonSaves.save(living, this.dragon, this.saveLabel(), this.saveDc(), this.saveEffect(), "breath",
                DragonSaves.BREATH_WINDOW);
    }

    /** The DEX save DC, from the dragon ({@link FireBreather#getBreathSaveDc()}). */
    protected int saveDc() {
        return this.dragon instanceof FireBreather breather ? breather.getBreathSaveDc() : WyvernEntity.BREATH_SAVE_DC;
    }

    /** What the save is called on the save lane. */
    protected String saveLabel() {
        return DragonSaves.FIRE_BREATH;
    }

    /** What a successful save does, shown on the save lane. */
    protected String saveEffect() {
        return DragonSaves.FIRE_BREATH_EFFECT;
    }

    /** Where the flame meets a block, every 5 ticks with mobGriefing on: sometimes lights a fire. */
    protected void affectGround(BlockHitResult hit) {
        if (this.dragon.getRandom().nextInt(3) == 0) {
            BlockPos firePos = hit.getBlockPos().offset(hit.getSide());
            if (AbstractFireBlock.canPlaceAt(this.dragon.world, firePos, hit.getSide().getOpposite())) {
                this.dragon.world.setBlockState(firePos, AbstractFireBlock.getState(this.dragon.world, firePos));
            }
        }
    }

    private void burnCone() {
        Vec3d mouth = this.getMouthPos();
        Vec3d aim = this.getAim();
        Vec3d end = mouth.add(aim.multiply(this.range));
        Box area = new Box(mouth, end).expand(this.range * 0.45);
        for (Entity entity : this.dragon.world.getOtherEntities(this.dragon, area,
                e -> e instanceof LivingEntity && e.isAlive() && !(e instanceof DragonPart))) {
            LivingEntity living = (LivingEntity) entity;
            if (this.isAlly(living) || !this.inCone(mouth, aim, living)) {
                continue;
            }
            this.hit(living);
        }

        // Scorch the ground where the flame lands
        if (this.dragon.age % 5 == 0 && this.dragon.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            BlockHitResult hit = this.dragon.world.raycast(new RaycastContext(mouth, end,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, this.dragon));
            if (hit.getType() == HitResult.Type.BLOCK) {
                this.affectGround(hit);
            }
        }
    }

    private boolean inCone(Vec3d mouth, Vec3d aim, LivingEntity entity) {
        Box box = entity.getBoundingBox();
        // Anything right in front of the mouth gets burnt even if it's off to the side
        if (box.expand(1.0).contains(mouth)) {
            return true;
        }
        Vec3d center = box.getCenter();
        Vec3d to = center.subtract(mouth);
        if (to.length() > this.range + entity.getWidth() / 2) {
            return false;
        }
        // Widen the cone by the entity's size so big mobs and near misses at the edge still count
        double along = to.dotProduct(aim);
        double sideways = to.subtract(aim.multiply(along)).length();
        if (along <= 0 || sideways > along * CONE_TAN + entity.getWidth() / 2 + 0.3) {
            return false;
        }
        return this.dragon.world.raycast(new RaycastContext(mouth, center, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, this.dragon)).getType() == HitResult.Type.MISS;
    }

    private boolean isAlly(LivingEntity entity) {
        if (entity == this.dragon.getOwner() || this.dragon.isTeammate(entity)) {
            return true;
        }
        if (entity instanceof TameableEntity tameable) {
            UUID owner = this.dragon.getOwnerUuid();
            if (owner != null && owner.equals(tameable.getOwnerUuid())) {
                return true;
            }
        }
        // Wild dragons don't torch each other
        return !this.dragon.isTamed() && entity instanceof MultipartDragon
                && entity instanceof TameableEntity other && !other.isTamed();
    }

    protected void spawnParticles() {
        Vec3d mouth = this.getMouthPos();
        Vec3d aim = this.getAim();
        var random = this.dragon.getRandom();
        // The jet stops at the first block in its way
        BlockHitResult hit = this.dragon.world.raycast(new RaycastContext(mouth, mouth.add(aim.multiply(this.range)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, this.dragon));
        double length = hit.getPos().distanceTo(mouth);
        // Two unit vectors across the jet, for the particles' sideways offsets
        Vec3d side = aim.crossProduct(Math.abs(aim.y) > 0.9 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0)).normalize();
        Vec3d up = side.crossProduct(aim);

        // Puffs launched from the mouth fast enough to just reach the end of the jet before fading,
        // swelling as they slow down, so the jet starts thin and billows out like a flamethrower
        double reach = (1 - Math.pow(ModParticles.DRAGON_FLAME_DRAG, ModParticles.DRAGON_FLAME_AGE)) / (1 - ModParticles.DRAGON_FLAME_DRAG);
        for (int i = 0; i < 30; i++) {
            double along = random.nextDouble() * Math.min(1.5, length * 0.15);
            double speed = (length - along) / reach * (0.85 + random.nextDouble() * 0.25);
            double a = random.nextGaussian() * CONE_TAN * 0.6;
            double b = random.nextGaussian() * CONE_TAN * 0.6;
            Vec3d pos = mouth.add(aim.multiply(along));
            Vec3d velocity = aim.add(side.multiply(a)).add(up.multiply(b)).multiply(speed);
            this.dragon.world.addParticle(ModParticles.DRAGON_FLAME, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        }
        // A few loose sparks around the jet
        for (int i = 0; i < 3; i++) {
            double along = length * random.nextDouble();
            double radius = 0.3 + along * CONE_TAN;
            Vec3d pos = mouth.add(aim.multiply(along)).add(side.multiply(random.nextGaussian() * radius))
                    .add(up.multiply(random.nextGaussian() * radius));
            this.dragon.world.addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, aim.x * 0.1, aim.y * 0.1 + 0.03, aim.z * 0.1);
        }
        // Smoke rolling off the far end of the jet
        for (int i = 0; i < 2; i++) {
            Vec3d pos = mouth.add(aim.multiply(length * (0.6 + random.nextDouble() * 0.4)));
            this.dragon.world.addParticle(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z,
                    aim.x * 0.05, 0.05, aim.z * 0.05);
        }
        // Flames splashing off whatever it hits
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3d at = hit.getPos();
            for (int i = 0; i < 4; i++) {
                this.dragon.world.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z,
                        random.nextGaussian() * 0.08, random.nextDouble() * 0.08, random.nextGaussian() * 0.08);
            }
        }
        if (random.nextInt(3) == 0) {
            this.dragon.world.addParticle(ParticleTypes.LAVA, mouth.x, mouth.y, mouth.z, 0, 0, 0);
        }
    }
}
