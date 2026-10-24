package mattonfire.dnd.entity;

import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Misc.BardCompanions;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.entity.hireling.HirelingAbilities;
import mattonfire.dnd.entity.hireling.HirelingGoals;
import mattonfire.dnd.entity.hireling.HirelingRace;
import mattonfire.dnd.entity.hireling.HirelingRegistry;
import mattonfire.dnd.entity.hireling.HirelingRole;
import mattonfire.dnd.entity.hireling.HirelingWages;
import mattonfire.dnd.entity.hireling.Hirelings;
import mattonfire.dnd.world.GameTime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * An adventurer for hire from a Hobbit Tavern ({@code docs/systems/hirelings.md}).
 *
 * <p>A candidate stands in its inn ({@link #isForHire}) until a player hires it with gold
 * ({@link Hirelings#hire}). A hired hireling follows its owner or waits where told, fights for them, and
 * stabilises them when they're Downed. It costs a daily wage from its purse; unpaid days and bad treatment
 * cost loyalty, and it leaves at 0 loyalty or after two unpaid days running. A blow that would kill it
 * knocks it out instead ({@link #knockOut}).
 *
 * <p>Each role has an automatic ability ({@link HirelingAbilities}) and counts for its owner's party
 * synergies ({@code PartySynergies}).
 */
public class HirelingEntity extends PathAwareEntity {
    private static final TrackedData<Integer> ROLE = DataTracker.registerData(HirelingEntity.class,
            TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> RACE = DataTracker.registerData(HirelingEntity.class,
            TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(HirelingEntity.class,
            TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Byte> FLAGS = DataTracker.registerData(HirelingEntity.class,
            TrackedDataHandlerRegistry.BYTE);

    public static final byte FLAG_FOR_HIRE = 1;
    public static final byte FLAG_WAITING = 2;
    public static final byte FLAG_KNOCKED_OUT = 4;
    public static final byte FLAG_DEPARTING = 8;

    public static final int START_LOYALTY = 50;
    public static final int DEVOTED_LOYALTY = 80;
    public static final float DEVOTED_BONUS = 1.10F;
    public static final int LOYALTY_PAID_DAY = 5;
    public static final int LOYALTY_KILL = 1;
    public static final int LOYALTY_KILLS_PER_DAY = 5;
    public static final int LOYALTY_UNPAID_DAY = -25;
    public static final int LOYALTY_KNOCKED_OUT = -10;
    public static final int LOYALTY_HIT_BY_OWNER = -20;
    public static final int LOYALTY_KIN_KILLED = -50;
    /** Unpaid days in a row before it leaves. */
    public static final int UNPAID_DAYS_TO_LEAVE = 2;
    public static final int KNOCKED_OUT_TICKS = 600;
    /** Gets up from a knock-out with this share of its health. */
    public static final float WAKE_HEALTH = 0.25F;
    /** Dies for good if knocked out with its owner further than this the whole time. */
    public static final double OWNER_NEAR = 64.0D;
    public static final int DEPART_TICKS = 200;
    /** A Wait-mode hireling fights hostiles this close, then walks back to its spot. */
    public static final double WAIT_GUARD_RANGE = 8.0D;
    /** Engages a monster this close that targets its owner, a party member or a hireling. */
    public static final double GUARD_RANGE = 10.0D;

    @Nullable
    private UUID owner;
    private String name = "";
    private int purse;
    private int loyalty = START_LOYALTY;
    private long paidThrough;
    private int unpaidStreak;
    @Nullable
    private BlockPos post;
    private int knockedOutTicks;
    /** Whether the owner has been away (more than {@link #OWNER_NEAR}) for the whole knock-out so far. */
    private boolean ownerAwayAllKnockout;
    private int killLoyalty;
    private long killLoyaltyDay = -1;
    @Nullable
    private UUID rosterInn;
    private long rosterDay = -1;
    private int departTicks;
    @Nullable
    private Vec3d departTo;
    private int generation;
    private boolean leaveWhenUp;
    /** Ticks until its role ability is ready again ({@link HirelingAbilities}). */
    private int abilityCooldown;
    /** The Guide's torch stock. */
    private int torches = HirelingAbilities.TORCH_STOCK;
    /** Blocks walked in the dark since the Guide's last torch (not saved). */
    public double torchWalked;
    /** Where it was last tick, for the Guide's torch distance (not saved). */
    @Nullable
    public Vec3d lastTorchPos;

    public HirelingEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        this.getNavigation().setCanSwim(true);
        this.setCanPickUpLoot(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setEquipmentDropChance(slot, 0.0F);
        }
    }

    public static DefaultAttributeContainer.Builder createHirelingAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ROLE, 0);
        this.dataTracker.startTracking(RACE, 0);
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(FLAGS, FLAG_FOR_HIRE);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(0, new HirelingGoals.KnockedOutGoal(this));
        this.goalSelector.add(1, new HirelingGoals.StabiliseGoal(this));
        this.goalSelector.add(2, new HirelingGoals.BowGoal(this));
        this.goalSelector.add(2, new HirelingGoals.MeleeGoal(this));
        this.goalSelector.add(3, new HirelingGoals.FollowOwnerGoal(this));
        this.goalSelector.add(3, new HirelingGoals.ReturnToPostGoal(this));
        this.goalSelector.add(4, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(5, new HirelingGoals.WanderInnGoal(this));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new HirelingGoals.RevengeTargetGoal(this));
        this.targetSelector.add(2, new HirelingGoals.DefendOwnerGoal(this));
        this.targetSelector.add(3, new HirelingGoals.GuardAlliesGoal(this));
        this.targetSelector.add(4, new HirelingGoals.WaitGuardGoal(this));
    }

    @Override
    public net.minecraft.entity.EntityData initialize(net.minecraft.world.ServerWorldAccess world,
                                                      net.minecraft.world.LocalDifficulty difficulty,
                                                      net.minecraft.entity.SpawnReason spawnReason,
                                                      @Nullable net.minecraft.entity.EntityData entityData,
                                                      @Nullable NbtCompound entityNbt) {
        net.minecraft.entity.EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.name.isEmpty()) {
            // A spawn egg or /summon: a random candidate (with no inn, so it stays until hired).
            HirelingRole role = HirelingRole.values()[this.random.nextInt(HirelingRole.values().length)];
            HirelingRace race = HirelingRace.pick(this.random);
            this.setup(role, race, this.random.nextInt(race.skins), mattonfire.dnd.entity.hireling.HirelingNames.pick(race, this.random));
        }
        return data;
    }

    // ---- Who it is ----

    /** Sets up a new hireling: role, race, skin, name and the role's gear. */
    public void setup(HirelingRole role, HirelingRace race, int variant, String name) {
        this.dataTracker.set(ROLE, role.ordinal());
        this.dataTracker.set(RACE, race.ordinal());
        this.dataTracker.set(VARIANT, Math.floorMod(variant, race.skins));
        this.name = name;
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(role.health);
        this.setHealth(role.health);
        this.equipFor(role);
        this.refreshName();
        this.setPersistent();
    }

    private void equipFor(HirelingRole role) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.equipStack(slot, ItemStack.EMPTY);
        }
        switch (role) {
            case TANK -> {
                this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case HEALER -> {
                this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
            }
            case DAMAGE -> {
                this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            }
            case SUPPORT -> {
                this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.LUTE));
            }
            case UTILITY -> {
                this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.LANTERN));
            }
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setEquipmentDropChance(slot, 0.0F);
        }
    }

    /** "Brannoc Ashdown, Shieldbearer". */
    private void refreshName() {
        this.setCustomName(Text.literal(this.name + ", ").append(this.role().title()));
    }

    public HirelingRole role() {
        return HirelingRole.byOrdinal(this.dataTracker.get(ROLE));
    }

    public HirelingRace race() {
        return HirelingRace.byOrdinal(this.dataTracker.get(RACE));
    }

    public int variant() {
        return this.dataTracker.get(VARIANT);
    }

    /** Full name without the title, e.g. "Brannoc Ashdown". */
    public String hirelingName() {
        return this.name;
    }

    /** First name, e.g. "Brannoc": what messages and commands use. */
    public String firstName() {
        int space = this.name.indexOf(' ');
        return space < 0 ? this.name : this.name.substring(0, space);
    }

    // ---- State ----

    private boolean flag(byte flag) {
        return (this.dataTracker.get(FLAGS) & flag) != 0;
    }

    private void setFlag(byte flag, boolean on) {
        byte flags = this.dataTracker.get(FLAGS);
        this.dataTracker.set(FLAGS, (byte) (on ? flags | flag : flags & ~flag));
    }

    public boolean isForHire() {
        return this.flag(FLAG_FOR_HIRE);
    }

    public boolean isWaiting() {
        return this.flag(FLAG_WAITING);
    }

    public boolean isKnockedOut() {
        return this.flag(FLAG_KNOCKED_OUT);
    }

    public boolean isDeparting() {
        return this.flag(FLAG_DEPARTING);
    }

    /** Hired, not leaving. */
    public boolean isHired() {
        return this.owner != null && !this.isForHire() && !this.isDeparting();
    }

    /** Hired and on its feet. */
    public boolean isActive() {
        return this.isHired() && !this.isKnockedOut();
    }

    @Nullable
    public UUID ownerUuid() {
        return this.owner;
    }

    public boolean isOwner(@Nullable Entity entity) {
        return entity != null && this.owner != null && this.owner.equals(entity.getUuid());
    }

    /** The owner, if online (any dimension). */
    @Nullable
    public ServerPlayerEntity ownerPlayer() {
        if (this.owner == null || this.world.getServer() == null) {
            return null;
        }
        return this.world.getServer().getPlayerManager().getPlayer(this.owner);
    }

    /** The owner, if online and in this hireling's world. */
    @Nullable
    public ServerPlayerEntity ownerHere() {
        ServerPlayerEntity player = this.ownerPlayer();
        return player != null && player.world == this.world ? player : null;
    }

    public int purse() {
        return this.purse;
    }

    public void setPurse(int nuggets) {
        this.purse = Math.max(0, nuggets);
    }

    public int loyalty() {
        return this.loyalty;
    }

    public boolean isDevoted() {
        return this.loyalty >= DEVOTED_LOYALTY;
    }

    /** x1.1 while Devoted. */
    public float devotedBonus() {
        return this.isDevoted() ? DEVOTED_BONUS : 1.0F;
    }

    @Nullable
    public BlockPos post() {
        return this.post;
    }

    @Nullable
    public UUID rosterInn() {
        return this.rosterInn;
    }

    public long rosterDay() {
        return this.rosterDay;
    }

    public int generation() {
        return this.generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public int unpaidStreak() {
        return this.unpaidStreak;
    }

    public int abilityCooldown() {
        return this.abilityCooldown;
    }

    public void setAbilityCooldown(int ticks) {
        this.abilityCooldown = Math.max(0, ticks);
    }

    public int torches() {
        return this.torches;
    }

    public void setTorches(int torches) {
        this.torches = MathHelper.clamp(torches, 0, HirelingAbilities.TORCH_STOCK);
    }

    /** Days of wages left in the purse (for the owner's wage). */
    public int daysPaid() {
        ServerPlayerEntity player = this.ownerPlayer();
        int wage = player != null ? HirelingWages.wage(this.role(), player) : this.role().wage;
        return this.purse / Math.max(1, wage);
    }

    /** A candidate at an inn, for one day. */
    public void makeCandidate(UUID inn, long day, BlockPos spot) {
        this.setFlag(FLAG_FOR_HIRE, true);
        this.rosterInn = inn;
        this.rosterDay = day;
        this.setPositionTarget(spot, 5);
    }

    /** Hired by {@code player}, with {@code purse} nuggets already paid in. */
    public void hireBy(ServerPlayerEntity player, int purse) {
        this.owner = player.getUuid();
        this.setFlag(FLAG_FOR_HIRE, false);
        this.setFlag(FLAG_WAITING, false);
        this.rosterInn = null;
        this.rosterDay = -1;
        this.purse = purse;
        this.loyalty = START_LOYALTY;
        this.unpaidStreak = 0;
        this.paidThrough = GameTime.day(player.getServer());
        this.setPositionTarget(BlockPos.ORIGIN, -1);
        this.setTarget(null);
        this.setPersistent();
    }

    public void setWaiting(boolean waiting) {
        this.setFlag(FLAG_WAITING, waiting);
        this.post = waiting ? this.getBlockPos() : null;
        this.getNavigation().stop();
        this.setTarget(null);
        Hirelings.refreshRecord(this);
    }

    // ---- Loyalty and wages ----

    /** Changes loyalty; at 0 it leaves (once it's back on its feet). */
    public void changeLoyalty(int delta, String why) {
        int before = this.loyalty;
        this.loyalty = MathHelper.clamp(this.loyalty + delta, 0, 100);
        DnDClasses.LOGGER.info("[Hireling] {} loyalty {} -> {} ({})", this.firstName(), before, this.loyalty, why);
        if (this.loyalty <= 0 && this.isHired()) {
            if (this.isKnockedOut()) {
                this.leaveWhenUp = true;
            } else {
                this.leave(true);
            }
        }
    }

    /** A kill it made or helped with: +1 loyalty, at most +5 a day. */
    public void creditKill(LivingEntity victim) {
        if (!this.isHired()) {
            return;
        }
        long today = GameTime.day(this.world);
        if (this.killLoyaltyDay != today) {
            this.killLoyaltyDay = today;
            this.killLoyalty = 0;
        }
        if (this.killLoyalty < LOYALTY_KILLS_PER_DAY) {
            this.killLoyalty++;
            this.changeLoyalty(LOYALTY_KILL, "kill: " + victim.getType().getUntranslatedName());
        }
    }

    /** Charges the wages for every day since the last one paid, while its owner is online. */
    private void wageTick() {
        ServerPlayerEntity player = this.ownerPlayer();
        if (player == null || this.world.getServer() == null) {
            return;
        }
        long today = GameTime.day(this.world.getServer());
        long joined = HirelingRegistry.get(this.world.getServer()).joinDay(player.getUuid());
        if (this.paidThrough < joined) {
            // Days the owner spent offline are free.
            this.paidThrough = joined;
        }
        while (this.paidThrough < today && this.isHired()) {
            this.paidThrough++;
            this.payDay(player);
        }
    }

    /** One wage day: paid from the purse (+5 loyalty), or unpaid (-25, and it leaves after two in a row). */
    public void payDay(ServerPlayerEntity player) {
        int wage = HirelingWages.wage(this.role(), player);
        if (this.purse >= wage) {
            this.purse -= wage;
            this.unpaidStreak = 0;
            DnDClasses.LOGGER.info("[Hireling] {} paid {} (purse {})", this.firstName(), wage, this.purse);
            this.changeLoyalty(LOYALTY_PAID_DAY, "paid day");
        } else {
            this.unpaidStreak++;
            player.sendMessage(Text.translatable("hireling.dndclasses.unpaid", this.firstName(), this.unpaidStreak)
                    .formatted(Formatting.RED), false);
            DnDClasses.LOGGER.info("[Hireling] {} unpaid day {} (purse {})", this.firstName(), this.unpaidStreak,
                    this.purse);
            this.changeLoyalty(LOYALTY_UNPAID_DAY, "unpaid day");
            if (this.unpaidStreak >= UNPAID_DAYS_TO_LEAVE && this.isHired()) {
                this.leave(true);
            }
        }
        Hirelings.refreshRecord(this);
    }

    /**
     * Leaves its owner's service: walks off and is gone after {@value #DEPART_TICKS} ticks. {@code unhappy}: "has
     * had enough"; otherwise it was dismissed. The purse isn't refunded.
     */
    public void leave(boolean unhappy) {
        if (!this.isHired()) {
            return;
        }
        ServerPlayerEntity player = this.ownerPlayer();
        if (player != null) {
            player.sendMessage(Text.translatable(unhappy ? "hireling.dndclasses.leaves" : "hireling.dndclasses.dismissed",
                    this.firstName()).formatted(unhappy ? Formatting.RED : Formatting.YELLOW), false);
            Vec3d away = this.getPos().subtract(player.getPos()).multiply(1, 0, 1);
            if (away.lengthSquared() < 0.01D) {
                away = new Vec3d(1, 0, 0);
            }
            this.departTo = this.getPos().add(away.normalize().multiply(24.0D));
        }
        DnDClasses.LOGGER.info("[Hireling] {} leaves {}'s service ({})", this.firstName(),
                player != null ? player.getEntityName() : this.owner, unhappy ? "unhappy" : "dismissed");
        Hirelings.forget(this);
        this.setFlag(FLAG_DEPARTING, true);
        this.setFlag(FLAG_WAITING, false);
        this.departTicks = DEPART_TICKS;
        this.setTarget(null);
    }

    // ---- Knocked out ----

    /** A blow that would have killed it: down at 1 HP for {@value #KNOCKED_OUT_TICKS} ticks. */
    public void knockOut() {
        if (this.isKnockedOut()) {
            this.setHealth(1.0F);
            return;
        }
        this.setFlag(FLAG_KNOCKED_OUT, true);
        this.knockedOutTicks = KNOCKED_OUT_TICKS;
        this.ownerAwayAllKnockout = true;
        this.setHealth(1.0F);
        this.setPose(EntityPose.SLEEPING);
        this.getNavigation().stop();
        this.setTarget(null);
        this.clearActiveItem();
        this.extinguish();
        ServerPlayerEntity player = this.ownerPlayer();
        if (player != null) {
            player.sendMessage(Text.translatable("hireling.dndclasses.knocked_out", this.firstName())
                    .formatted(Formatting.RED), true);
        }
        DnDClasses.LOGGER.info("[Hireling] {} knocked out", this.firstName());
        this.changeLoyalty(LOYALTY_KNOCKED_OUT, "knocked out");
    }

    /** Back on its feet with a quarter of its health. */
    public void wakeUp() {
        if (!this.isKnockedOut()) {
            return;
        }
        this.setFlag(FLAG_KNOCKED_OUT, false);
        this.knockedOutTicks = 0;
        this.setPose(EntityPose.STANDING);
        this.setHealth(Math.max(1.0F, this.getMaxHealth() * WAKE_HEALTH));
        DnDClasses.LOGGER.info("[Hireling] {} is back up ({} HP)", this.firstName(), this.getHealth());
        if (this.leaveWhenUp || this.loyalty <= 0) {
            this.leaveWhenUp = false;
            this.leave(true);
        }
    }

    private void knockedOutTick() {
        if (this.getPose() != EntityPose.SLEEPING) {
            this.setPose(EntityPose.SLEEPING);
        }
        ServerPlayerEntity player = this.ownerPlayer();
        // An offline owner doesn't count as away: they can't come back in time.
        if (player == null || player.world == this.world && player.squaredDistanceTo(this) <= OWNER_NEAR * OWNER_NEAR) {
            this.ownerAwayAllKnockout = false;
        }
        if (--this.knockedOutTicks > 0) {
            return;
        }
        if (this.ownerAwayAllKnockout && this.isHired()) {
            if (player != null) {
                player.sendMessage(Text.translatable("hireling.dndclasses.died", this.firstName())
                        .formatted(Formatting.DARK_RED), false);
            }
            DnDClasses.LOGGER.info("[Hireling] {} didn't make it (owner away)", this.firstName());
            Hirelings.forget(this);
            this.discard();
            return;
        }
        this.wakeUp();
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        // Lying down: a low box that is still easy to click with food.
        return pose == EntityPose.SLEEPING ? EntityDimensions.changing(0.9F, 0.5F) : super.getDimensions(pose);
    }

    @Override
    public boolean canTakeDamage() {
        // Mobs leave a knocked-out hireling alone (and TargetPredicate skips it).
        return !this.isKnockedOut() && super.canTakeDamage();
    }

    // ---- Ticking ----

    @Override
    public void tick() {
        super.tick();
        if (this.world.isClient) {
            return;
        }
        if (this.isDeparting()) {
            this.departingTick();
            return;
        }
        if (this.isForHire()) {
            if (this.rosterDay >= 0 && this.age % 100 == 0 && GameTime.day(this.world) != this.rosterDay) {
                // Yesterday's candidate, unhired: gone (the innkeeper calls new ones).
                this.discard();
            }
            return;
        }
        if (this.isKnockedOut()) {
            this.knockedOutTick();
        }
        if (this.isActive()) {
            HirelingAbilities.tick(this);
        }
        if (this.age % 20 == 0) {
            this.wageTick();
        }
        if (this.age % 100 == 0) {
            Hirelings.refreshRecord(this);
        }
    }

    private void departingTick() {
        if (this.isKnockedOut()) {
            this.setFlag(FLAG_KNOCKED_OUT, false);
            this.setPose(EntityPose.STANDING);
        }
        if (this.departTo != null && this.age % 20 == 0) {
            this.getNavigation().startMovingTo(this.departTo.x, this.departTo.y, this.departTo.z, 1.0D);
        }
        if (--this.departTicks <= 0) {
            this.discard();
        }
    }

    // ---- Fighting ----

    @Override
    public boolean canTarget(LivingEntity target) {
        if (!this.isActive() || target instanceof PlayerEntity || target instanceof HirelingEntity
                || target instanceof MerchantEntity || target instanceof SettlementNpcEntity
                || target instanceof IronGolemEntity || BardCompanions.isCompanion(target)
                || target instanceof Tameable tameable && tameable.getOwnerUuid() != null) {
            return false;
        }
        // Only the archer goes for creepers, from range.
        if (target instanceof CreeperEntity && this.role() != HirelingRole.DAMAGE) {
            return false;
        }
        return super.canTarget(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && !this.canTarget(target)) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean tryAttack(Entity target) {
        float damage = this.role().melee * this.devotedBonus();
        boolean hit = target.damage(this.getDamageSources().mobAttack(this), damage);
        if (hit) {
            this.applyDamageEffects(this, target);
            this.onAttacking(target);
            if (target instanceof LivingEntity living) {
                living.takeKnockback(0.3D, MathHelper.sin(this.getYaw() * 0.017453292F),
                        -MathHelper.cos(this.getYaw() * 0.017453292F));
            }
        }
        return hit;
    }

    /** The Sellsword's arrow: 4-6 damage (x1.1 Devoted). */
    public void shootArrow(LivingEntity target, float pull) {
        PersistentProjectileEntity arrow = ProjectileUtil.createArrowProjectile(this, new ItemStack(Items.ARROW), pull);
        double dx = target.getX() - this.getX();
        double dy = target.getBodyY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.setVelocity(dx, dy + flat * 0.2D, dz, 1.6F, 2.0F);
        arrow.setDamage((2.5D + this.random.nextDouble() * 1.25D) * this.devotedBonus());
        arrow.setCritical(false);
        arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
        this.world.spawnEntity(arrow);
    }

    /** Puts the bow or the sword in the Sellsword's hand. */
    public void holdForRange(boolean bow) {
        if (this.role() != HirelingRole.DAMAGE) {
            return;
        }
        ItemStack held = this.getMainHandStack();
        if (bow && !held.isOf(Items.BOW)) {
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        } else if (!bow && !held.isOf(Items.IRON_SWORD)) {
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.world.isClient) {
            return false;
        }
        if (this.isKnockedOut() && !Hirelings.bypasses(source, amount)) {
            return false;
        }
        // The Shieldbearer takes half from melee blows in front of it.
        Entity attacker = source.getAttacker();
        if (this.role() == HirelingRole.TANK && attacker != null && source.getSource() == attacker
                && !source.isIn(DamageTypeTags.BYPASSES_SHIELD)
                && (source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.PLAYER_ATTACK)
                || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO))) {
            Vec3d toAttacker = attacker.getPos().subtract(this.getPos()).normalize();
            if (toAttacker.dotProduct(this.getRotationVec(1.0F).multiply(1, 0, 1).normalize()) > 0.0D) {
                amount *= 0.5F;
                this.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 0.8F, 0.9F + this.random.nextFloat() * 0.2F);
            }
        }
        return super.damage(source, amount);
    }

    @Override
    public void onDeath(DamageSource source) {
        if (!this.world.isClient && this.isHired()) {
            ServerPlayerEntity player = this.ownerPlayer();
            if (player != null) {
                player.sendMessage(Text.translatable("hireling.dndclasses.died", this.firstName())
                        .formatted(Formatting.DARK_RED), false);
            }
            DnDClasses.LOGGER.info("[Hireling] {} died ({})", this.firstName(), source.getName());
            Hirelings.forget(this);
        }
        super.onDeath(source);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    // ---- Talking to it ----

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        ItemStack held = player.getStackInHand(hand);
        if (this.world.isClient) {
            return held.isEmpty() || HirelingWages.isGold(held) || held.isFood()
                    || held.isOf(Items.TORCH) && this.role() == HirelingRole.UTILITY ? ActionResult.SUCCESS : ActionResult.PASS;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        this.getLookControl().lookAt(player);
        if (this.isForHire()) {
            if (held.isEmpty()) {
                Hirelings.sendHireCard(serverPlayer, this);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }
        if (!this.isHired()) {
            return ActionResult.PASS;
        }
        boolean friend = this.isOwner(player) || Hirelings.sameParty(serverPlayer.getServer(), this.owner, player.getUuid());
        if (this.isKnockedOut() && friend && (held.isFood() || isHealingPotion(held))) {
            if (!player.getAbilities().creativeMode) {
                held.decrement(1);
                if (held.isOf(Items.POTION)) {
                    player.getInventory().offerOrDrop(new ItemStack(Items.GLASS_BOTTLE));
                }
            }
            DnDClasses.LOGGER.info("[Hireling] {} woken by {}", this.firstName(), player.getEntityName());
            this.wakeUp();
            this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.8F, 1.0F);
            return ActionResult.SUCCESS;
        }
        if (this.isOwner(player)) {
            if (held.isEmpty() && player.isSneaking()) {
                this.setWaiting(!this.isWaiting());
                player.sendMessage(Text.translatable(this.isWaiting() ? "hireling.dndclasses.waits" : "hireling.dndclasses.follows",
                        this.firstName()).formatted(Formatting.YELLOW), true);
                return ActionResult.SUCCESS;
            }
            if (HirelingWages.isGold(held)) {
                Hirelings.payFromHand(serverPlayer, this, held);
                return ActionResult.SUCCESS;
            }
            if (HirelingAbilities.refillTorches(serverPlayer, this, held)) {
                return ActionResult.SUCCESS;
            }
            if (held.isEmpty()) {
                Hirelings.sendStatusCard(serverPlayer, this);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }
        if (held.isEmpty()) {
            ServerPlayerEntity owner = this.ownerPlayer();
            player.sendMessage(Text.translatable("hireling.dndclasses.serves", this.getDisplayName(),
                    owner != null ? owner.getDisplayName() : Text.literal("someone")).formatted(Formatting.GRAY), false);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private static boolean isHealingPotion(ItemStack stack) {
        return stack.isOf(Items.POTION) && (PotionUtil.getPotion(stack) == Potions.HEALING
                || PotionUtil.getPotion(stack) == Potions.STRONG_HEALING);
    }

    // ---- Saving ----

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("Role", this.role().id());
        nbt.putString("Race", this.race().id());
        nbt.putInt("Variant", this.variant());
        nbt.putByte("HirelingFlags", this.dataTracker.get(FLAGS));
        nbt.putString("HirelingName", this.name);
        if (this.owner != null) {
            nbt.putUuid("Owner", this.owner);
        }
        nbt.putInt("Purse", this.purse);
        nbt.putInt("Loyalty", this.loyalty);
        nbt.putLong("PaidThrough", this.paidThrough);
        nbt.putInt("UnpaidStreak", this.unpaidStreak);
        if (this.post != null) {
            nbt.put("Post", NbtHelper.fromBlockPos(this.post));
        }
        nbt.putInt("KnockedOutTicks", this.knockedOutTicks);
        nbt.putBoolean("OwnerAwayAllKnockout", this.ownerAwayAllKnockout);
        nbt.putInt("KillLoyalty", this.killLoyalty);
        nbt.putLong("KillLoyaltyDay", this.killLoyaltyDay);
        if (this.rosterInn != null) {
            nbt.putUuid("RosterInn", this.rosterInn);
        }
        nbt.putLong("RosterDay", this.rosterDay);
        nbt.putInt("DepartTicks", this.departTicks);
        nbt.putInt("Generation", this.generation);
        nbt.putBoolean("LeaveWhenUp", this.leaveWhenUp);
        nbt.putInt("AbilityCooldown", this.abilityCooldown);
        nbt.putInt("Torches", this.torches);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        HirelingRole role = HirelingRole.byId(nbt.getString("Role"));
        HirelingRace race = HirelingRace.byId(nbt.getString("Race"));
        this.dataTracker.set(ROLE, (role != null ? role : HirelingRole.TANK).ordinal());
        this.dataTracker.set(RACE, (race != null ? race : HirelingRace.HUMAN).ordinal());
        this.dataTracker.set(VARIANT, nbt.getInt("Variant"));
        this.dataTracker.set(FLAGS, nbt.getByte("HirelingFlags"));
        this.name = nbt.getString("HirelingName");
        this.owner = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        this.purse = nbt.getInt("Purse");
        this.loyalty = nbt.contains("Loyalty") ? nbt.getInt("Loyalty") : START_LOYALTY;
        this.paidThrough = nbt.getLong("PaidThrough");
        this.unpaidStreak = nbt.getInt("UnpaidStreak");
        this.post = nbt.contains("Post") ? NbtHelper.toBlockPos(nbt.getCompound("Post")) : null;
        this.knockedOutTicks = nbt.getInt("KnockedOutTicks");
        this.ownerAwayAllKnockout = nbt.getBoolean("OwnerAwayAllKnockout");
        this.killLoyalty = nbt.getInt("KillLoyalty");
        this.killLoyaltyDay = nbt.getLong("KillLoyaltyDay");
        this.rosterInn = nbt.containsUuid("RosterInn") ? nbt.getUuid("RosterInn") : null;
        this.rosterDay = nbt.contains("RosterDay") ? nbt.getLong("RosterDay") : -1;
        this.departTicks = nbt.getInt("DepartTicks");
        this.generation = nbt.getInt("Generation");
        this.leaveWhenUp = nbt.getBoolean("LeaveWhenUp");
        this.abilityCooldown = nbt.getInt("AbilityCooldown");
        this.torches = nbt.contains("Torches") ? nbt.getInt("Torches") : HirelingAbilities.TORCH_STOCK;
        if (this.isKnockedOut()) {
            this.setPose(EntityPose.SLEEPING);
        }
    }

    /** For the HUD and the list: is it in a world that's loaded (true for any entity instance). */
    public ServerWorld serverWorld() {
        return (ServerWorld) this.world;
    }
}
