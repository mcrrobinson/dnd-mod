package mattonfire.dnd.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.SkillChecks.Perceivable;
import mattonfire.dnd.classes.SkillChecks.Perception;
import mattonfire.dnd.classes.SkillChecks.PerceptionService;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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

/**
 * A chest that isn't. A dormant mimic sits on the block grid facing a cardinal direction, looks
 * exactly like a chest (it's drawn with the real chest texture), makes no sound, can't be pushed
 * and can be stood on. Opening it (right-click) or hitting it wakes it: it bites, grabs the
 * player in its jaws for a moment, and hops after them. If nobody is around for a while it
 * settles back onto the grid and goes dormant again. When it dies it drops the loot it was
 * guarding: its loot table is the one of the chest it replaced.
 */
public class MimicEntity extends HostileEntity implements GeoEntity, Perceivable {
    private static final TrackedData<Boolean> DORMANT = DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    /** Ticks without a target before an awake mimic shuts its lid and pretends again. */
    private static final int SETTLE_DELAY = 200;
    /** How long a bite holds the target in the mimic's jaws. */
    private static final int GRAB_TICKS = 30;
    /** Pause after a grab ends before the next bite can grab again. */
    private static final int GRAB_COOLDOWN = 60;
    /** Passive Perception that notices a dormant mimic breathing, and the DC a Search must beat. */
    public static final int TELL_DC = 13;
    /** How close a player must be for the passive tell. */
    public static final double TELL_RANGE = 6.0;

    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("dormant");
    private static final RawAnimation AWAKE_ANIM = RawAnimation.begin().thenLoop("awake");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation WAKE_ANIM = RawAnimation.begin().thenPlay("wake");
    private static final RawAnimation BITE_ANIM = RawAnimation.begin().thenPlay("bite");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private Identifier loot;
    private float restYaw;
    private int idleTicks;
    private int grabTicks;
    private int grabCooldown;
    @Nullable
    private LivingEntity grabbed;
    /** Players who got the passive tell from this mimic (not saved). */
    private final Set<UUID> noticed = new HashSet<>();

