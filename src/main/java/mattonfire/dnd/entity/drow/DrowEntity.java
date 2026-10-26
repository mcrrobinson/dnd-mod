package mattonfire.dnd.entity.drow;

import mattonfire.dnd.entity.ElfEntity;
import mattonfire.dnd.entity.MobActions;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A drow of the Spider Houses (design 9, section 4). The base class holds what they all share:
 * <ul>
 * <li>a skin variant and a name ("Zesstra Do'Urden");</li>
 * <li>who they fight: players below Neutral with the Spider Houses on sight ({@link DrowHostility}), whoever strikes
 * one (every drow nearby joins in), and surface elves; never drow or spiders ({@link DrowAllies}), and they can't hurt
 * them;</li>
 * <li><b>Sunlight Sensitivity</b>: in the Overworld by day with sky light {@value #SUNLIGHT_LEVEL}+, Weakness I and
 * Slowness I, refreshed every second, and their bolts fly wide;</li>
 * <li><b>Fey Ancestry</b>: they can't be Frightened (the {@code condition_immune/frightened} tag);</li>
 * <li>a shield, for those that carry one: half the projectiles from the front glance off it;</li>
 * <li>the hand crossbow ({@link #shootBolt}) for those that carry one, swapped for the blade within
 * {@value #MELEE_RANGE} blocks.</li>
 * </ul>
 * Natural spawns despawn like any monster; drow placed by a structure are persistent.
 */
public abstract class DrowEntity extends HostileEntity {
    public static final int VARIANTS = 6;
    public static final int SUNLIGHT_LEVEL = 12;
    /** Within this many blocks crossbow carriers draw their blades. */
    public static final double MELEE_RANGE = 4.0D;
    /** Hand crossbow range. */
    public static final double BOLT_MIN_RANGE = 6.0D;
    public static final double BOLT_MAX_RANGE = 14.0D;
    /** Ticks between bolts. */
    public static final int BOLT_INTERVAL = 50;
    public static final float BOLT_DAMAGE = 3.0F;
    /** Drow this close to a struck drow join the fight. */
    public static final double RALLY_RANGE = 16.0D;
    /** Chance that a shield turns a projectile from the front. */
    public static final float SHIELD_BLOCK_CHANCE = 0.5F;

    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(DrowEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final String[] FIRST_NAMES = {
            "Zaknafein", "Dinin", "Nalfein", "Jarlaxle", "Ryld", "Pharaun", "Valas", "Tsabrak", "Quenthel", "Triel",
            "Vierna", "Maya", "Briza", "Zesstra", "Ilphrin", "Nathrae", "Viconia", "Halisstra", "Danifae", "Sabal",
            "Chalithra", "Belaern", "Ilmryn", "Merinid", "Szordrin", "Vhurdaer"
    };
    private static final String[] HOUSES = {
            "Do'Urden", "Baenre", "Xorlarrin", "Mizzrym", "Melarn", "Agrach Dyrr", "Barrison Del'Armgo", "Faen Tlabbar",
            "Hun'ett", "Tlin'orzza", "Vandree", "Auvryndar"
    };

    protected DrowEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 10;
        this.getNavigation().setCanSwim(true);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.initCombatGoals();
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, DrowEntity.class).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                player -> DrowHostility.targets(this, (PlayerEntity) player)));
        // Surface elves are hated on sight.
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, ElfEntity.class, 10, true, false, null));
    }

    /** The fighting goals (priority 1-4). */
    protected abstract void initCombatGoals();

    /** Puts this drow's gear on it (weapons, a shield). */
    protected abstract void equip(Random random);

    // ---- Variant and name ----

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

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.setVariant(random.nextInt(VARIANTS));
        this.setCustomName(this.drowName(random));
        this.equip(random);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            // Their gear comes from the loot table, not the hands.
            this.setEquipmentDropChance(slot, 0.0F);
        }
        if (spawnReason == SpawnReason.STRUCTURE) {
            this.setPersistent();
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    /** The name it's given when it spawns ("Zesstra Do'Urden"). */
    protected Text drowName(Random random) {
        return Text.literal(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + house(random));
    }

    /** A random drow house ("Baenre"). */
    protected static String house(Random random) {
        return HOUSES[random.nextInt(HOUSES.length)];
    }

    /** A weapon with no attribute modifiers of its own, so the mob's attack damage is exactly its stat. */
    protected static ItemStack blade(net.minecraft.item.Item item) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateNbt().put("AttributeModifiers", new NbtList());
        return stack;
    }

    // ---- Who it fights ----

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == null || this.mayTarget(target)) {
            super.setTarget(target);
        }
    }

    protected boolean mayTarget(LivingEntity target) {
        // Their own captives are left alone in the pens.
        if (DrowAllies.isAlly(target) || mattonfire.dnd.world.underdark.Captives.isCaptive(target)) {
            return false;
        }
        if (target instanceof PlayerEntity player) {
            return DrowHostility.mayFight(this, player);
        }
        return true;
    }

    @Override
    public boolean isTeammate(net.minecraft.entity.Entity other) {
        return super.isTeammate(other) || DrowAllies.isAlly(other);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() != null && source.getAttacker() != this && DrowAllies.isAlly(source.getAttacker())) {
            return false;
        }
        if (!this.world.isClient && this.blocksWithShield(source)) {
            return false;
        }
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient && source.getAttacker() instanceof PlayerEntity player
                && !player.isCreative() && !player.isSpectator()) {
            for (DrowEntity other : this.world.getEntitiesByClass(DrowEntity.class,
                    this.getBoundingBox().expand(RALLY_RANGE), d -> d != this && d.isAlive() && d.getTarget() == null)) {
                other.setTarget(player);
            }
        }
        return hurt;
    }

    /** Whether the shield (if it carries one) turns this hit: a projectile from the front, half the time. */
    protected boolean blocksWithShield(DamageSource source) {
        if (!this.getOffHandStack().isOf(Items.SHIELD) || !(source.getSource() instanceof ProjectileEntity)
                || !this.isInFront(source) || this.random.nextFloat() >= SHIELD_BLOCK_CHANCE) {
            return false;
        }
        this.world.playSound(null, this.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1.0F,
                0.9F + this.random.nextFloat() * 0.2F);
        return true;
    }

    /** Whether the hit comes from in front of this drow. */
    protected boolean isInFront(DamageSource source) {
        Vec3d from = source.getPosition();
        if (from == null) {
            return false;
        }
        Vec3d facing = this.getRotationVec(1.0F).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3d to = from.subtract(this.getPos()).multiply(1.0D, 0.0D, 1.0D);
        return to.lengthSquared() > 1.0E-4 && to.normalize().dotProduct(facing) > 0.3D;
    }

    // ---- Sunlight Sensitivity ----

    /** In the Overworld, by day, under open sky (sky light {@value #SUNLIGHT_LEVEL}+). */
    public boolean isSunlit() {
        if (this.world.getRegistryKey() != World.OVERWORLD || !this.world.isDay()) {
            return false;
        }
        return this.world.getLightLevel(LightType.SKY, BlockPos.ofFloored(this.getEyePos())) >= SUNLIGHT_LEVEL;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.age % 20 == 0 && this.isSunlit()) {
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0, false, false, true));
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, false, false, true));
        }
        if (this.age % 20 == 0 && this.getTarget() instanceof PlayerEntity player && !this.mayTarget(player)) {
            super.setTarget(null);
        }
        if (this.hasHandCrossbow()) {
            this.swapWeapon();
        }
    }

    // ---- The hand crossbow ----

    /** Whether this drow carries a hand crossbow (warriors and elites). */
    protected boolean hasHandCrossbow() {
        return false;
    }

    /** The blade it draws within {@value #MELEE_RANGE} blocks. */
    protected ItemStack meleeWeapon() {
        return blade(Items.IRON_SWORD);
    }

    /** The crossbow at range, the blade up close. */
    private void swapWeapon() {
        LivingEntity target = this.getTarget();
        boolean close = target != null && this.squaredDistanceTo(target) <= (MELEE_RANGE + 0.5D) * (MELEE_RANGE + 0.5D);
        boolean holdingCrossbow = this.getMainHandStack().isOf(Items.CROSSBOW);
        if (close && holdingCrossbow) {
            this.equipStack(EquipmentSlot.MAINHAND, this.meleeWeapon());
        } else if (!close && target != null && !holdingCrossbow) {
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            CrossbowItem.setCharged(crossbow, true);
            this.equipStack(EquipmentSlot.MAINHAND, crossbow);
        }
    }

    /** Fires a poisoned bolt at {@code target}. Sunlight ruins the aim. */
    public void shootBolt(LivingEntity target) {
        DrowBoltEntity bolt = new DrowBoltEntity(this.world, this);
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double dy = target.getBodyY(0.5D) - bolt.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float divergence = 14 - this.world.getDifficulty().getId() * 4;
        if (this.isSunlit()) {
            divergence += 10.0F;
        }
        bolt.setVelocity(dx, dy + horizontal * 0.12D, dz, 2.2F, divergence);
        this.world.spawnEntity(bolt);
        this.playSound(SoundEvents.ITEM_CROSSBOW_SHOOT, 1.0F, 1.3F + this.random.nextFloat() * 0.2F);
        MobActions.log(this, "hand crossbow", target);
    }

    // ---- Sounds: an illusioner's, a little higher ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 1.1F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ILLUSIONER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ILLUSIONER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ILLUSIONER_DEATH;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setVariant(nbt.getInt("Variant"));
    }
}
