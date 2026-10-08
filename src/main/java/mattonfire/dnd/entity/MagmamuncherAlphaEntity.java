package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.boss.BossFight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Boss of the Nether: a huge, hostile Magmamuncher. Its bites set targets alight. Below half health
 * it enrages: it moves faster, bites harder, calls two regular Magmamunchers to its side and starts
 * spitting volleys of fireballs at targets out of reach.
 */
public class MagmamuncherAlphaEntity extends HostileEntity implements GeoEntity, Boss {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final String[] BITES = {"attack1", "attack2", "attack3"};

    private static final int BITE_FIRE_SECONDS = 4;
    private static final int ENRAGED_BITE_FIRE_SECONDS = 8;
    private static final int SPIT_COOLDOWN = 80;
    private static final int SPIT_VOLLEY = 3;
    private static final double SPIT_MIN_RANGE = 4.0;
    private static final double SPIT_MAX_RANGE = 20.0;
    private static final int PACK_SIZE = 2;

    private static final UUID ENRAGE_SPEED_ID = UUID.fromString("0f3b6a1e-2c5d-4e7f-8a9b-1c2d3e4f5a71");
    private static final UUID ENRAGE_DAMAGE_ID = UUID.fromString("7e6d5c4b-3a29-4180-9f8e-7d6c5b4a3f82");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    // Yellow bar while it fights a player, red once it enrages.
    private final BossFight bossFight;
    private long nextSpitTime;

