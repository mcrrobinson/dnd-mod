package mattonfire.dnd.entity;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.classes.SkillChecks.Study;
import mattonfire.dnd.entity.ai.goal.RustMonsterEatGoal;
import mattonfire.dnd.magic.Corrosion;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Rust Monster: an armadillo-like cave beast with two feathery antennae that rust metal on touch.
 * <ul>
 * <li><b>Antenna touch</b>: every {@link #TOUCH_INTERVAL} ticks with its target within
 * {@link #TOUCH_RANGE} blocks, a DEX save ({@link #TOUCH}, DC {@value #TOUCH_DC}); on a failure one
 * random corrodible piece of armour (or, with none, a held item) rusts one step ({@link Corrosion}).</li>
 * <li><b>Weapons that hit it</b>: a player's melee hit with a corrodible main-hand item rusts it one
 * step, {@link #WEAPON_CORRODE_CHANCE} of the time (no save). Arrows are safe.</li>
 * <li><b>It eats metal</b> ({@link RustMonsterEatGoal}): it walks over to dropped iron or gold, eats it,
 * heals and is sated for {@link #SATED_TICKS} ticks, when it drops its target and picks no new one.</li>
 * </ul>
 * It only hunts players who carry corrodible metal ({@link #carriesMetal}); anyone else it leaves alone
 * unless they hurt it. Mountain dwarves attack it on sight.
 */
public class RustMonsterEntity extends HostileEntity implements GeoEntity {
    public static final int TOUCH_DC = 12;
    public static final int TOUCH_INTERVAL = 40;
    public static final double TOUCH_RANGE = 2.5D;
    public static final float WEAPON_CORRODE_CHANCE = 0.5F;
    public static final int SATED_TICKS = 200;
    public static final float HEAL_PER_ITEM = 4.0F;
    /** More than this many metal items in the inventory draw it, even with nothing worn or held. */
    public static final int CARRIED_METAL_THRESHOLD = 8;

    public static final MobSaveInfo.Entry TOUCH = MobSaveInfo.register(ModEntityTypes.RUST_MONSTER,
            "save.dndclasses.rust_touch", Ability.DEX, TOUCH_DC, false, "save.dndclasses.effect.rust_touch");

    private static final TrackedData<Boolean> EATING = DataTracker.registerData(RustMonsterEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation EAT = RawAnimation.begin().thenLoop("eat");
    private static final String TOUCH_ANIM = "antenna_touch";

    private static final DustParticleEffect RUST_DUST = new DustParticleEffect(new Vector3f(0.7F, 0.34F, 0.15F), 1.0F);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int touchCooldown = TOUCH_INTERVAL;
    private int satedTicks;
    /** Chance a metal weapon that hits it rusts; {@code WeaponCorrodeChance} in NBT (1.0 for tests). */
    private float weaponCorrodeChance = WEAPON_CORRODE_CHANCE;

    public RustMonsterEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 12;
    }

    /** Registers the save entry and the Study weakness; call once after the entity types. */
    public static void register() {
        // Loading the class registers TOUCH
        Study.registerWeakness(ModEntityTypes.RUST_MONSTER, Text.translatable("study.dndclasses.weakness.rust_monster"));
    }

    public static DefaultAttributeContainer.Builder createRustMonsterAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 32.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0D);
    }

    /**
     * Natural spawns: in the dark, out of sight of the sky and below y=0 in the Overworld. Anywhere dark in
     * other dimensions (the Underdark's spawn lists decide where it appears there).
     */
    public static boolean canSpawn(EntityType<RustMonsterEntity> type, ServerWorldAccess world, SpawnReason reason,
                                   BlockPos pos, Random random) {
        if (!HostileEntity.canSpawnInDark(type, world, reason, pos, random)) {
            return false;
        }
        if (reason != SpawnReason.NATURAL || world.toServerWorld().getRegistryKey() != World.OVERWORLD) {
            return true;
        }
        return pos.getY() < 0 && !world.isSkyVisible(pos);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(EATING, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new RustMonsterEatGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this) {
            @Override
            public boolean canStart() {
                return !RustMonsterEntity.this.isSated() && super.canStart();
            }
        });
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                target -> !this.isSated() && target instanceof PlayerEntity player && carriesMetal(player)));
    }

    // ---- State ----

    public boolean isSated() {
        return this.satedTicks > 0;
    }

    /** Full of iron: drops its target and won't pick a new one for {@link #SATED_TICKS} ticks. */
    public void sate() {
        this.satedTicks = SATED_TICKS;
        this.setTarget(null);
        this.setAttacker(null);
    }

    public boolean isEating() {
        return this.dataTracker.get(EATING);
    }

    public void setEating(boolean eating) {
        this.dataTracker.set(EATING, eating);
    }

    /** Whether {@code entity} wears or holds corrodible metal, or carries more than a few metal items. */
    public static boolean carriesMetal(LivingEntity entity) {
        for (ItemStack stack : entity.getItemsEquipped()) {
            if (stack.isIn(Corrosion.CORRODIBLE)) {
                return true;
            }
        }
        if (entity instanceof PlayerEntity player) {
            int count = 0;
            for (ItemStack stack : player.getInventory().main) {
                if (stack.isIn(Corrosion.RUST_MONSTER_FOOD)) {
                    count += stack.getCount();
                    if (count > CARRIED_METAL_THRESHOLD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ---- Attacks ----

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.satedTicks > 0) {
            this.satedTicks--;
            if (this.getTarget() != null) {
                this.setTarget(null);
            }
        }
        if (this.touchCooldown > 0) {
            this.touchCooldown--;
        }
        LivingEntity target = this.getTarget();
        if (this.touchCooldown <= 0 && target != null && target.isAlive() && !this.isEating()
                && this.squaredDistanceTo(target) <= TOUCH_RANGE * TOUCH_RANGE && this.canSee(target)) {
            this.antennaTouch(target);
        }
    }

    /** The DEX save against the antennae; a failure rusts one corrodible piece of gear. */
    private void antennaTouch(LivingEntity target) {
        this.touchCooldown = TOUCH_INTERVAL;
        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        this.triggerAnim("touch_controller", TOUCH_ANIM);
        MobActions.log(this, "antenna touch", target);
        if (target instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        SaveResult save = SavingThrow.of(target, TOUCH)
                .source(this)
                .onSuccess(Text.translatable(TOUCH.effectKey()))
                .onFailure(Text.translatable(TOUCH.effectKey() + ".failure"))
                .roll();
        if (save.succeeded()) {
            DnDClasses.LOGGER.info("[Rust] {} resists", target.getName().getString());
            return;
        }
        EquipmentSlot slot = this.pickCorrodible(target);
        if (slot == null) {
            DnDClasses.LOGGER.info("[Rust] {} has nothing to corrode", target.getName().getString());
            return;
        }
        Corrosion.corrode(target, target.getEquippedStack(slot), slot);
        ((ServerWorld) this.world).spawnParticles(RUST_DUST, target.getX(), target.getBodyY(0.5D), target.getZ(),
                12, 0.3D, 0.4D, 0.3D, 0.0D);
    }

    /** A random corrodible armour piece, or with none, a corrodible main or off hand item. */
    private EquipmentSlot pickCorrodible(LivingEntity target) {
        List<EquipmentSlot> armor = new ArrayList<>();
        List<EquipmentSlot> hands = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (Corrosion.canCorrode(target.getEquippedStack(slot))) {
                (slot.getType() == EquipmentSlot.Type.ARMOR ? armor : hands).add(slot);
            }
        }
        List<EquipmentSlot> pool = !armor.isEmpty() ? armor : hands;
        return pool.isEmpty() ? null : pool.get(this.random.nextInt(pool.size()));
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient && source.isOf(DamageTypes.PLAYER_ATTACK)
                && source.getSource() == source.getAttacker() && source.getAttacker() instanceof PlayerEntity player) {
            ItemStack weapon = player.getMainHandStack();
            if (Corrosion.canCorrode(weapon) && this.random.nextFloat() < this.weaponCorrodeChance) {
                Corrosion.corrode(player, weapon, EquipmentSlot.MAINHAND);
            }
        }
        return hurt;
    }

    // ---- Sounds ----

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_TURTLE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_SILVERFISH_STEP, 0.3F, 0.7F);
    }

    @Override
    public float getSoundPitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 0.6F;
    }

    // ---- Animation ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::predicate));
        controllers.add(new AnimationController<>(this, "touch_controller", 0, state -> PlayState.STOP)
                .triggerableAnim(TOUCH_ANIM, RawAnimation.begin().thenPlay(TOUCH_ANIM)));
    }

    private PlayState predicate(AnimationState<RustMonsterEntity> state) {
        if (this.isEating()) {
            return state.setAndContinue(EAT);
        }
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ---- Saving ----

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("SatedTicks", this.satedTicks);
        nbt.putFloat("WeaponCorrodeChance", this.weaponCorrodeChance);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.satedTicks = nbt.getInt("SatedTicks");
        if (nbt.contains("WeaponCorrodeChance")) {
            this.weaponCorrodeChance = nbt.getFloat("WeaponCorrodeChance");
        }
    }
}
