package mattonfire.dnd.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A Clockwork Sentinel, Cogsdeep's guard: a copper golem with a clock in its chest, {@value #HEALTH} HP. It walks
 * the burrow within its home range and hunts monsters (not creepers, not a dormant mimic) within
 * {@link #HUNT_RANGE} blocks of home, swinging them into the air like an iron golem. Zombies and goblins go for it
 * as they do for iron golems. Neutral to players: it turns on one who strikes a gnome or a Sentinel
 * ({@link GnomeTinkerEntity#defend}), or whom it sees break the burrow's law ({@link TinkersLaw}), and on anyone
 * Hostile with the Tinkers' Guild. A copper ingot mends it by {@value #HEALTH_PER_INGOT}.
 *
 * <p>It's an {@link IronGolemEntity} underneath (the model, the attack swing, the crack textures, anger), with
 * the golem's village goals swapped for these.
 */
public class ClockworkSentinelEntity extends IronGolemEntity implements SettlementGuard {
    public static final double HEALTH = 40.0D;
    public static final int HUNT_RANGE = 20;
    public static final float HEALTH_PER_INGOT = 10.0F;

    @Nullable
    private BlockPos home;
    private int homeRange = 12;

    public ClockworkSentinelEntity(EntityType<? extends IronGolemEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 6;
    }

    public static DefaultAttributeContainer.Builder createSentinelAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, HEALTH)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, (double) HUNT_RANGE);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6D, 0.0F));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, GnomeTinkerEntity.class).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 5, true, false,
                entity -> hunts(entity) && this.nearHome(entity)));
    }

    /** The monsters Sentinels hunt: not creepers, and not a dormant mimic. */
    static boolean hunts(LivingEntity entity) {
        return entity instanceof Monster && !(entity instanceof CreeperEntity)
                && !(entity instanceof MimicEntity mimic && mimic.isDormant());
    }

    private boolean nearHome(LivingEntity entity) {
        return this.home == null || entity.getBlockPos().isWithinDistance(this.home, HUNT_RANGE);
    }

    /** Ties the Sentinel to a spot it keeps within {@code range} of. */
    public void setHome(BlockPos home, int range) {
        this.home = home.toImmutable();
        this.homeRange = range;
        this.setPositionTarget(this.home, this.homeRange);
    }

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        this.setPersistent();
        if (this.home == null) {
            this.setHome(this.getBlockPos(), this.homeRange);
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    /** The Sentinel's bell as it turns on a player who wronged the burrow or struck its folk. */
    @Override
    public void provokeShout() {
        this.playSound(SoundEvents.BLOCK_BELL_USE, 0.8F, 1.6F);
    }

    // The mod gives every mob a player-targeting goal; Sentinels only turn on players who wronged the burrow.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof PlayerEntity player && player != this.getAttacker() && !this.shouldAngerAt(player)) {
            return;
        }
        if (target instanceof GnomeTinkerEntity || target instanceof ClockworkSentinelEntity) {
            return;
        }
        super.setTarget(target);
    }

    /** Angry at the player it holds a grudge against, and at anyone Hostile with the Tinkers' Guild, on sight. */
    @Override
    public boolean shouldAngerAt(LivingEntity entity) {
        return super.shouldAngerAt(entity) || SettlementGuard.attacksOnSight(this, entity);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient && source.getAttacker() instanceof PlayerEntity player
                && !player.isCreative() && !player.isSpectator()) {
            GnomeTinkerEntity.defend(this, player);
        }
        return hurt;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.age % 100 == 0 && this.home != null) {
            this.setPositionTarget(this.home, this.homeRange);
        }
        // A player it no longer holds a grudge against (forgiven, or their standing rose) is let go.
        if (this.age % 20 == 0 && this.getTarget() instanceof PlayerEntity player && player != this.getAttacker()
                && !this.shouldAngerAt(player)) {
            super.setTarget(null);
        }
    }

    /** A copper ingot mends it (an iron ingot does nothing: it's not an iron golem). */
    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.COPPER_INGOT)) {
            return ActionResult.PASS;
        }
        float before = this.getHealth();
        this.heal(HEALTH_PER_INGOT);
        if (this.getHealth() == before) {
            return ActionResult.PASS;
        }
        this.playSound(SoundEvents.ENTITY_IRON_GOLEM_REPAIR, 1.0F, 1.3F);
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.success(this.world.isClient);
    }

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 1.3F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_IRON_GOLEM_HURT;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("HomeRange", this.homeRange);
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("HomeRange")) {
            this.homeRange = nbt.getInt("HomeRange");
        }
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")), this.homeRange);
        }
    }
}