    public MagmamuncherAlphaEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.bossFight = new BossFight(this, BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10)
                .range(48.0)
                .music(ModSounds.MUSIC_DRAGON_FIGHT)
                .phase(0.5F, BossBar.Color.RED, this::enrage)
                .xp(100);
    }

    public static DefaultAttributeContainer.Builder createMagmamuncherAlphaAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 10.0D)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 2.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 14.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, MagmamuncherEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            target.setOnFireFor(this.isEnraged() ? ENRAGED_BITE_FIRE_SECONDS : BITE_FIRE_SECONDS);
            this.triggerAnim("attack_controller", BITES[this.random.nextInt(BITES.length)]);
        }
        return hit;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        LivingEntity target = this.getTarget();
        long now = this.world.getTime();
        if (this.isEnraged() && target != null && target.isAlive() && now >= this.nextSpitTime) {
            double distance = this.distanceTo(target);
            if (distance > SPIT_MIN_RANGE && distance < SPIT_MAX_RANGE && this.canSee(target)) {
                this.nextSpitTime = now + SPIT_COOLDOWN;
                this.spitFire(target);
            }
        }
    }

    /** A spread of small fireballs from the mouth, like a blaze volley but all at once. */
    private void spitFire(LivingEntity target) {
        this.triggerAnim("attack_controller", "attack1");
        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        double mouthY = this.getBodyY(0.6D);
        double dx = target.getX() - this.getX();
        double dy = target.getBodyY(0.5D) - mouthY;
        double dz = target.getZ() - this.getZ();
        double spread = Math.sqrt(Math.sqrt(dx * dx + dz * dz)) * 0.4D;
        for (int i = 0; i < SPIT_VOLLEY; i++) {
            SmallFireballEntity fireball = new SmallFireballEntity(this.world, this,
                    dx + this.random.nextGaussian() * spread, dy, dz + this.random.nextGaussian() * spread);
            fireball.setPosition(this.getX() + dx / Math.max(1.0D, this.distanceTo(target)) * 1.5D, mouthY,
                    this.getZ() + dz / Math.max(1.0D, this.distanceTo(target)) * 1.5D);
            this.world.spawnEntity(fireball);
        }
        this.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 2.0F, 0.6F);
    }

    @Override
    public void tick() {
        super.tick();
        this.bossFight.tick();
        // Once it has fought a player it stays, so kiting it out of range can't despawn a half-killed boss.
        if (!this.world.isClient && !this.isPersistent() && this.bossFight.isFightingPlayer()) {
            this.setPersistent();
        }
        if (this.world.isClient && this.random.nextInt(4) == 0) {
            // Embers drifting off its back.
            this.world.addParticle(this.random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.LAVA,
                    this.getParticleX(0.6D), this.getBodyY(0.7D + this.random.nextDouble() * 0.3D),
                    this.getParticleZ(0.6D), 0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    public BossFight getBossFight() {
        return this.bossFight;
    }

    /** Below half health: faster, harder bites, longer burns, fireball volleys. */
    public boolean isEnraged() {
        return this.bossFight.getPhase() >= 1;
    }

    private void enrage() {
        addModifier(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED),
                new EntityAttributeModifier(ENRAGE_SPEED_ID, "Magmamuncher Alpha enrage speed", 0.3D,
                        EntityAttributeModifier.Operation.MULTIPLY_BASE));
        addModifier(this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE),
                new EntityAttributeModifier(ENRAGE_DAMAGE_ID, "Magmamuncher Alpha enrage damage", 6.0D,
                        EntityAttributeModifier.Operation.ADDITION));
        this.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 3.0F, 0.6F);
        ServerWorld world = (ServerWorld) this.world;
        world.spawnParticles(ParticleTypes.LAVA, this.getX(), this.getBodyY(0.8D), this.getZ(),
                30, 1.2D, 0.6D, 1.2D, 0.0D);
        world.spawnParticles(ParticleTypes.FLAME, this.getX(), this.getBodyY(0.5D), this.getZ(),
                60, 1.5D, 0.8D, 1.5D, 0.05D);
        this.summonPack();
        this.nextSpitTime = this.world.getTime() + 20;
    }

    private static void addModifier(EntityAttributeInstance attribute, EntityAttributeModifier modifier) {
        if (attribute != null && !attribute.hasModifier(modifier)) {
            attribute.addPersistentModifier(modifier);
        }
    }

    /** Two regular Magmamunchers crawl out of the ground to join the fight. */
    private void summonPack() {
        ServerWorld world = (ServerWorld) this.world;
        LivingEntity target = this.getTarget() != null ? this.getTarget() : this.getAttacker();
        for (int i = 0; i < PACK_SIZE; i++) {
            MagmamuncherEntity minion = ModEntityTypes.MAGMAMUNCHER.create(world);
            if (minion == null || !this.placeMinion(minion)) {
                continue;
            }
            minion.initialize(world, world.getLocalDifficulty(minion.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
            if (target != null && target.isAlive()) {
                minion.setTarget(target);
            }
            world.spawnEntityAndPassengers(minion);
            world.spawnParticles(ParticleTypes.LAVA, minion.getX(), minion.getY() + 0.2D, minion.getZ(),
                    12, 0.5D, 0.2D, 0.5D, 0.0D);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, minion.getX(), minion.getBodyY(0.5D), minion.getZ(),
                    15, 0.5D, 0.5D, 0.5D, 0.02D);
        }
    }

    /** Finds standing room for a minion a few blocks around the alpha. */
    private boolean placeMinion(MagmamuncherEntity minion) {
        for (int attempt = 0; attempt < 12; attempt++) {
            float angle = this.random.nextFloat() * MathHelper.TAU;
            double distance = 3.0D + this.random.nextDouble() * 2.5D;
            BlockPos column = BlockPos.ofFloored(this.getX() + MathHelper.cos(angle) * distance, this.getY(),
                    this.getZ() + MathHelper.sin(angle) * distance);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos pos = column.up(dy);
                if (!this.world.getBlockState(pos.down()).isSideSolidFullSquare(this.world, pos.down(), Direction.UP)) {
                    continue;
                }
                minion.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        this.random.nextFloat() * 360.0F, 0.0F);
                if (this.world.isSpaceEmpty(minion)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isTeammate(Entity other) {
        return (other instanceof MagmamuncherEntity muncher && !muncher.isTamed()) || super.isTeammate(other);
    }

    @Override
    public float getSoundPitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 0.6F;
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::predicate));
        AnimationController<MagmamuncherAlphaEntity> attack = new AnimationController<>(this, "attack_controller", 0,
                state -> PlayState.STOP);
        for (String bite : BITES) {
            attack.triggerableAnim(bite, RawAnimation.begin().thenPlay(bite));
        }
        controllers.add(attack);
    }

    private PlayState predicate(AnimationState<MagmamuncherAlphaEntity> state) {
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.bossFight.writeNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.bossFight.readNbt(nbt);
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
}
