package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
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
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
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
 * A Dragonborn of the Dragon Temples. The base class holds what they all share: a scale colour (red, white,
 * blue, bronze or gold) that sets their ancestry, a name, a home spot they keep near, and the rule that
 * striking one brings every Templar nearby down on you. Templars ({@link DragonbornTemplarEntity}) guard the
 * temple; the Elder ({@link DragonbornElderEntity}) trades and gives quests. Neutral to everyone.
 */
public abstract class DragonbornEntity extends PathAwareEntity {
    /** Scale colours: red, white, blue, bronze, gold. */
    public static final int VARIANTS = 5;
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(DragonbornEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Templars this close to a struck Dragonborn come to its defence. */
    static final double DEFENCE_RANGE = 16.0D;

    private static final String[] FIRST_NAMES = {
            "Arjhan", "Balasar", "Bharash", "Donaar", "Ghesh", "Heskan", "Kriv", "Medrash", "Mehen", "Nadarr",
            "Pandjed", "Patrin", "Rhogar", "Shamash", "Shedinn", "Tarhun", "Torinn", "Akra", "Biri", "Daar",
            "Farideh", "Harann", "Havilar", "Jheri", "Kava", "Korinn", "Mishann", "Nala", "Perra", "Raiann",
            "Sora", "Surina", "Thava", "Uadjit"
    };
    private static final String[] CLANS = {
            "Clethtinthiallor", "Daardendrian", "Delmirev", "Drachedandion", "Fenkenkabradon", "Kepeshkmolik",
            "Kerrhylon", "Kimbatuul", "Linxakasendalor", "Myastan", "Nemmonis", "Norixius", "Ophinshtalajiir",
            "Prexijandilin", "Shestendeliath", "Turnuroth", "Verthisathurgiesh", "Yarjerit"
    };

    @Nullable
    private BlockPos home;

    protected DragonbornEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        this.getNavigation().setCanSwim(true);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.3D));
        // Players Hostile with the Dragonsworn are kept at a distance.
        this.goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 8.0F, 0.9D, 1.2D,
                player -> TierEffects.fleesFrom(TierEffects.tierWith((PlayerEntity) player, this))));
        this.goalSelector.add(3, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    /** 0 red, 1 white, 2 blue, 3 bronze, 4 gold. */
    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, Math.floorMod(variant, VARIANTS));
    }

    /** The breath of this Dragonborn's colour: red and gold breathe fire, white frost, blue and bronze lightning. */
    public DragonAncestry getAncestry() {
        return ancestryOf(this.getVariant());
    }

    public static DragonAncestry ancestryOf(int variant) {
        return switch (Math.floorMod(variant, VARIANTS)) {
            case 1 -> DragonAncestry.FROST;
            case 2, 3 -> DragonAncestry.STORM;
            default -> DragonAncestry.EMBER;
        };
    }

    /** Ties the Dragonborn to a spot it keeps near: {@link #dayRange} by day, {@link #nightRange} at night. */
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
        this.setVariant(random.nextInt(VARIANTS));
        this.setCustomName(Text.literal(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + CLANS[random.nextInt(CLANS.length)]));
        this.setPersistent();
        this.setHome(this.getBlockPos());
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // The mod gives every mob a player-targeting goal; Dragonborn only fight whom mayTarget allows.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == null || this.mayTarget(target)) {
            super.setTarget(target);
        }
    }

    /** Whether this Dragonborn may fight {@code target}. The Elder never picks fights. */
    protected boolean mayTarget(LivingEntity target) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // A Templar's breath or stray blow doesn't hurt its own folk.
        if (source.getAttacker() instanceof DragonbornEntity) {
            return false;
        }
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient && source.getAttacker() instanceof PlayerEntity player
                && !player.isCreative() && !player.isSpectator()) {
            defend(this, player);
        }
        return hurt;
    }

    /** Every Templar near {@code npc} turns on {@code player}, who struck it. */
    static void defend(DragonbornEntity npc, PlayerEntity player) {
        for (DragonbornTemplarEntity templar : npc.world.getEntitiesByClass(DragonbornTemplarEntity.class,
                new Box(npc.getBlockPos()).expand(DEFENCE_RANGE), LivingEntity::isAlive)) {
            templar.provoke(player);
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

    protected int dayRange() {
        return 20;
    }

    protected int nightRange() {
        return 12;
    }

    // ---- Sounds: a villager's, deeper ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 0.75F;
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
        this.setVariant(nbt.getInt("Variant"));
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")));
        }
    }
}
