package mattonfire.dnd.entity;

import java.util.UUID;
import mattonfire.dnd.entity.boss.BossFight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
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
 * The Lich's phylactery: a soul vessel on a carved plinth that the Lich places near itself. While
 * it stands, a Lich that dies flees into it and reforms here after {@link #REFORM_TICKS} with full
 * health; only once it is smashed can the Lich die for good. Smashing it while the Lich is reforming
 * destroys the Lich outright, so the phylactery then drops the Lich's rewards.
 */
public class PhylacteryEntity extends MobEntity implements GeoEntity {
    public static final int REFORM_TICKS = 600;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation REFORM = RawAnimation.begin().thenLoop("reform");
    private static final TrackedData<Boolean> REFORMING = DataTracker.registerData(PhylacteryEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID lichUuid;
    /** Ticks until the fled Lich reforms, or -1 when no Lich is reforming. */
    private int reformTicks = -1;
    /** Custom name of the Lich that fled here, so it comes back under the same name. */
    @Nullable
    private String lichName;

    public PhylacteryEntity(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 10;
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createPhylacteryAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0D);
    }

    public void setLich(LichEntity lich) {
        this.lichUuid = lich.getUuid();
    }

    @Nullable
    public UUID getLichUuid() {
        return this.lichUuid;
    }

    public boolean isReforming() {
        return this.reformTicks >= 0;
    }

    /** Called when the linked Lich dies: its soul flies here and it reforms in {@link #REFORM_TICKS}. */
    public void beginReform(LichEntity lich) {
        this.reformTicks = REFORM_TICKS;
        this.lichName = lich.hasCustomName() ? Text2.toJson(lich.getCustomName()) : null;
        this.dataTracker.set(REFORMING, true);
        if (this.world instanceof ServerWorld world) {
            // A trail of souls from the body to the phylactery
            double dx = this.getX() - lich.getX();
            double dy = this.getBodyY(0.7D) - lich.getBodyY(0.6D);
            double dz = this.getZ() - lich.getZ();
            for (int i = 0; i <= 20; i++) {
                double t = i / 20.0D;
                world.spawnParticles(ParticleTypes.SOUL, lich.getX() + dx * t, lich.getBodyY(0.6D) + dy * t,
                        lich.getZ() + dz * t, 2, 0.1D, 0.1D, 0.1D, 0.01D);
            }
            world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PARTICLE_SOUL_ESCAPE,
                    SoundCategory.HOSTILE, 3.0F, 0.6F);
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(REFORMING, false);
    }

    @Override
    protected void initGoals() {
        // It's an object: no AI.
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.world instanceof ServerWorld world)) {
            return;
        }
        if (this.age % 20 == 0) {
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getBodyY(0.8D), this.getZ(),
                    1, 0.15D, 0.15D, 0.15D, 0.005D);
        }
        if (this.reformTicks >= 0) {
            float angle = this.age * 0.4F;
            world.spawnParticles(ParticleTypes.SOUL, this.getX() + MathHelper.cos(angle) * 0.8D, this.getBodyY(0.6D),
                    this.getZ() + MathHelper.sin(angle) * 0.8D, 1, 0.0D, 0.05D, 0.0D, 0.01D);
            if (this.reformTicks % 100 == 0 && this.reformTicks > 0) {
                this.playSound(SoundEvents.BLOCK_BEACON_AMBIENT, 2.0F, 0.5F);
            }
            if (this.reformTicks-- == 0) {
                this.reformLich(world);
            }
        }
    }

    private void reformLich(ServerWorld world) {
        this.reformTicks = -1;
        this.dataTracker.set(REFORMING, false);
        LichEntity lich = ModEntityTypes.LICH.create(world);
        if (lich == null) {
            return;
        }
        float angle = this.random.nextFloat() * MathHelper.TAU;
        lich.refreshPositionAndAngles(this.getX() + MathHelper.cos(angle) * 1.5D, this.getY(),
                this.getZ() + MathHelper.sin(angle) * 1.5D, this.random.nextFloat() * 360.0F, 0.0F);
        if (!world.isSpaceEmpty(lich)) {
            lich.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), lich.getYaw(), 0.0F);
        }
        if (this.lichName != null) {
            lich.setCustomName(Text2.fromJson(this.lichName));
        }
        lich.linkPhylactery(this);
        // A dungeon's boss stays its dungeon's boss: the room's ward finds it again by these tags.
        this.getCommandTags().stream().filter(tag -> tag.startsWith("dndclasses.dungeon")).forEach(lich::addCommandTag);
        lich.initialize(world, world.getLocalDifficulty(lich.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
        lich.setPersistent();
        world.spawnEntityAndPassengers(lich);
        this.lichUuid = lich.getUuid();

        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, lich.getX(), lich.getBodyY(0.5D), lich.getZ(),
                60, 0.5D, 1.0D, 0.5D, 0.05D);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, lich.getX(), lich.getBodyY(0.5D), lich.getZ(),
                30, 0.5D, 1.0D, 0.5D, 0.02D);
        world.playSound(null, lich.getX(), lich.getY(), lich.getZ(), SoundEvents.ENTITY_WITHER_SPAWN,
                SoundCategory.HOSTILE, 1.0F, 1.4F);
        lich.speak("reform");
        PlayerEntity player = world.getClosestPlayer(lich, 24.0D);
        if (player != null && !player.isCreative() && !player.isSpectator()) {
            lich.setTarget(player);
        }
    }

    @Nullable
    private LichEntity findLich() {
        if (this.lichUuid != null && this.world instanceof ServerWorld world
                && world.getEntity(this.lichUuid) instanceof LichEntity lich && lich.isAlive()) {
            return lich;
        }
        return null;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.world.isClient || LichEntity.isLichSide(source.getAttacker())) {
            return false;
        }
        if (source.getAttacker() instanceof PlayerEntity player && LichEntity.isNecromancer(player)) {
            amount *= LichEntity.NECROMANCER_DAMAGE_BONUS;
        }
        boolean damaged = super.damage(source, amount);
        if (damaged && this.isAlive() && source.getAttacker() instanceof LivingEntity attacker) {
            LichEntity lich = this.findLich();
            if (lich != null) {
                lich.defendPhylactery(this, attacker);
            }
        }
        return damaged;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.DROWN) || source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.IN_WALL)
                || source.isOf(DamageTypes.FREEZE) || source.isOf(DamageTypes.CRAMMING) || super.isInvulnerableTo(source);
    }

    @Override
    public void onDeath(DamageSource source) {
        if (!this.world.isClient && !this.dead && !this.isRemoved()) {
            ServerWorld world = (ServerWorld) this.world;
            world.spawnParticles(ParticleTypes.SOUL, this.getX(), this.getBodyY(0.6D), this.getZ(),
                    40, 0.4D, 0.4D, 0.4D, 0.08D);
            world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_GLASS_BREAK,
                    SoundCategory.HOSTILE, 2.0F, 0.5F);
            world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WITHER_DEATH,
                    SoundCategory.HOSTILE, 0.6F, 1.6F);
            if (this.isReforming()) {
                // The Lich had nowhere left to go: this is its true death.
                this.experiencePoints = LichEntity.XP;
                LivingEntity killer = this.getPrimeAdversary();
                if (killer instanceof ServerPlayerEntity player) {
                    BossFight.grantAdvancement(player, LichEntity.ADVANCEMENT);
                }
                LichEntity.broadcast(world, this, LichEntity.speech(this.lichName != null
                        ? Text2.fromJson(this.lichName) : ModEntityTypes.LICH.getName(), "unmade"));
            } else {
                LichEntity lich = this.findLich();
                if (lich != null) {
                    lich.onPhylacteryDestroyed(source.getAttacker());
                }
            }
        }
        super.onDeath(source);
    }

    @Override
    protected Identifier getLootTableId() {
        return this.isReforming() ? LichEntity.LOOT_TABLE : super.getLootTableId();
    }

    @Override
    public void checkDespawn() {
        if (this.world.getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
        }
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushAway(Entity entity) {
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
    }

    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance effect) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLOCK_AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.lichUuid != null) {
            nbt.putUuid("Lich", this.lichUuid);
        }
        nbt.putInt("ReformTicks", this.reformTicks);
        if (this.lichName != null) {
            nbt.putString("LichName", this.lichName);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.lichUuid = nbt.containsUuid("Lich") ? nbt.getUuid("Lich") : null;
        this.reformTicks = nbt.contains("ReformTicks") ? nbt.getInt("ReformTicks") : -1;
        this.lichName = nbt.contains("LichName") ? nbt.getString("LichName") : null;
        this.dataTracker.set(REFORMING, this.reformTicks >= 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<PhylacteryEntity> state) {
        return state.setAndContinue(this.dataTracker.get(REFORMING) ? REFORM : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /** Text (de)serialisation for the saved Lich name. */
    private static final class Text2 {
        static String toJson(net.minecraft.text.Text text) {
            return net.minecraft.text.Text.Serializer.toJson(text);
        }

        static net.minecraft.text.Text fromJson(String json) {
            net.minecraft.text.Text text = net.minecraft.text.Text.Serializer.fromJson(json);
            return text != null ? text : net.minecraft.text.Text.literal(json);
        }
    }
}
