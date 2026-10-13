package mattonfire.dnd.entity;

import java.util.EnumSet;
import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Race.BreathWeapon;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
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
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A Dragonborn Templar, the temple's guard. Neutral to players, like an Elf Warden: it hunts monsters
 * (except creepers) within {@link #HUNT_RANGE} blocks of home with an iron sword or a trident, and breathes
 * at anything it fights that's within {@link #BREATH_RANGE} blocks: a 60° cone of its colour's breath
 * (fire, frost or lightning, with the player Breath Weapon's visuals) for {@link #BREATH_DAMAGE} damage, DEX
 * save DC {@link #BREATH_DC} for half, every {@link #BREATH_COOLDOWN} ticks. It turns on a player who strikes
 * a Dragonborn, or whom it sees break the temple's law ({@link TempleLaw}).
 */
public class DragonbornTemplarEntity extends DragonbornEntity implements GuardAnger {
    public static final int HUNT_RANGE = 24;
    public static final double BREATH_RANGE = 5.0D;
    public static final float BREATH_DAMAGE = 5.0F;
    public static final int BREATH_COOLDOWN = 10 * 20;
    public static final int BREATH_DC = 12;
    private static final double COS_HALF_ANGLE = Math.cos(Math.toRadians(BreathWeapon.HALF_ANGLE_DEG));
    private static final String SAVE_LABEL = "save.dndclasses.templar_breath";
    private static final String SAVE_TAG = "templar_breath";

    private final GuardAnger.State anger = new GuardAnger.State();
    /** The age at which the next breath is ready. */
    private int breathReadyAge;

    public DragonbornTemplarEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder createTemplarAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 34.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, (double) HUNT_RANGE);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new BreathGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.add(3, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        SettlementGuard.addTargetGoals(this.targetSelector, this, DragonbornEntity.class,
                entity -> entity instanceof Monster && !(entity instanceof CreeperEntity)
                        && !(entity instanceof MimicEntity mimic && mimic.isDormant())
                        && this.nearHome(entity, HUNT_RANGE));
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(world.getRandom().nextFloat() < 0.7F ? Items.IRON_SWORD : Items.TRIDENT));
        this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setEquipmentDropChance(slot, 0.05F);
        }
        return data;
    }

    // Templars stay round the temple.
    @Override
    protected int dayRange() {
        return 16;
    }

    @Override
    protected int nightRange() {
        return 10;
    }

    /** The Templar's shout as it turns on a player who wronged the temple or struck one of its folk. */
    @Override
    public void provokeShout() {
        this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch() * 0.8F);
    }

    // Guards only turn on players who wronged them, and never on their own folk.
    @Override
    protected boolean mayTarget(LivingEntity target) {
        return SettlementGuard.mayTarget(this, target, DragonbornEntity.class);
    }

    /** Angry at the player it holds a grudge against, and at anyone Hostile with the Dragonsworn, on sight. */
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

    // ---- The breath ----

    public boolean breathReady() {
        return this.age >= this.breathReadyAge;
    }

    /** Whether the breath may hit {@code entity}: what it's fighting, monsters, and players it's angry with. */
    private boolean breathHits(LivingEntity entity, @Nullable LivingEntity target) {
        if (entity == this || !entity.isAlive() || entity.isSpectator() || entity instanceof DragonbornEntity) {
            return false;
        }
        if (entity == target || entity instanceof Monster) {
            return true;
        }
        return entity instanceof PlayerEntity player && !player.isCreative() && this.shouldAngerAt(player);
    }

    /**
     * Breathes at {@code target}: everything the breath may hit in a {@link #BREATH_RANGE}-block, 60° cone
     * towards its eyes, in clear line of sight, takes {@link #BREATH_DAMAGE} (a DEX save halves it) and the
     * colour's rider: Ember burns, Frost freezes, Storm makes it glow.
     */
    public void breathe(LivingEntity target) {
        if (!(this.world instanceof ServerWorld world)) {
            return;
        }
        this.breathReadyAge = this.age + BREATH_COOLDOWN;
        DragonAncestry ancestry = this.getAncestry();
        Vec3d eye = this.getEyePos();
        Vec3d look = target.getEyePos().subtract(eye).normalize();
        this.getLookControl().lookAt(target, 60.0F, 60.0F);
        DamageSource source = switch (ancestry) {
            case FROST -> ModDamageTypes.of(world, ModDamageTypes.FROST_BREATH, this);
            case STORM -> ModDamageTypes.of(world, BreathWeapon.LIGHTNING_DAMAGE, this);
            default -> ModDamageTypes.of(world, BreathWeapon.FIRE_DAMAGE, this);
        };
        List<LivingEntity> victims = world.getEntitiesByClass(LivingEntity.class, new Box(eye, eye).expand(BREATH_RANGE + 1.0D),
                e -> this.breathHits(e, target) && this.inCone(eye, look, e));
        for (LivingEntity victim : victims) {
            if (DragonSaves.isUnaffected(victim, source)) {
                continue;
            }
            SaveResult save = DragonSaves.save(victim, this, SAVE_LABEL, BREATH_DC, BreathWeapon.SAVE_EFFECT, SAVE_TAG, 20);
            if (!victim.damage(source, save.damage(BREATH_DAMAGE))) {
                continue;
            }
            switch (ancestry) {
                case FROST -> {
                    victim.extinguish();
                    if (save.failed()) {
                        victim.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, 40, 0), this);
                    }
                }
                case STORM -> victim.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, false, false), this);
                default -> victim.setFireTicks(Math.max(victim.getFireTicks(), save.duration(80)));
            }
        }
        BreathWeapon.playSound(world, this, ancestry);
        BreathWeapon.sendVisuals(this, ancestry, eye, look);
        DnDClasses.LOGGER.info("[DragonTemple] Templar {} breathed {} at {} ({} hit)", this.getEntityName(), ancestry.id(),
                target.getEntityName(), victims.size());
    }

    private boolean inCone(Vec3d eye, Vec3d look, LivingEntity e) {
        Box box = e.getBoundingBox();
        Vec3d closest = new Vec3d(MathHelper.clamp(eye.x, box.minX, box.maxX), MathHelper.clamp(eye.y, box.minY, box.maxY),
                MathHelper.clamp(eye.z, box.minZ, box.maxZ));
        if (closest.squaredDistanceTo(eye) > BREATH_RANGE * BREATH_RANGE) {
            return false;
        }
        boolean aimed = within(eye, look, e.getEyePos()) || within(eye, look, box.getCenter());
        return aimed && this.world.raycast(new RaycastContext(eye, e.getEyePos(), RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private static boolean within(Vec3d eye, Vec3d look, Vec3d point) {
        Vec3d to = point.subtract(eye);
        double length = to.length();
        return length < 1.0E-4D || to.dotProduct(look) / length >= COS_HALF_ANGLE;
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

    /** Breathes at the target when it's close, in sight and the breath is ready; runs beside the melee goal. */
    static final class BreathGoal extends Goal {
        private final DragonbornTemplarEntity templar;

        BreathGoal(DragonbornTemplarEntity templar) {
            this.templar = templar;
            this.setControls(EnumSet.noneOf(Control.class));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.templar.getTarget();
            return target != null && target.isAlive() && this.templar.breathReady()
                    && this.templar.squaredDistanceTo(target) <= BREATH_RANGE * BREATH_RANGE
                    && this.templar.getVisibilityCache().canSee(target);
        }

        @Override
        public boolean shouldContinue() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = this.templar.getTarget();
            if (target != null) {
                this.templar.breathe(target);
            }
        }
    }
}
