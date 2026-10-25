package mattonfire.dnd.entity.drow;

import java.util.EnumSet;

import mattonfire.dnd.entity.MobActions;
import mattonfire.dnd.magic.MagicMissiles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * A Drow Mage: 22 health, 2 armour, a dagger (3). Keeps {@value #MIN_RANGE}-{@value #MAX_RANGE} blocks from its
 * target and casts, when it can see it:
 * <ul>
 * <li><b>Darkness</b> every {@value #DARKNESS_COOLDOWN} ticks: a sphere ({@link DrowDarkness}) round a point
 * between it and its target, centred on the target's side;</li>
 * <li><b>Faerie Fire</b> every {@value #FAERIE_FIRE_COOLDOWN} ticks on its target: Glowing for
 * {@value #FAERIE_FIRE_TICKS} ticks and +{@value FaerieFire#BONUS_DAMAGE} damage from every hit
 * ({@link FaerieFire});</li>
 * <li><b>Magic Missile</b> every {@value #MISSILE_COOLDOWN} ticks when nothing else is ready: 3 darts of 2 force
 * damage, no attack roll ({@link MagicMissiles}).</li>
 * </ul>
 */
public class DrowMageEntity extends DrowEntity {
    public static final double MIN_RANGE = 8.0D;
    public static final double MAX_RANGE = 12.0D;
    /** Spells reach this far. */
    public static final double CAST_RANGE = 16.0D;
    public static final int DARKNESS_COOLDOWN = 400;
    public static final int FAERIE_FIRE_COOLDOWN = 300;
    public static final int FAERIE_FIRE_TICKS = 200;
    public static final int MISSILE_COOLDOWN = 60;
    public static final int MISSILE_DARTS = 3;
    public static final float MISSILE_DAMAGE = 2.0F;
    /** Ticks after one spell before the next. */
    private static final int GLOBAL_COOLDOWN = 20;

    private int darknessCooldown = 40;
    private int faerieFireCooldown = 20;
    private int missileCooldown = MISSILE_COOLDOWN;
    private int globalCooldown;

    public DrowMageEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createDrowMageAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initCombatGoals() {
        this.goalSelector.add(2, new KeepDistanceGoal(this));
    }

    @Override
    protected void equip(Random random) {
        // An amethyst focus in place of a dagger.
        this.equipStack(EquipmentSlot.MAINHAND, blade(Items.AMETHYST_SHARD));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.darknessCooldown > 0) this.darknessCooldown--;
        if (this.faerieFireCooldown > 0) this.faerieFireCooldown--;
        if (this.missileCooldown > 0) this.missileCooldown--;
        if (this.globalCooldown > 0) this.globalCooldown--;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.globalCooldown > 0 || !(this.world instanceof ServerWorld world)
                || this.squaredDistanceTo(target) > CAST_RANGE * CAST_RANGE || !this.getVisibilityCache().canSee(target)) {
            return;
        }
        if (this.darknessCooldown <= 0) {
            this.castDarkness(world, target);
        } else if (this.faerieFireCooldown <= 0 && !target.hasStatusEffect(Drow.FAERIE_FIRE)) {
            this.castFaerieFire(world, target);
        } else if (this.missileCooldown <= 0) {
            MobActions.log(this, "magic missile", target);
            this.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            MagicMissiles.fire(world, this, target, MISSILE_DARTS, MISSILE_DAMAGE);
            this.missileCooldown = MISSILE_COOLDOWN;
            this.globalCooldown = GLOBAL_COOLDOWN;
        }
    }

    private void castDarkness(ServerWorld world, LivingEntity target) {
        // Between the two, a little past the middle towards the target, so the target is inside and the mage isn't.
        Vec3d from = this.getPos();
        Vec3d to = target.getPos();
        double distance = from.distanceTo(to);
        Vec3d center = distance < 1.0E-3 ? to
                : from.add(to.subtract(from).multiply(Math.max(0.5D, (distance - 2.0D) / distance)));
        center = center.add(0.0D, 1.0D, 0.0D);
        MobActions.log(this, "darkness", target);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1.0F, 0.8F);
        DrowDarkness.cast(world, center, this);
        this.darknessCooldown = DARKNESS_COOLDOWN;
        this.globalCooldown = GLOBAL_COOLDOWN;
    }

    private void castFaerieFire(ServerWorld world, LivingEntity target) {
        MobActions.log(this, "faerie fire", target);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 1.5F, 0.7F);
        world.spawnParticles(ParticleTypes.WITCH, target.getX(), target.getBodyY(0.5D), target.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.02D);
        FaerieFire.apply(target, this, FAERIE_FIRE_TICKS);
        this.faerieFireCooldown = FAERIE_FIRE_COOLDOWN;
        this.globalCooldown = GLOBAL_COOLDOWN;
    }

    /**
     * Keeps {@value #MIN_RANGE}-{@value #MAX_RANGE} blocks off: backs away when the target comes nearer, walks in when
     * it's further or out of sight, and stabs with the dagger when cornered.
     */
    static final class KeepDistanceGoal extends Goal {
        private final DrowMageEntity mage;
        private int stabCooldown;
        private int repathCooldown;

        KeepDistanceGoal(DrowMageEntity mage) {
            this.mage = mage;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.mage.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mage.getTarget();
            if (target == null) {
                return;
            }
            if (this.stabCooldown > 0) this.stabCooldown--;
            if (this.repathCooldown > 0) this.repathCooldown--;
            this.mage.getLookControl().lookAt(target, 30.0F, 30.0F);
            double distance = Math.sqrt(this.mage.squaredDistanceTo(target));
            boolean sees = this.mage.getVisibilityCache().canSee(target);
            if (distance <= 2.2D && this.stabCooldown <= 0) {
                this.mage.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                this.mage.tryAttack(target);
                this.stabCooldown = 20;
            }
            if (!sees || distance > MAX_RANGE) {
                if (this.repathCooldown <= 0) {
                    this.mage.getNavigation().startMovingTo(target, 1.0D);
                    this.repathCooldown = 10;
                }
            } else if (distance < MIN_RANGE) {
                if (this.repathCooldown <= 0 || this.mage.getNavigation().isIdle()) {
                    Vec3d away = NoPenaltyTargeting.findFrom(this.mage, 10, 4, target.getPos());
                    if (away != null) {
                        this.mage.getNavigation().startMovingTo(away.x, away.y, away.z, 1.2D);
                    }
                    this.repathCooldown = 20;
                }
            } else {
                this.mage.getNavigation().stop();
            }
        }
    }
}
