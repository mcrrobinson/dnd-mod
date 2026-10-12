package mattonfire.dnd.entity;

import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.Entity;
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
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
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
 * A Tiefling of the Brimstone Sanctums. The base class holds what they all share: a skin variant (crimson,
 * plum, ash, slate or ember), a name (an infernal name or a virtue name), a home spot they keep near, fire
 * immunity (from their entity type), Hellish Rebuke, and the rule that striking one brings every Exile nearby
 * down on you. Exiles ({@link TieflingExileEntity}) keep the sanctum; the Pactbroker
 * ({@link TieflingPactbrokerEntity}) trades. Neutral to everyone.
 */
public abstract class TieflingEntity extends PathAwareEntity {
    /** Skins: crimson, plum, ash, slate, ember. */
    public static final int VARIANTS = 5;
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(TieflingEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Exiles this close to a struck Tiefling come to its defence. */
    static final double DEFENCE_RANGE = 16.0D;
    /** Hellish Rebuke: a melee attacker burns for this long. */
    public static final int REBUKE_FIRE_SECONDS = 4;

    private static final String[] INFERNAL_NAMES = {
            "Akmenos", "Amnon", "Barakas", "Damakos", "Ekemon", "Iados", "Kairon", "Leucis", "Melech", "Mordai",
            "Morthos", "Pelaios", "Skamos", "Therai", "Akta", "Anakis", "Bryseis", "Criella", "Damaia", "Ea",
            "Kallista", "Lerissa", "Makaria", "Nemeia", "Orianna", "Phelaia", "Rieta"
    };
    private static final String[] VIRTUE_NAMES = {
            "Ash", "Carrion", "Despair", "Hope", "Ire", "Mercy", "Penance", "Quiet", "Sorrow", "Torment", "Vigil", "Weary"
    };

    @Nullable
    private BlockPos home;
    private int range = 16;

    protected TieflingEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        this.getNavigation().setCanSwim(true);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.3D));
        // Players Hostile with the Brimstone Covenant are kept at a distance.
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

    /** 0 crimson, 1 plum, 2 ash, 3 slate, 4 ember. */
    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, Math.floorMod(variant, VARIANTS));
    }

    /** Ties the Tiefling to a spot it keeps within {@code range} of (half that at night). */
    public void setHome(BlockPos home, int range) {
        this.home = home.toImmutable();
        this.range = range;
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
        String[] names = random.nextInt(3) == 0 ? VIRTUE_NAMES : INFERNAL_NAMES;
        this.setCustomName(Text.literal(names[random.nextInt(names.length)]));
        this.setPersistent();
        if (this.home == null) {
            this.setHome(this.getBlockPos(), this.range);
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // The mod gives every mob a player-targeting goal; Tieflings only fight whom mayTarget allows.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == null || this.mayTarget(target)) {
            super.setTarget(target);
        }
    }

    /** Whether this Tiefling may fight {@code target}. The Pactbroker never picks fights. */
    protected boolean mayTarget(LivingEntity target) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // An Exile's stray fireball or blow doesn't hurt its own folk.
        if (source.getAttacker() instanceof TieflingEntity) {
            return false;
        }
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient) {
            Entity attacker = source.getAttacker();
            if (attacker instanceof LivingEntity living && attacker == source.getSource() && isMelee(source)) {
                this.rebuke(living);
            }
            if (attacker instanceof PlayerEntity player && !player.isCreative() && !player.isSpectator()) {
                defend(this, player);
            }
        }
        return hurt;
    }

    private static boolean isMelee(DamageSource source) {
        return source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO)
                || source.isOf(DamageTypes.PLAYER_ATTACK);
    }

    /** Hellish Rebuke: whoever struck this Tiefling in melee bursts into flame for {@link #REBUKE_FIRE_SECONDS} s. */
    void rebuke(LivingEntity attacker) {
        if (!attacker.isAlive() || attacker.isFireImmune()) {
            return;
        }
        attacker.setOnFireFor(REBUKE_FIRE_SECONDS);
        if (this.world instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.FLAME, attacker.getX(), attacker.getBodyY(0.5D), attacker.getZ(), 16,
                    attacker.getWidth() * 0.4D, attacker.getHeight() * 0.3D, attacker.getWidth() * 0.4D, 0.02D);
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ITEM_FIRECHARGE_USE,
                    SoundCategory.HOSTILE, 0.8F, 0.9F);
        }
        mattonfire.dnd.classes.DnDClasses.LOGGER.info("[BrimstoneSanctum] {} rebuked {} (fire {} s)", this.getName().getString(),
                attacker.getName().getString(), REBUKE_FIRE_SECONDS);
    }

    /** Every Exile near {@code npc} turns on {@code player}, who struck it. */
    static void defend(TieflingEntity npc, PlayerEntity player) {
        for (TieflingExileEntity exile : npc.world.getEntitiesByClass(TieflingExileEntity.class,
                new Box(npc.getBlockPos()).expand(DEFENCE_RANGE), LivingEntity::isAlive)) {
            exile.provoke(player);
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
            // The Nether has no day and night, so the range never changes.
            this.setPositionTarget(this.home, this.range);
        }
    }

    // ---- Sounds: a villager's, lower and rougher ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 0.85F;
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
        nbt.putInt("HomeRange", this.range);
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setVariant(nbt.getInt("Variant"));
        if (nbt.contains("HomeRange")) {
            this.range = nbt.getInt("HomeRange");
        }
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")), this.range);
        }
    }
}
