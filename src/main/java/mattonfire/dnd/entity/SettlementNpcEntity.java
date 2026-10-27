package mattonfire.dnd.entity;

import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * What the settlement folk share (elves, Dragonborn, Tieflings, Gnome Tinkers, hobbits, and the guards and
 * merchants built on them): a skin variant ({@code Variant}), a home spot they keep near ({@code Home}, and
 * {@code HomeRange} for folk with one fixed range), persistence, villager sounds, no picking fights or loot, and
 * the rule that striking one brings the settlement's guards down on the player ({@link #callGuards}).
 */
public abstract class SettlementNpcEntity extends PathAwareEntity {
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(SettlementNpcEntity.class,
            TrackedDataHandlerRegistry.INTEGER);
    /** Guards this close to a struck NPC come to its defence ({@link #defend}). */
    public static final double DEFENCE_RANGE = 16.0D;
    /** Ticks between the checks that move the home range between its day and night size. */
    private static final int RANGE_CHECK_TICKS = 100;

    @Nullable
    private BlockPos home;
    /** One range kept day and night, or -1 for {@link #dayRange()} / {@link #nightRange()}. */
    private int homeRange;

    /** Folk who keep a {@link #dayRange()} by day and a {@link #nightRange()} at night. */
    protected SettlementNpcEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        this(entityType, world, -1);
    }

    /** Folk who keep one range day and night: {@code homeRange} unless {@link #setHome(BlockPos, int)} says. */
    protected SettlementNpcEntity(EntityType<? extends PathAwareEntity> entityType, World world, int homeRange) {
        super(entityType, world);
        this.homeRange = homeRange;
        ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        this.getNavigation().setCanSwim(true);
    }

    /** How many skins this kind of NPC has. */
    protected abstract int variants();

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, Math.floorMod(variant, this.variants()));
    }

    // ---- Home ----

    /** Ties the NPC to a spot it keeps near: {@link #dayRange} by day, {@link #nightRange} at night. */
    public void setHome(BlockPos home) {
        this.home = home.toImmutable();
        this.updateRange();
    }

    /** Ties the NPC to a spot it keeps within {@code range} of, day and night. */
    public void setHome(BlockPos home, int range) {
        this.homeRange = range;
        this.setHome(home);
    }

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    /** Whether {@code entity} is within {@code range} of home (always, for an NPC without one). */
    protected boolean nearHome(LivingEntity entity, int range) {
        return this.home == null || entity.getBlockPos().isWithinDistance(this.home, range);
    }

    /** How far from home this NPC roams by day. */
    protected int dayRange() {
        return this.homeRange;
    }

    /** How far from home this NPC roams at night. */
    protected int nightRange() {
        return this.homeRange;
    }

    private void updateRange() {
        if (this.home != null) {
            this.setPositionTarget(this.home, this.world.isDay() ? this.dayRange() : this.nightRange());
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.age % RANGE_CHECK_TICKS == 0) {
            this.updateRange();
        }
    }

    // ---- Goals ----

    /** The everyday goals of settlement folk who don't fight: see {@link FolkGoals}. */
    protected FolkGoals folkGoals() {
        return new FolkGoals(this);
    }

    /**
     * Swim, run from danger and from players Hostile with the NPC's faction (and from monsters, for folk who do),
     * use doors, wander, and look at players (and their kin). Set the speeds, then {@link #add()}.
     */
    protected static final class FolkGoals {
        private final SettlementNpcEntity npc;
        private double escapeSpeed = 1.3D;
        private float monsterFleeDistance;
        private double monsterFleeSpeed;
        private double playerFleeSpeed = 1.2D;
        private double walkTargetSpeed;
        private double wanderSpeed = 0.5D;
        @Nullable
        private Class<? extends LivingEntity> kin;

        private FolkGoals(SettlementNpcEntity npc) {
            this.npc = npc;
        }

        public FolkGoals escape(double speed) {
            this.escapeSpeed = speed;
            return this;
        }

        /** Also run from any hostile mob within {@code distance}, sprinting at {@code fastSpeed} when it's close. */
        public FolkGoals fleeMonsters(float distance, double fastSpeed) {
            this.monsterFleeDistance = distance;
            this.monsterFleeSpeed = fastSpeed;
            return this;
        }

        public FolkGoals fleePlayers(double fastSpeed) {
            this.playerFleeSpeed = fastSpeed;
            return this;
        }

        /** Also walk to a walk target (a bed, a meeting point). */
        public FolkGoals walkTarget(double speed) {
            this.walkTargetSpeed = speed;
            return this;
        }

        public FolkGoals wander(double speed) {
            this.wanderSpeed = speed;
            return this;
        }

        /** Also look at their own kind. */
        public FolkGoals lookAtKin(Class<? extends LivingEntity> kin) {
            this.kin = kin;
            return this;
        }

        public void add() {
            SettlementNpcEntity npc = this.npc;
            npc.goalSelector.add(0, new SwimGoal(npc));
            npc.goalSelector.add(1, new EscapeDangerGoal(npc, this.escapeSpeed));
            if (this.monsterFleeDistance > 0.0F) {
                npc.goalSelector.add(2, new FleeEntityGoal<>(npc, HostileEntity.class, this.monsterFleeDistance, 0.9D,
                        this.monsterFleeSpeed));
            }
            // Players Hostile with the NPC's faction are kept at a distance.
            npc.goalSelector.add(2, new FleeEntityGoal<>(npc, PlayerEntity.class, 8.0F, 0.9D, this.playerFleeSpeed,
                    player -> TierEffects.fleesFrom(TierEffects.tierWith((PlayerEntity) player, npc))));
            npc.goalSelector.add(3, new LongDoorInteractGoal(npc, true));
            if (this.walkTargetSpeed > 0.0D) {
                npc.goalSelector.add(4, new GoToWalkTargetGoal(npc, this.walkTargetSpeed));
            }
            npc.goalSelector.add(5, new WanderAroundFarGoal(npc, this.wanderSpeed));
            npc.goalSelector.add(6, new LookAtEntityGoal(npc, PlayerEntity.class, 6.0F));
            if (this.kin != null) {
                npc.goalSelector.add(6, new LookAtEntityGoal(npc, this.kin, 6.0F));
            }
            npc.goalSelector.add(7, new LookAroundGoal(npc));
        }
    }

    // ---- Fights ----

    // The mod gives every mob a player-targeting goal; settlement folk only fight whom mayTarget allows.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // A thrall (Charmed by a Mind Flayer, say) fights its charmer's target whatever its own rules.
        if (target == null || this.mayTarget(target)
                || target == mattonfire.dnd.classes.Conditions.ConditionEffects.charmerTarget(this)) {
            super.setTarget(target);
        }
    }

    /** Whether this NPC may fight {@code target}. Folk who aren't guards never pick fights. */
    protected boolean mayTarget(LivingEntity target) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isFriendlyFire(source)) {
            return false;
        }
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient) {
            this.afterHurt(source);
            if (source.getAttacker() instanceof PlayerEntity player && !player.isCreative() && !player.isSpectator()) {
                this.callGuards(player);
            }
        }
        return hurt;
    }

    /** Damage this NPC's own folk did by accident (a stray arrow or breath), which doesn't hurt it. */
    protected boolean isFriendlyFire(DamageSource source) {
        return false;
    }

    /** Server side, after this NPC was hurt by {@code source}. */
    protected void afterHurt(DamageSource source) {
    }

    /** {@code player} struck this NPC: its settlement's guards come for them (see {@link #defend}). */
    protected void callGuards(PlayerEntity player) {
    }

    /** Every {@code guards} within {@link #DEFENCE_RANGE} of {@code npc} turns on {@code player}, who struck it. */
    static <G extends MobEntity & SettlementGuard> void defend(Entity npc, PlayerEntity player, Class<G> guards) {
        for (G guard : npc.world.getEntitiesByClass(guards, new Box(npc.getBlockPos()).expand(DEFENCE_RANGE),
                LivingEntity::isAlive)) {
            guard.provoke(player);
        }
    }

    // ---- Sounds: a villager's (each kind sets its own pitch) ----

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getVariant());
        if (this.homeRange >= 0) {
            nbt.putInt("HomeRange", this.homeRange);
        }
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setVariant(nbt.getInt("Variant"));
        if (nbt.contains("HomeRange")) {
            this.homeRange = nbt.getInt("HomeRange");
        }
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")));
        }
    }
}
