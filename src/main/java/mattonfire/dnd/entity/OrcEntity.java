package mattonfire.dnd.entity;

import mattonfire.dnd.world.gen.RacialHomes;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * An orc of an Orc Stronghold (the Bloodtusk Horde). The base class holds what they all share: a skin
 * variant, a name, a home spot they keep near, the hold they belong to, and who they fight
 * ({@link OrcHostility}): players Hostile with the horde on sight within 16 blocks (8 for Barbarians), anyone
 * who strikes an orc (every orc within 16 blocks joins in), and hobbits, dwarves, elves and villagers near
 * home. Never goblins: the horde and the goblins are allies, and neither targets the other.
 */
public abstract class OrcEntity extends PathAwareEntity implements GuardAnger {
    public static final int VARIANTS = 6;
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(OrcEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Orcs this close to a struck orc join the fight. */
    public static final double RALLY_RANGE = 16.0D;
    /** Orcs go for the horde's enemies (hobbits, dwarves, elves, villagers) this close to home. */
    public static final int HUNT_RANGE = 24;
    /** Hold key for an orc that doesn't know its hold yet. */
    public static final long NO_HOLD = Long.MIN_VALUE;

    private static final String[] FIRST_NAMES = {
            "Dench", "Feng", "Gell", "Henk", "Holg", "Imsh", "Keth", "Krusk", "Mhurren", "Ront", "Shump", "Thokk",
            "Grukk", "Mazg", "Urzul", "Rogash", "Baggi", "Emen", "Engong", "Kansif", "Myev", "Neega", "Ovak",
            "Ownka", "Shautha", "Sutha", "Vola", "Volen", "Yevelda", "Brakka", "Gorza"
    };
    private static final String[] EPITHETS = {
            "Bloodtusk", "Skullsplitter", "Ironjaw", "Bonegnawer", "Redhand", "Gutripper", "Stonefist", "Ashmaw",
            "Wolfbane", "Blackscar", "Halfear", "Dustborn", "Gorefang", "Rockhide"
    };

    @Nullable
    private BlockPos home;
    private int homeRange = 16;
    private long hold = NO_HOLD;
    private final GuardAnger.State anger = new GuardAnger.State();

    protected OrcEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        this.getNavigation().setCanSwim(true);
        this.experiencePoints = 8;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.initCombatGoals();
        this.goalSelector.add(3, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, OrcEntity.class).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                player -> OrcHostility.attacksOnSight(this, (PlayerEntity) player)));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false,
                entity -> OrcHostility.isHordeEnemy(entity) && this.nearHome(entity)));
    }

    /** The fighting goals (priority 1-2). Warriors swing an axe; archers shoot. */
    protected void initCombatGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1D, true));
    }

    protected boolean nearHome(LivingEntity entity) {
        return this.home == null || entity.getBlockPos().isWithinDistance(this.home, HUNT_RANGE);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, Math.floorMod(variant, VARIANTS));
    }

    /** Ties the orc to a spot it keeps within {@code range} of. */
    public void setHome(BlockPos home, int range) {
        this.home = home.toImmutable();
        this.homeRange = range;
        this.setPositionTarget(this.home, range);
    }

    public int getHomeRange() {
        return this.homeRange;
    }

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    /** The hold this orc belongs to (its structure start, {@code ChunkPos.toLong}), or {@link #NO_HOLD}. */
    public long getHold() {
        return this.hold;
    }

    public void setHold(long hold) {
        this.hold = hold;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.setVariant(random.nextInt(VARIANTS));
        this.setCustomName(Text.literal(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + EPITHETS[random.nextInt(EPITHETS.length)]));
        this.setPersistent();
        if (this.home == null) {
            this.setHome(this.getBlockPos(), 16);
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // ---- Who it fights ----

    // The mod gives every mob a player-targeting goal; orcs only fight whom mayTarget allows.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == null || this.mayTarget(target, target == this.getTarget())) {
            super.setTarget(target);
        }
    }

    /** Whether this orc may fight {@code target}; {@code engaged} when it's already fighting it. */
    protected boolean mayTarget(LivingEntity target, boolean engaged) {
        if (target instanceof PlayerEntity player) {
            return OrcHostility.mayFight(this, player, engaged);
        }
        return !OrcHostility.isAlly(target);
    }

    /**
     * This orc saw {@code player} wrong the hold, or strike one of its folk, and turns on them; not while they fight
     * the Trial of Strength (except the Champion).
     */
    @Override
    public void provoke(PlayerEntity player) {
        if (OrcTrial.isDueling(player) && !(this instanceof OrcChampionEntity)) {
            return;
        }
        GuardAnger.super.provoke(player);
    }

    @Override
    public void provokeShout() {
        this.playSound(SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY, 1.0F, this.getSoundPitch());
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // No friendly fire between the horde and its goblin allies.
        if (source.getAttacker() != null && OrcHostility.isAlly(source.getAttacker())) {
            return false;
        }
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient && source.getAttacker() instanceof PlayerEntity player
                && !player.isCreative() && !player.isSpectator() && !OrcTrial.isDuelBlow(this, player)) {
            this.provoke(player);
            rally(this, player);
        }
        return hurt;
    }

    /** Every orc near {@code orc} turns on {@code player}, who struck it. */
    static void rally(OrcEntity orc, PlayerEntity player) {
        for (OrcEntity other : orc.world.getEntitiesByClass(OrcEntity.class, new Box(orc.getBlockPos()).expand(RALLY_RANGE),
                o -> o != orc && o.isAlive() && !(o instanceof OrcChampionEntity))) {
            other.provoke(player);
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!this.world.isClient) {
            this.tickAngerLogic((ServerWorld) this.world, true);
            // Stand down if the player's standing changed mid-fight, or they became Blood-Sworn.
            if (this.age % 20 == 0 && this.getTarget() instanceof PlayerEntity player && !this.mayTarget(player, true)) {
                super.setTarget(null);
            }
            if (this.hold == NO_HOLD && this.age % 100 == 0) {
                RacialHomes.Visit visit = RacialHomes.visitOf((ServerWorld) this.world,
                        this.home != null ? this.home : this.getBlockPos(), RacialHomes.ORC_STRONGHOLD);
                if (visit != null) {
                    this.hold = visit.id();
                }
            }
        }
    }

    // ---- Angerable ----

    @Override
    public GuardAnger.State angerState() {
        return this.anger;
    }

    // ---- Sounds: a piglin brute's, deeper ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 0.8F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PIGLIN_BRUTE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PIGLIN_BRUTE_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getVariant());
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
            nbt.putInt("HomeRange", this.homeRange);
        }
        nbt.putLong("Hold", this.hold);
        this.writeAngerToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setVariant(nbt.getInt("Variant"));
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")), nbt.contains("HomeRange") ? nbt.getInt("HomeRange") : 16);
        }
        if (nbt.contains("Hold")) {
            this.hold = nbt.getLong("Hold");
        }
        this.readAngerFromNbt(this.world, nbt);
    }

    /** Villagers count as the horde's enemies too; used by {@link OrcHostility#isHordeEnemy}. */
    static boolean isVillager(LivingEntity entity) {
        return entity instanceof MerchantEntity;
    }
}
