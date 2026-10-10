package mattonfire.dnd.entity;

import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
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
 * An elf of the elven enclaves. This class is the Wood Elf, the enclave's ordinary folk: they wander the
 * platforms and the glade by day, keep near home at night, and run from monsters. Wardens
 * ({@link ElfWardenEntity}) guard them and the merchants ({@link ElfMerchantEntity}) trade. Strike any elf
 * and the Wardens nearby turn on you; what else angers them is Sylvan Law ({@link SylvanLaw}).
 */
public class ElfEntity extends PathAwareEntity {
    public static final int VARIANTS = 6;
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(ElfEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final int DAY_RANGE = 24;
    private static final int NIGHT_RANGE = 8;
    /** Wardens this close to a struck elf come to its defence. */
    static final double DEFENCE_RANGE = 16.0D;

    private static final String[] FIRST_NAMES = {
            "Aelar", "Arannis", "Berrian", "Caelynn", "Carric", "Enna", "Erevan", "Galinndan", "Hadarai", "Heian",
            "Ivellios", "Keyleth", "Laucian", "Lia", "Mialee", "Naivara", "Quelenna", "Quarion", "Riardon", "Sariel",
            "Shava", "Soveliss", "Thamior", "Tharivol", "Theren", "Thia", "Valanthe", "Varis", "Adrie", "Birel"
    };
    private static final String[] SURNAMES = {
            "Galanodel", "Amakiir", "Holimion", "Ilphelkiir", "Liadon", "Meliamne", "Nailo", "Siannodel",
            "Xiloscient", "Amastacia", "Brightwood", "Moonwhisper", "Starbloom", "Silverfrond", "Evenwood"
    };

    @Nullable
    private BlockPos home;

    public ElfEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        this.getNavigation().setCanSwim(true);
    }

    public static DefaultAttributeContainer.Builder createElfAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.4D));
        this.goalSelector.add(2, new FleeEntityGoal<>(this, HostileEntity.class, 10.0F, 0.9D, 1.3D));
        // Players Hostile with the Sylvan Court are kept at a distance.
        this.goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 8.0F, 0.9D, 1.3D,
                player -> TierEffects.fleesFrom(TierEffects.tierWith((PlayerEntity) player, this))));
        this.goalSelector.add(3, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(4, new GoToWalkTargetGoal(this, 0.8D));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(6, new LookAtEntityGoal(this, ElfEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    /** Ties the elf to a spot it wanders around by day and keeps near at night. */
    public void setHome(BlockPos home) {
        this.home = home.toImmutable();
        this.updateRange();
    }

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.dataTracker.set(VARIANT, random.nextInt(VARIANTS));
        this.setCustomName(Text.literal(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + SURNAMES[random.nextInt(SURNAMES.length)]));
        this.setPersistent();
        this.setHome(this.getBlockPos());
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // The mod gives every mob a player-targeting goal; elves only fight whom mayTarget allows.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == null || this.mayTarget(target)) {
            super.setTarget(target);
        }
    }

    /** Whether this elf may fight {@code target}. Wood elves and merchants never pick fights. */
    protected boolean mayTarget(LivingEntity target) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // A Warden's stray arrow doesn't hurt its own folk.
        if (source.getSource() instanceof ProjectileEntity projectile && projectile.getOwner() instanceof ElfEntity) {
            return false;
        }
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient && source.getAttacker() instanceof PlayerEntity player
                && !player.isCreative() && !player.isSpectator()) {
            defend(this, player);
        }
        return hurt;
    }

    /** Every Warden near {@code elf} turns on {@code player}, who struck it. */
    static void defend(Entity elf, PlayerEntity player) {
        for (ElfWardenEntity warden : elf.world.getEntitiesByClass(ElfWardenEntity.class,
                new Box(elf.getBlockPos()).expand(DEFENCE_RANGE), LivingEntity::isAlive)) {
            warden.provoke(player);
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.age % 100 == 0) {
            this.updateRange();
        }
    }

    private void updateRange() {
        if (this.home != null) {
            this.setPositionTarget(this.home, this.world.isDay() ? this.dayRange() : this.nightRange());
        }
    }

    /** How far from home this elf roams by day. */
    protected int dayRange() {
        return DAY_RANGE;
    }

    /** How far from home this elf roams at night. */
    protected int nightRange() {
        return NIGHT_RANGE;
    }

    // ---- Sounds: a villager's, lighter ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 1.2F;
    }

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
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(VARIANT, Math.floorMod(nbt.getInt("Variant"), VARIANTS));
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")));
        }
    }
}
