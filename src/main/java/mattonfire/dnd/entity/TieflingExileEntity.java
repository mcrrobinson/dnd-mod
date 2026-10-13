package mattonfire.dnd.entity;

import java.util.EnumSet;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;

/**
 * A Tiefling Exile, who keeps a Brimstone Sanctum. Neutral to players, like a Dragonborn Templar: it hunts
 * monsters within {@link #HUNT_RANGE} blocks of home (never piglins, zombified piglins, endermen or creepers,
 * so the sanctum stays neutral ground) with a golden sword or an iron axe, and throws a small fireball at
 * anything it fights {@link #FIREBALL_MIN}-{@link #FIREBALL_MAX} blocks away, every {@link #FIREBALL_COOLDOWN}
 * ticks. Fire immune, and a melee hit on any Tiefling sets the attacker alight (Hellish Rebuke). It turns on a
 * player who strikes a Tiefling, or whom it sees break the sanctum's law ({@link SanctumLaw}).
 */
public class TieflingExileEntity extends TieflingEntity implements GuardAnger {
    public static final int HUNT_RANGE = 24;
    public static final double FIREBALL_MIN = 6.0D;
    public static final double FIREBALL_MAX = 16.0D;
    public static final int FIREBALL_COOLDOWN = 4 * 20;

    private final GuardAnger.State anger = new GuardAnger.State();
    /** The age at which the next fireball is ready. */
    private int fireballReadyAge;

    public TieflingExileEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createExileAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, (double) HUNT_RANGE);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FireballGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.add(3, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        SettlementGuard.addTargetGoals(this.targetSelector, this, TieflingEntity.class,
                entity -> hunts(entity) && this.nearHome(entity, HUNT_RANGE));
    }

    /** The monsters Exiles hunt: not creepers, endermen, piglins of either kind, or a dormant mimic. */
    static boolean hunts(LivingEntity entity) {
        return entity instanceof Monster && !(entity instanceof CreeperEntity) && !(entity instanceof EndermanEntity)
                && !(entity instanceof AbstractPiglinEntity) && !(entity instanceof ZombifiedPiglinEntity)
                && !(entity instanceof MimicEntity mimic && mimic.isDormant());
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(world.getRandom().nextFloat() < 0.7F ? Items.GOLDEN_SWORD : Items.IRON_AXE));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setEquipmentDropChance(slot, 0.05F);
        }
        return data;
    }

    /** The Exile's shout as it turns on a player who wronged the sanctum or struck one of its folk. */
    @Override
    public void provokeShout() {
        this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch() * 0.8F);
    }

    // Guards only turn on players who wronged them, and never on their own folk.
    @Override
    protected boolean mayTarget(LivingEntity target) {
        return SettlementGuard.mayTarget(this, target, TieflingEntity.class);
    }

    /** Angry at the player it holds a grudge against, and at anyone Hostile with the Brimstone Covenant, on sight. */
    @Override
    public boolean shouldAngerAt(LivingEntity entity) {
        return GuardAnger.super.shouldAngerAt(entity) || SettlementGuard.attacksOnSight(this, entity);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!this.world.isClient) {
            this.tickAngerLogic((ServerWorld) this.world, true);
        }
    }

    // ---- The fireball ----

    public boolean fireballReady() {
        return this.age >= this.fireballReadyAge;
    }

    /** Throws a small fireball (a blaze's) at {@code target}. */
    public void throwFireball(LivingEntity target) {
        this.fireballReadyAge = this.age + FIREBALL_COOLDOWN;
        this.getLookControl().lookAt(target, 60.0F, 60.0F);
        double dx = target.getX() - this.getX();
        double dy = target.getBodyY(0.5D) - this.getBodyY(0.5D);
        double dz = target.getZ() - this.getZ();
        SmallFireballEntity fireball = new SmallFireballEntity(this.world, this, dx, dy, dz);
        fireball.setPosition(fireball.getX(), this.getBodyY(0.5D) + 0.5D, fireball.getZ());
        this.world.spawnEntity(fireball);
        this.swingHand(net.minecraft.util.Hand.OFF_HAND);
        if (!this.isSilent()) {
            this.world.syncWorldEvent(null, WorldEvents.BLAZE_SHOOTS, this.getBlockPos(), 0);
        }
        mattonfire.dnd.classes.DnDClasses.LOGGER.info("[BrimstoneSanctum] Exile {} threw a fireball at {} ({} blocks)",
                this.getName().getString(), target.getName().getString(), String.format("%.1f", Math.sqrt(this.squaredDistanceTo(target))));
    }

    // ---- Angerable ----

    @Override
    public GuardAnger.State angerState() {
        return this.anger;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.writeAngerToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.readAngerFromNbt(this.world, nbt);
    }

    /** Throws a fireball when the target is 6-16 blocks off, in sight and the fireball is ready; runs beside the melee goal. */
    static final class FireballGoal extends Goal {
        private final TieflingExileEntity exile;

        FireballGoal(TieflingExileEntity exile) {
            this.exile = exile;
            this.setControls(EnumSet.noneOf(Control.class));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.exile.getTarget();
            if (target == null || !target.isAlive() || !this.exile.fireballReady()) {
                return false;
            }
            double d = this.exile.squaredDistanceTo(target);
            return d >= FIREBALL_MIN * FIREBALL_MIN && d <= FIREBALL_MAX * FIREBALL_MAX
                    && this.exile.getVisibilityCache().canSee(target);
        }

        @Override
        public boolean shouldContinue() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = this.exile.getTarget();
            if (target != null) {
                this.exile.throwFireball(target);
            }
        }
    }
}