    public MimicEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder createMimicAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    /**
     * A dormant mimic for worldgen, standing in for a chest at {@code pos} that faces {@code facing}
     * and would have held {@code lootTable}. Not yet added to the world.
     */
    @Nullable
    public static MimicEntity disguised(World world, BlockPos pos, Direction facing, Identifier lootTable) {
        MimicEntity mimic = ModEntityTypes.MIMIC.create(world);
        if (mimic == null) {
            return null;
        }
        mimic.restYaw = facing.asRotation();
        mimic.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, mimic.restYaw, 0.0F);
        mimic.setHeadYaw(mimic.restYaw);
        mimic.setBodyYaw(mimic.restYaw);
        mimic.loot = lootTable;
        mimic.setPersistent();
        return mimic;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DORMANT, true);
    }

    public boolean isDormant() {
        return this.dataTracker.get(DORMANT);
    }

    private void setDormant(boolean dormant) {
        this.dataTracker.set(DORMANT, dormant);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2D, false));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                player -> !this.isDormant()));
    }

    // ---- Waking up ----

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.isDormant()) {
            return super.interactMob(player, hand);
        }
        if (!this.world.isClient) {
            this.wake(player);
            this.tryAttack(player);
        }
        return ActionResult.success(this.world.isClient);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient && this.isDormant() && this.isAlive()) {
            this.wake(source.getAttacker() instanceof LivingEntity attacker ? attacker : null);
        }
        return hurt;
    }

    private void wake(@Nullable LivingEntity culprit) {
        this.setDormant(false);
        this.idleTicks = 0;
        if (culprit != null && !(culprit instanceof PlayerEntity player && (player.isCreative() || player.isSpectator()))) {
            this.setTarget(culprit);
        }
        this.playSound(SoundEvents.BLOCK_CHEST_OPEN, 1.0F, 0.7F);
        this.playSound(SoundEvents.ENTITY_HOGLIN_ANGRY, 1.0F, 0.6F);
        this.triggerAnim("action_controller", "wake");
    }

    /** Back on the grid, square to it, lid shut: just a chest again. */
    private void settle() {
        this.setDormant(true);
        this.idleTicks = 0;
        this.grabbed = null;
        this.getNavigation().stop();
        this.restYaw = Direction.fromRotation(this.getYaw()).asRotation();
        BlockPos block = this.getBlockPos();
        double x = block.getX() + 0.5D;
        double z = block.getZ() + 0.5D;
        Box snapped = this.getBoundingBox().offset(x - this.getX(), 0.0D, z - this.getZ());
        if (this.world.isSpaceEmpty(this, snapped)) {
            this.refreshPositionAndAngles(x, this.getY(), z, this.restYaw, 0.0F);
        } else {
            this.setYaw(this.restYaw);
        }
        this.setHeadYaw(this.restYaw);
        this.setBodyYaw(this.restYaw);
        this.playSound(SoundEvents.BLOCK_CHEST_CLOSE, 1.0F, 0.8F);
    }

    // ---- Biting and grabbing ----

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        this.triggerAnim("action_controller", "bite");
        this.playSound(SoundEvents.BLOCK_CHEST_CLOSE, 1.0F, 0.6F);
        if (hit) {
            this.playSound(SoundEvents.ENTITY_EVOKER_FANGS_ATTACK, 1.0F, 0.8F);
            if (target instanceof LivingEntity living && this.grabCooldown <= 0) {
                this.grabbed = living;
                this.grabTicks = GRAB_TICKS;
                this.grabCooldown = GRAB_TICKS + GRAB_COOLDOWN;
            }
        }
        return hit;
    }

    /** Drags the victim back to the mimic's mouth and keeps them there. */
    private void holdGrabbed(LivingEntity victim) {
        Vec3d facing = Vec3d.fromPolar(0.0F, this.bodyYaw);
        Vec3d mouth = this.getPos().add(facing.multiply(this.getWidth() * 0.5D + victim.getWidth() * 0.5D + 0.1D));
        Vec3d pull = new Vec3d(mouth.x - victim.getX(), 0.0D, mouth.z - victim.getZ()).multiply(0.4D);
        if (pull.lengthSquared() > 0.25D) {
            pull = pull.normalize().multiply(0.5D);
        }
        victim.setVelocity(pull.x, Math.min(victim.getVelocity().y, 0.0D), pull.z);
        victim.velocityModified = true;
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 3, false, false, true), this);
        this.getLookControl().lookAt(victim, 30.0F, 30.0F);
    }

    // ---- Being noticed ----

    @Override
    public void tick() {
        super.tick();
        if (this.world instanceof ServerWorld server && this.isDormant() && this.isAlive()
                && this.age % PerceptionService.PASSIVE_INTERVAL == 0) {
            Vec3d at = this.getPos();
            for (ServerPlayerEntity player : server.getPlayers(p -> p.isAlive() && !p.isSpectator()
                    && !this.noticed.contains(p.getUuid()) && p.squaredDistanceTo(at) <= TELL_RANGE * TELL_RANGE)) {
                if (PerceptionService.noticesPassively(player, Skill.PERCEPTION, TELL_DC)) {
                    DnDClasses.LOGGER.info("[Perception] {} notices the mimic at {} breathing (passive {} vs DC {})",
                            player.getEntityName(), this.getBlockPos().toShortString(),
                            PerceptionService.passive(player, Skill.PERCEPTION), TELL_DC);
                    this.perceive(player, false);
                }
            }
        }
    }

    @Override
    public int perceptionDc() {
        return TELL_DC;
    }

    @Override
    public Vec3d perceptionPos() {
        return this.getPos();
    }

    /** A dormant mimic can always be Searched (each Search outlines it again); the passive tell comes once. */
    @Override
    public boolean hiddenFrom(ServerPlayerEntity player) {
        return this.isDormant();
    }

    @Override
    public void perceive(ServerPlayerEntity player, boolean searched) {
        if (this.noticed.add(player.getUuid())) {
            Perception.mark(player, this, Perception.Mark.BREATH, 0);
            if (!searched) {
                player.sendMessage(Text.translatable("perception.dndclasses.mimic_tell"), true);
            }
        }
        if (searched) {
            Perception.mark(player, this, Perception.Mark.OUTLINE, Perception.OUTLINE_TICKS);
            player.sendMessage(Text.translatable("perception.dndclasses.mimic_found"), true);
        }
    }

    // ---- Ticking ----

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.isDormant()) {
            this.getNavigation().stop();
            this.setTarget(null);
            return;
        }
        if (this.grabCooldown > 0) {
            this.grabCooldown--;
        }
        if (this.grabbed != null) {
            LivingEntity victim = this.grabbed;
            boolean free = --this.grabTicks <= 0 || !victim.isAlive() || victim.isRemoved()
                    || this.squaredDistanceTo(victim) > 16.0D
                    || (victim instanceof PlayerEntity player && (player.isCreative() || player.isSpectator()));
            if (free) {
                this.grabbed = null;
            } else {
                this.getNavigation().stop();
                this.holdGrabbed(victim);
            }
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            if (++this.idleTicks >= SETTLE_DELAY && this.isOnGround()) {
                this.settle();
            }
        } else {
            this.idleTicks = 0;
        }
    }

    @Override
    public void tickMovement() {
        if (this.isDormant() && !this.world.isClient) {
            // A chest doesn't turn round or get shoved about.
            this.setYaw(this.restYaw);
            this.prevYaw = this.restYaw;
            this.setHeadYaw(this.restYaw);
            this.setBodyYaw(this.restYaw);
            this.prevHeadYaw = this.restYaw;
            this.prevBodyYaw = this.restYaw;
            this.setPitch(0.0F);
            Vec3d v = this.getVelocity();
            this.setVelocity(0.0D, v.y, 0.0D);
        }
        super.tickMovement();
    }

    @Override
    public boolean isPushable() {
        return !this.isDormant() && super.isPushable();
    }

    @Override
    public void pushAwayFrom(Entity entity) {
        if (!this.isDormant()) {
            super.pushAwayFrom(entity);
        }
    }

    /** Solid while dormant, like the chest it pretends to be: you can bump into it and stand on it. */
    @Override
    public boolean isCollidable() {
        return this.isDormant() && this.isAlive();
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return !this.isDormant() && super.canImmediatelyDespawn(distanceSquared);
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isDormant();
    }

    // ---- Loot ----

    /** The loot of the chest it replaced, or {@code entities/mimic} (dungeon loot) if it never replaced one. */
    @Override
    protected Identifier getLootTableId() {
        return this.loot != null ? this.loot : super.getLootTableId();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Dormant", this.isDormant());
        nbt.putFloat("RestYaw", this.restYaw);
        if (this.loot != null) {
            nbt.putString("MimicLoot", this.loot.toString());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Dormant")) {
            this.setDormant(nbt.getBoolean("Dormant"));
        }
        this.restYaw = nbt.contains("RestYaw") ? nbt.getFloat("RestYaw") : MathHelper.wrapDegrees(this.getYaw());
        this.loot = nbt.contains("MimicLoot") ? Identifier.tryParse(nbt.getString("MimicLoot")) : null;
    }

    @Override
    public void refreshPositionAndAngles(double x, double y, double z, float yaw, float pitch) {
        super.refreshPositionAndAngles(x, y, z, yaw, pitch);
        // Spawn eggs and /summon: face the way it was put down, squared to the grid.
        if (this.isDormant()) {
            this.restYaw = Direction.fromRotation(yaw).asRotation();
        }
    }

    // ---- Sounds ----

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isDormant() ? null : SoundEvents.ENTITY_HOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLOCK_WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_WOOD_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.BLOCK_WOOD_STEP, 0.6F, 0.8F);
    }

    // ---- Animation ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::movementPredicate));
        controllers.add(new AnimationController<MimicEntity>(this, "action_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("wake", WAKE_ANIM)
                .triggerableAnim("bite", BITE_ANIM));
    }

    private PlayState movementPredicate(AnimationState<MimicEntity> state) {
        if (this.isDormant()) {
            return state.setAndContinue(DORMANT_ANIM);
        }
        return state.setAndContinue(state.isMoving() ? WALK_ANIM : AWAKE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
